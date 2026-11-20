import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
/**
 * 导入和「更新当前配置」都走到 TvBoxActivity.lambda$6：fetch 之后直接 new JSONObject。
 * 只把这一处 TvBoxNet.fetch 改成同签名的 TvBoxConfigText.fetchConfig。寄存器、指令条数和其他方法不变。
 */
public class PatchTvBoxConfig {
 static final String ACTIVITY="Lcom/guocheng/mediapocket/TvBoxActivity;";
 static final String METHOD="lambda$6$com-guocheng-mediapocket-TvBoxActivity";
 static final String OLD="Lcom/guocheng/mediapocket/TvBoxNet;";
 static final String NOW="Lcom/guocheng/mediapocket/TvBoxConfigText;";
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static boolean fetch(MethodReference ref,String owner,String name){
  return ref.getDefiningClass().equals(owner)&&ref.getName().equals(name)&&ref.getParameterTypes().size()==3
   &&ref.getParameterTypes().get(0).equals("Ljava/lang/String;")&&ref.getParameterTypes().get(1).equals("Lorg/json/JSONObject;")
   &&ref.getParameterTypes().get(2).equals("Ljava/lang/String;")&&ref.getReturnType().equals("Ljava/lang/String;");
 }
 static boolean target(Method m){return m.getDefiningClass().equals(ACTIVITY)&&m.getName().equals(METHOD)&&m.getParameterTypes().size()==1&&m.getReturnType().equals("Ljava/lang/Runnable;");}
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));
  ArrayList<ClassDef> classes=new ArrayList<>();int patches=0;
  for(ClassDef c:dex.getClasses()){
   if(!c.getType().equals(ACTIVITY)){classes.add(c);continue;}
   ArrayList<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    if(!target(m)||m.getImplementation()==null){methods.add(m);continue;}
    MutableMethodImplementation code=new MutableMethodImplementation(m.getImplementation());
    int hit=-1;
    for(int i=0;i<code.getInstructions().size();i++){
     Instruction ins=code.getInstructions().get(i);
     if(!(ins instanceof ReferenceInstruction)||!(((ReferenceInstruction)ins).getReference() instanceof MethodReference))continue;
     MethodReference ref=(MethodReference)((ReferenceInstruction)ins).getReference();
     if(!fetch(ref,OLD,"fetch"))continue;
     require(hit<0,"lambda$6 calls TvBoxNet.fetch more than once");
     ImmutableMethodReference next=new ImmutableMethodReference(NOW,"fetchConfig",ref.getParameterTypes(),ref.getReturnType());
     if(ins.getOpcode()==Opcode.INVOKE_STATIC){
      FiveRegisterInstruction five=(FiveRegisterInstruction)ins;
      code.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,five.getRegisterCount(),five.getRegisterC(),five.getRegisterD(),five.getRegisterE(),five.getRegisterF(),five.getRegisterG(),next));
     }else if(ins.getOpcode()==Opcode.INVOKE_STATIC_RANGE){
      RegisterRangeInstruction range=(RegisterRangeInstruction)ins;
      code.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,range.getStartRegister(),range.getRegisterCount(),next));
     }else throw new AssertionError("unexpected fetch opcode "+ins.getOpcode());
     hit=i;
    }
    require(hit>=0,"lambda$6 does not call TvBoxNet.fetch");
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
    patches++;
   }
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  require(patches==1,"expected one lambda$6, found "+patches);
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));
  DexFile old=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));
  DexFile updated=DexFileFactory.loadDexFile(new File(args[1]),Opcodes.forApi(26));
  Map<String,Method> defs=new HashMap<>();
  for(ClassDef c:updated.getClasses())for(Method m:c.getMethods())defs.put(key(m),m);
  int same=0,changed=0;
  for(ClassDef c:old.getClasses())for(Method m:c.getMethods()){
   Method n=defs.remove(key(m));require(n!=null,"missing "+key(m));
   require(m.getAccessFlags()==n.getAccessFlags(),"flags "+key(m));
   if(m.getImplementation()==null){require(n.getImplementation()==null,"impl "+key(m));same++;continue;}
   List<Instruction> a=list(m.getImplementation()),b=list(n.getImplementation());
   require(a.size()==b.size(),"instruction count "+key(m));
   require(m.getImplementation().getRegisterCount()==n.getImplementation().getRegisterCount(),"registers "+key(m));
   for(int i=0;i<a.size();i++){
    if(target(m)&&fetchCall(a.get(i),OLD,"fetch")){
     require(fetchCall(b.get(i),NOW,"fetchConfig"),"replacement "+i);
     require(a.get(i).getOpcode()==b.get(i).getOpcode(),"opcode "+i);
     require(registers(a.get(i)).equals(registers(b.get(i))),"registers of fetch call");
     changed++;
    }else require(PatchHeaderRoute.fingerprint(a.get(i)).equals(PatchHeaderRoute.fingerprint(b.get(i))),"instruction changed "+key(m)+" #"+i);
   }
   same++;
  }
  require(defs.isEmpty(),"unexpected methods "+defs.keySet());
  require(changed==1,"replacements "+changed);
  System.out.println("PASS: TvBoxActivity import/refresh calls TvBoxConfigText.fetchConfig; "+same+" methods otherwise unchanged.");
 }
 static boolean fetchCall(Instruction ins,String owner,String name){return ins instanceof ReferenceInstruction&&((ReferenceInstruction)ins).getReference() instanceof MethodReference&&fetch((MethodReference)((ReferenceInstruction)ins).getReference(),owner,name);}
 static String registers(Instruction ins){if(ins instanceof FiveRegisterInstruction){FiveRegisterInstruction f=(FiveRegisterInstruction)ins;return f.getRegisterCount()+":"+f.getRegisterC()+","+f.getRegisterD()+","+f.getRegisterE()+","+f.getRegisterF()+","+f.getRegisterG();}RegisterRangeInstruction r=(RegisterRangeInstruction)ins;return r.getStartRegister()+"+"+r.getRegisterCount();}
 static String key(Method m){return m.getDefiningClass()+"->"+m.getName()+m.getParameterTypes()+m.getReturnType();}
 static List<Instruction> list(MethodImplementation code){ArrayList<Instruction> out=new ArrayList<>();for(Instruction i:code.getInstructions())out.add(i);return out;}
}
