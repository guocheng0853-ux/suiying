import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.reference.*;
/**
 * 在 VlcPlayback.setSource(String,Map) 开头前插 4 条指令：
 *   url = HeaderRoute.url(url, headers); headers = HeaderRoute.headers(headers);
 * 原方法体其余指令、寄存器数、其他所有方法逐指令不变（写出后重新载入逐条比对）。
 */
public class PatchHeaderRoute {
 static final String VLC="Lcom/guocheng/mediapocket/VlcPlayback;",ROUTE="Lcom/guocheng/mediapocket/HeaderRoute;";
 static boolean target(Method m){return m.getDefiningClass().equals(VLC)&&m.getName().equals("setSource")&&m.getParameterTypes().size()==2&&m.getParameterTypes().get(0).toString().equals("Ljava/lang/String;")&&m.getParameterTypes().get(1).toString().equals("Ljava/util/Map;");}
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));ArrayList<ClassDef> classes=new ArrayList<>();int patches=0;
  for(ClassDef c:dex.getClasses()){
   if(!c.getType().equals(VLC)){classes.add(c);continue;}
   ArrayList<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    if(!target(m)){methods.add(m);continue;}
    MutableMethodImplementation code=new MutableMethodImplementation(m.getImplementation());int regs=code.getRegisterCount(),url=regs-2,map=regs-1;require(map<=15,"35c needs registers <= v15");
    code.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,url,map,0,0,0,new ImmutableMethodReference(ROUTE,"url",Arrays.asList("Ljava/lang/String;","Ljava/util/Map;"),"Ljava/lang/String;")));
    code.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,url));
    code.addInstruction(2,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,map,0,0,0,0,new ImmutableMethodReference(ROUTE,"headers",Collections.singletonList("Ljava/util/Map;"),"Ljava/util/Map;")));
    code.addInstruction(3,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,map));
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));patches++;
   }
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  require(patches==1,"Expected exactly one VlcPlayback.setSource; found "+patches);
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));
  // 重新载入逐条比对
  DexFile old=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26)),updated=DexFileFactory.loadDexFile(new File(args[1]),Opcodes.forApi(26));
  Map<String,Method> defs=new HashMap<>();for(ClassDef c:updated.getClasses())for(Method m:c.getMethods())defs.put(key(m),m);int count=0;
  for(ClassDef c:old.getClasses())for(Method m:c.getMethods()){
   Method n=defs.remove(key(m));require(n!=null,"Missing "+key(m));require(m.getAccessFlags()==n.getAccessFlags(),"flags "+key(m));
   if(m.getImplementation()==null){require(n.getImplementation()==null,"impl "+key(m));continue;}
   List<Instruction> a=list(m.getImplementation()),b=list(n.getImplementation());int skip=0;
   if(target(m)){skip=4;Opcode[] ops={Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT_OBJECT,Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT_OBJECT};for(int i=0;i<4;i++)require(b.get(i).getOpcode()==ops[i],"inserted op "+i);require(m.getImplementation().getRegisterCount()==n.getImplementation().getRegisterCount(),"register count changed");}
   require(b.size()==a.size()+skip,"instruction count "+key(m));
   for(int i=0;i<a.size();i++)require(fingerprint(a.get(i)).equals(fingerprint(b.get(i+skip))),"instruction changed "+key(m)+" #"+i);
   count++;
  }
  require(defs.isEmpty(),"Unexpected new methods "+defs.keySet());
  System.out.println("PASS: VlcPlayback.setSource routes headers via HeaderRoute (4 prepended instructions); "+count+" original method bodies preserved instruction-for-instruction.");
 }
 static String key(Method m){return m.getDefiningClass()+"->"+m.getName()+m.getParameterTypes()+m.getReturnType();}
 static List<Instruction> list(MethodImplementation code){List<Instruction> out=new ArrayList<>();for(Instruction i:code.getInstructions())out.add(i);return out;}
 static void getters(Class<?> c,Map<String,java.lang.reflect.Method> out){for(Class<?> i:c.getInterfaces()){for(java.lang.reflect.Method m:i.getMethods())if(m.getParameterCount()==0&&m.getName().startsWith("get"))out.put(m.getName(),m);getters(i,out);}if(c.getSuperclass()!=null)getters(c.getSuperclass(),out);}
 static String fingerprint(Instruction ins)throws Exception{Map<String,java.lang.reflect.Method> g=new TreeMap<>();getters(ins.getClass(),g);StringBuilder s=new StringBuilder();for(Map.Entry<String,java.lang.reflect.Method> e:g.entrySet()){Object v=e.getValue().invoke(ins);s.append(e.getKey()).append('=');if(v instanceof Iterable){for(Object x:(Iterable<?>)v){if(x instanceof SwitchElement)s.append(((SwitchElement)x).getKey()).append(':').append(((SwitchElement)x).getOffset());else s.append(x);s.append(',');}}else s.append(v);s.append(';');}return s.toString();}
}
