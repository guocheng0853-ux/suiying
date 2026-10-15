import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.reference.*;
/** Add a host-scoped compatibility branch; preserve the original bytes method as fallback for all other hosts. */
public class PatchClawNet {
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));ArrayList<ClassDef> classes=new ArrayList<>();int patches=0;
  for(ClassDef c:dex.getClasses()){
   if(!c.getType().equals("Lcom/guocheng/mediapocket/Net;")){classes.add(c);continue;}
   ArrayList<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    if(!m.getName().equals("bytes")||!m.getReturnType().equals("[B")){methods.add(m);continue;}
    MutableMethodImplementation code=new MutableMethodImplementation(m.getImplementation());int param=code.getRegisterCount()-1;if(param<1)throw new IllegalStateException("Insufficient scratch registers");String type="Lcom/guocheng/mediapocket/ClawCompatibleNetwork;";
    code.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,param,1,new ImmutableMethodReference(type,"handles",Collections.singletonList("Ljava/lang/String;"),"Z")));
    code.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
    code.addInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(0)));
    code.addInstruction(3,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,param,1,new ImmutableMethodReference(type,"bytes",Collections.singletonList("Ljava/lang/String;"),"[B")));
    code.addInstruction(4,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,0));
    code.addInstruction(5,new BuilderInstruction11x(Opcode.RETURN_OBJECT,0));
    code.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(6)));
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));patches++;
   }
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  if(patches!=1)throw new IllegalStateException("Expected exactly one Net.bytes patch; found "+patches);
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));System.out.println("Added host-scoped branch to Net.bytes; original fallback retained.");
 }
}
