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

/**
 * 自动档不再因为片源名里的 10-bit / HDR / DV 一开始就软解。
 * VlcPlayback 四参数构造里的 DecodePolicy.software 改调 DecodePolicyRules.software（只有显式 software 才返回 true）。
 * 黑屏仍走原来的 DecodePolicy.retry。解码菜单文案改成「优先硬解，黑屏再软解」。
 */
public class PatchDecodePolicy {
 static final String OLD="自动 · 10-bit / HDR 优先软件",NEXT="自动 · 优先硬解，黑屏再软解";
 static final String FROM="Lcom/guocheng/mediapocket/DecodePolicy;->software(Ljava/lang/String;Ljava/lang/String;)Z";
 static final String TO="Lcom/guocheng/mediapocket/DecodePolicyRules;->software(Ljava/lang/String;Ljava/lang/String;)Z";
 static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));List<ClassDef> classes=new ArrayList<>();Map<String,Set<Integer>> sites=new HashMap<>();int calls=0,labels=0;
  for(ClassDef c:dex.getClasses()){
   List<Method> methods=new ArrayList<>();boolean classTouched=false;
   for(Method m:c.getMethods()){
    MethodImplementation impl=m.getImplementation();if(impl==null){methods.add(m);continue;}
    List<Instruction> before=PatchPlaybackOrientation.list(impl);MutableMethodImplementation code=new MutableMethodImplementation(impl);Set<Integer> replaced=new HashSet<>();
    for(int i=0;i<before.size();i++){
     Instruction ins=before.get(i);
     if(ins.getOpcode()==Opcode.CONST_STRING&&ins instanceof ReferenceInstruction){
      Reference ref=((ReferenceInstruction)ins).getReference();
      if(ref instanceof StringReference&&OLD.equals(((StringReference)ref).getString())){
       int reg=((OneRegisterInstruction)ins).getRegisterA();
       code.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,reg,new ImmutableStringReference(NEXT)));
       replaced.add(i);labels++;classTouched=true;
      }
     }
     if(ins.getOpcode()==Opcode.INVOKE_STATIC&&ins instanceof ReferenceInstruction&&FROM.equals(((ReferenceInstruction)ins).getReference().toString())){
      FiveRegisterInstruction call=(FiveRegisterInstruction)ins;
      ImmutableMethodReference target=new ImmutableMethodReference("Lcom/guocheng/mediapocket/DecodePolicyRules;","software",Arrays.asList("Ljava/lang/String;","Ljava/lang/String;"),"Z");
      code.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,call.getRegisterCount(),call.getRegisterC(),call.getRegisterD(),call.getRegisterE(),call.getRegisterF(),call.getRegisterG(),target));
      replaced.add(i);calls++;classTouched=true;
     }
    }
    if(!replaced.isEmpty())sites.put(PatchPlaybackOrientation.key(m),replaced);
    methods.add(replaced.isEmpty()?m:new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
   }
   classes.add(classTouched?touchedClass(c,methods):c);
  }
  require(calls==1,"Expected exactly one DecodePolicy.software call; got "+calls);
  require(labels==2,"Expected the player menu and the settings menu auto-decode labels; got "+labels);
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));
  DexFile updated=DexFileFactory.loadDexFile(new File(args[1]),Opcodes.forApi(26));Map<String,Method> newer=new HashMap<>();for(ClassDef c:updated.getClasses())for(Method m:c.getMethods())newer.put(PatchPlaybackOrientation.key(m),m);
  for(ClassDef c:dex.getClasses())for(Method m:c.getMethods()){
   String key=PatchPlaybackOrientation.key(m);Method n=newer.remove(key);require(n!=null&&m.getAccessFlags()==n.getAccessFlags(),"method ABI changed "+key);
   if(m.getImplementation()==null){require(n.getImplementation()==null,"code added "+key);continue;}
   require(m.getImplementation().getRegisterCount()==n.getImplementation().getRegisterCount(),"registers changed "+key);
   List<Instruction> a=PatchPlaybackOrientation.list(m.getImplementation()),b=PatchPlaybackOrientation.list(n.getImplementation());require(a.size()==b.size(),"instruction count changed "+key);
   for(int i=0;i<a.size();i++){
    require(a.get(i).getCodeUnits()==b.get(i).getCodeUnits(),"code size changed "+key);
    boolean site=sites.containsKey(key)&&sites.get(key).contains(i);
    if(!site)require(PatchPlaybackOrientation.fingerprint(a.get(i)).equals(PatchPlaybackOrientation.fingerprint(b.get(i))),"original instruction changed "+key+" #"+i);
    else if(a.get(i).getOpcode()==Opcode.INVOKE_STATIC)require(((ReferenceInstruction)b.get(i)).getReference().toString().equals(TO),"hook target");
    else require(b.get(i).getOpcode()==Opcode.CONST_STRING&&NEXT.equals(((StringReference)((ReferenceInstruction)b.get(i)).getReference()).getString()),"label text");
   }
  }
  require(newer.isEmpty(),"unexpected methods");System.out.println("PASS: auto decode prefers hardware; menu label updated; other instructions preserved.");
 }
 static ClassDef touchedClass(ClassDef c,List<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
}
