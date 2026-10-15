import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.reference.*;
/**
 * TorrentEngine 两处前插调用（其余指令不变，写出后重新载入比对）：
 *  1) acquireSession：SessionManager.uploadRateLimit(I) 之后插入 TorrentBoost.tune(session)
 *  2) 任何方法里 SessionManager.download(String,File,torrent_flags_t) 之前插入 uri = TorrentBoost.magnet(uri)
 */
public class PatchTorrentBoost {
 static final String ENGINE="Lcom/guocheng/mediapocket/TorrentEngine;",SM="Lcom/frostwire/jlibtorrent/SessionManager;",BOOST="Lcom/guocheng/mediapocket/TorrentBoost;";
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 static boolean isCall(Instruction i,String name,String params){if(!(i instanceof ReferenceInstruction)||!(((ReferenceInstruction)i).getReference() instanceof MethodReference))return false;MethodReference r=(MethodReference)((ReferenceInstruction)i).getReference();return r.getDefiningClass().equals(SM)&&r.getName().equals(name)&&String.join("",r.getParameterTypes().stream().map(Object::toString).toArray(String[]::new)).equals(params);}
 static final String DL_PARAMS="Ljava/lang/String;Ljava/io/File;Lcom/frostwire/jlibtorrent/swig/torrent_flags_t;";
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));ArrayList<ClassDef> classes=new ArrayList<>();int tune=0,magnet=0;Set<String> changed=new HashSet<>();
  for(ClassDef c:dex.getClasses()){
   if(!c.getType().startsWith("Lcom/guocheng/mediapocket/TorrentEngine")){classes.add(c);continue;}
   ArrayList<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    MethodImplementation impl=m.getImplementation();if(impl==null){methods.add(m);continue;}
    List<Instruction> list=new ArrayList<>();for(Instruction i:impl.getInstructions())list.add(i);
    ArrayList<Integer> upload=new ArrayList<>(),download=new ArrayList<>();
    for(int i=0;i<list.size();i++){if(c.getType().equals(ENGINE)&&m.getName().equals("acquireSession")&&isCall(list.get(i),"uploadRateLimit","I"))upload.add(i);if(isCall(list.get(i),"download",DL_PARAMS))download.add(i);}
    if(upload.isEmpty()&&download.isEmpty()){methods.add(m);continue;}
    MutableMethodImplementation code=new MutableMethodImplementation(impl);
    // 从后往前插，前面的下标不受影响
    TreeMap<Integer,String> edits=new TreeMap<>(Collections.reverseOrder());for(int i:upload)edits.put(i,"tune");for(int i:download)edits.put(i,"magnet");
    for(Map.Entry<Integer,String> e:edits.entrySet()){
     int at=e.getKey();FiveRegisterInstruction call=(FiveRegisterInstruction)list.get(at);
     if(e.getValue().equals("tune")){int session=call.getRegisterC();require(session<=15,"reg");code.addInstruction(at+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,session,0,0,0,0,new ImmutableMethodReference(BOOST,"tune",Collections.singletonList(SM),"V")));tune++;}
     else{int uri=call.getRegisterD();require(uri<=15,"reg");code.addInstruction(at,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,uri,0,0,0,0,new ImmutableMethodReference(BOOST,"magnet",Collections.singletonList("Ljava/lang/String;"),"Ljava/lang/String;")));code.addInstruction(at+1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,uri));magnet++;}
    }
    changed.add(key(m));
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
   }
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  require(tune==1,"expected one uploadRateLimit site in acquireSession, found "+tune);require(magnet>=1,"expected magnet download call");
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));
  DexFile old=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26)),updated=DexFileFactory.loadDexFile(new File(args[1]),Opcodes.forApi(26));
  Map<String,Method> defs=new HashMap<>();for(ClassDef c:updated.getClasses())for(Method m:c.getMethods())defs.put(key(m),m);int same=0;
  for(ClassDef c:old.getClasses())for(Method m:c.getMethods()){
   Method n=defs.remove(key(m));require(n!=null,"missing "+key(m));if(m.getImplementation()==null)continue;
   List<Instruction> a=new ArrayList<>(),b=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())a.add(i);for(Instruction i:n.getImplementation().getInstructions())b.add(i);
   if(!changed.contains(key(m))){require(a.size()==b.size(),"size "+key(m));for(int i=0;i<a.size();i++)require(PatchHeaderRoute.fingerprint(a.get(i)).equals(PatchHeaderRoute.fingerprint(b.get(i))),"changed "+key(m));same++;continue;}
   // 修改过的方法：去掉插入的 TorrentBoost 调用（及紧随的 move-result）后，操作码与引用逐条一致
   ArrayList<Instruction> kept=new ArrayList<>();
   for(int i=0;i<b.size();i++){Instruction x=b.get(i);if(x instanceof ReferenceInstruction&&((ReferenceInstruction)x).getReference() instanceof MethodReference&&((MethodReference)((ReferenceInstruction)x).getReference()).getDefiningClass().equals(BOOST)){if(((MethodReference)((ReferenceInstruction)x).getReference()).getName().equals("magnet"))i++;continue;}kept.add(x);}
   require(kept.size()==a.size(),"modified size "+key(m));
   for(int i=0;i<a.size();i++){require(a.get(i).getOpcode()==kept.get(i).getOpcode(),"opcode "+key(m)+" #"+i);if(a.get(i) instanceof ReferenceInstruction)require(((ReferenceInstruction)a.get(i)).getReference().toString().equals(((ReferenceInstruction)kept.get(i)).getReference().toString()),"ref "+key(m)+" #"+i);}
  }
  System.out.println("PASS: TorrentEngine boost hooks ("+tune+" tune, "+magnet+" magnet); "+changed.size()+" modified methods keep original opcodes/references; "+same+" other method bodies identical.");
 }
 static String key(Method m){return m.getDefiningClass()+"->"+m.getName()+m.getParameterTypes()+m.getReturnType();}
}
