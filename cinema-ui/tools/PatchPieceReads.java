import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.reference.*;
public class PatchPieceReads {
 public static void main(String[] a)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(a[0]),Opcodes.forApi(26));List<ClassDef> out=new ArrayList<>();int removed=0,calls=0;
  String reader="Lcom/guocheng/mediapocket/VerifiedPieceReader",handle="Lcom/frostwire/jlibtorrent/TorrentHandle;";
  for(ClassDef c:dex.getClasses()){
   if(c.getType().equals(reader+";")||c.getType().equals(reader+"$Pieces;")){removed++;continue;}
   if(!c.getType().startsWith("Lcom/guocheng/mediapocket/TorrentEngine")){out.add(c);continue;}
   List<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    if(m.getImplementation()==null){methods.add(m);continue;}
    MutableMethodImplementation code=new MutableMethodImplementation(m.getImplementation());
    for(int i=0;i<code.getInstructions().size();i++){
     Instruction x=code.getInstructions().get(i);
     if(!(x instanceof ReferenceInstruction))continue;
     Object ref=((ReferenceInstruction)x).getReference();if(!(ref instanceof MethodReference))continue;
     MethodReference mr=(MethodReference)ref;
     if(!mr.getDefiningClass().equals(handle)||!mr.getName().equals("setPieceDeadline"))continue;
     if(x.getOpcode()!=Opcode.INVOKE_VIRTUAL||!(x instanceof FiveRegisterInstruction)||!mr.getParameterTypes().toString().equals("[I, I]"))throw new AssertionError("Unexpected deadline ABI");
     FiveRegisterInstruction old=(FiveRegisterInstruction)x;
     code.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,old.getRegisterC(),old.getRegisterD(),old.getRegisterE(),0,0,new ImmutableMethodReference("Lcom/guocheng/mediapocket/TorrentBoost;","deadline",Arrays.asList(handle,"I","I"),"V")));calls++;
    }
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
   }
   out.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  if(removed!=2||calls<3)throw new AssertionError("Expected reader pair and deadline sites: "+removed+" / "+calls);
  DexFileFactory.writeDexFile(a[1],new ImmutableDexFile(dex.getOpcodes(),out));
  System.out.println("PASS: replaced reader/interface and "+calls+" deadline calls (same registers and code size)");
 }
}
