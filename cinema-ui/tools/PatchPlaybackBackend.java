import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Select the requested backend before allocating LibVLC or a playback instance. */
public class PatchPlaybackBackend {
 static final String PLAYER="Lcom/guocheng/mediapocket/PlayerActivity;";
 static final String HOOK="Lcom/guocheng/mediapocket/PlaybackBackend;";
 static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 static int owner(Method m){int words=1;for(CharSequence p:m.getParameterTypes())words+=(p.toString().equals("J")||p.toString().equals("D"))?2:1;return m.getImplementation().getRegisterCount()-words;}
 static boolean selection(List<Instruction> list,int at){
  Instruction ins=list.get(at);
  if(!(ins instanceof ReferenceInstruction)||!(((ReferenceInstruction)ins).getReference() instanceof MethodReference))return false;
  MethodReference ref=(MethodReference)((ReferenceInstruction)ins).getReference();
  if(!ref.getDefiningClass().equals("Landroid/content/SharedPreferences;")||!ref.getName().equals("getBoolean"))return false;
  int stringReg=ins instanceof FiveRegisterInstruction?((FiveRegisterInstruction)ins).getRegisterD():((RegisterRangeInstruction)ins).getStartRegister()+1;
  for(int i=at-1;i>=0&&i>=at-4;i--){Instruction prev=list.get(i);if(prev instanceof OneRegisterInstruction&&((OneRegisterInstruction)prev).getRegisterA()==stringReg){return prev instanceof ReferenceInstruction&&((ReferenceInstruction)prev).getReference() instanceof StringReference&&"useVlc".equals(((StringReference)((ReferenceInstruction)prev).getReference()).getString());}}
  return false;
 }
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));List<ClassDef> classes=new ArrayList<>();Map<String,Set<Integer>> sites=new HashMap<>();int changes=0;
  for(ClassDef c:dex.getClasses()){
   if(!PLAYER.equals(c.getType())){classes.add(c);continue;}
   List<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    MethodImplementation impl=m.getImplementation();if(impl==null){methods.add(m);continue;}
    List<Instruction> before=PatchPlaybackOrientation.list(impl);MutableMethodImplementation code=new MutableMethodImplementation(impl);Set<Integer> replaced=new HashSet<>();
    for(int i=0;i<before.size();i++)if(selection(before,i)){
     require((m.getAccessFlags()&8)==0,"backend selection must belong to an Activity");int self=owner(m);
     ImmutableMethodReference target=new ImmutableMethodReference(HOOK,"preferVlc",Collections.singletonList("Ljava/lang/Object;"),"Z");
     if(self<=15)code.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,self,0,0,0,0,target));
     else code.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,target));
     replaced.add(i);changes++;
    }
    if(!replaced.isEmpty())sites.put(PatchPlaybackOrientation.key(m),replaced);
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
   }
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  require(changes==2&&sites.size()==2,"Expected exactly onCreate and advance backend selections; got "+changes);
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));
  DexFile updated=DexFileFactory.loadDexFile(new File(args[1]),Opcodes.forApi(26));Map<String,Method> newer=new HashMap<>();for(ClassDef c:updated.getClasses())for(Method m:c.getMethods())newer.put(PatchPlaybackOrientation.key(m),m);
  for(ClassDef c:dex.getClasses())for(Method m:c.getMethods()){
   String key=PatchPlaybackOrientation.key(m);Method n=newer.remove(key);require(n!=null&&m.getAccessFlags()==n.getAccessFlags(),"method ABI changed "+key);
   if(m.getImplementation()==null){require(n.getImplementation()==null,"code added "+key);continue;}
   require(m.getImplementation().getRegisterCount()==n.getImplementation().getRegisterCount(),"registers changed "+key);
   List<Instruction> a=PatchPlaybackOrientation.list(m.getImplementation()),b=PatchPlaybackOrientation.list(n.getImplementation());require(a.size()==b.size(),"instruction count changed "+key);
   for(int i=0;i<a.size();i++){
    require(a.get(i).getCodeUnits()==b.get(i).getCodeUnits(),"code size changed "+key);
    if(sites.containsKey(key)&&sites.get(key).contains(i)){
     Instruction call=b.get(i);require(call.getOpcode()==Opcode.INVOKE_STATIC||call.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"hook opcode");
     require(((ReferenceInstruction)call).getReference().toString().equals(HOOK+"->preferVlc(Ljava/lang/Object;)Z"),"hook target");
     int self=call instanceof FiveRegisterInstruction?((FiveRegisterInstruction)call).getRegisterC():((RegisterRangeInstruction)call).getStartRegister();require(self==owner(m),"wrong Activity register");
    }else require(PatchPlaybackOrientation.fingerprint(a.get(i)).equals(PatchPlaybackOrientation.fingerprint(b.get(i))),"original instruction changed "+key+" #"+i);
   }
  }
  require(newer.isEmpty(),"unexpected methods");System.out.println("PASS: two playback backend selections replaced; same registers/code size; all other instructions preserved.");
 }
}
