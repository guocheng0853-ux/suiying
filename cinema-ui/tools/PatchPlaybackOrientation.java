import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.reference.*;
/**
 * GesturePlayerActivity 生命周期开头各加一条静态调用：
 *   onCreate  → PlaybackOrientation.enter   （先于 super.onCreate）
 *   onPause   → PlaybackOrientation.onPause （仅 finish 时恢复方向）
 *   onDestroy → PlaybackOrientation.onDestroy
 * 原方法体其余指令、寄存器数，以及其他所有方法逐指令不变。
 */
public class PatchPlaybackOrientation {
 static final String PLAYER="Lcom/guocheng/mediapocket/GesturePlayerActivity;",HOOK="Lcom/guocheng/mediapocket/PlaybackOrientation;";
 static String hook(Method m){if(!m.getDefiningClass().equals(PLAYER))return null;if(m.getName().equals("onCreate")&&m.getParameterTypes().size()==1)return "enter";if(m.getName().equals("onPause")&&m.getParameterTypes().isEmpty())return "onPause";if(m.getName().equals("onDestroy")&&m.getParameterTypes().isEmpty())return "onDestroy";return null;}
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));ArrayList<ClassDef> classes=new ArrayList<>();int patches=0;
  for(ClassDef c:dex.getClasses()){
   if(!c.getType().equals(PLAYER)){classes.add(c);continue;}
   ArrayList<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    String name=hook(m);if(name==null){methods.add(m);continue;}
    MethodImplementation impl=m.getImplementation();require(impl!=null,"missing code "+name);
    MutableMethodImplementation code=new MutableMethodImplementation(impl);
    boolean isStatic=(m.getAccessFlags()&8)!=0;int thisReg=code.getRegisterCount()-m.getParameters().size()-(isStatic?0:1);
    require(thisReg>=0&&thisReg<=15,"this register out of 35c range for "+name+": v"+thisReg);
    code.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,thisReg,0,0,0,0,new ImmutableMethodReference(HOOK,name,Collections.singletonList("Ljava/lang/Object;"),"V")));
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));patches++;
   }
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  require(patches==3,"Expected onCreate, onPause and onDestroy hooks; found "+patches);
  DexFileFactory.writeDexFile(args[1],new ImmutableDexFile(dex.getOpcodes(),classes));
  DexFile old=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26)),updated=DexFileFactory.loadDexFile(new File(args[1]),Opcodes.forApi(26));
  Map<String,Method> defs=new HashMap<>();for(ClassDef c:updated.getClasses())for(Method m:c.getMethods())defs.put(key(m),m);int count=0;
  for(ClassDef c:old.getClasses())for(Method m:c.getMethods()){
   Method n=defs.remove(key(m));require(n!=null,"Missing "+key(m));require(m.getAccessFlags()==n.getAccessFlags(),"flags "+key(m));
   if(m.getImplementation()==null){require(n.getImplementation()==null,"impl "+key(m));continue;}
   List<Instruction> a=list(m.getImplementation()),b=list(n.getImplementation());String name=hook(m);int skip=0;
   if(name!=null){skip=1;require(b.get(0).getOpcode()==Opcode.INVOKE_STATIC,"hook opcode");Reference ref=((ReferenceInstruction)b.get(0)).getReference();require(ref.toString().contains(HOOK+"->"+name),"hook target "+ref);require(m.getImplementation().getRegisterCount()==n.getImplementation().getRegisterCount(),"register count");}
   require(b.size()==a.size()+skip,"instruction count "+key(m));
   for(int i=0;i<a.size();i++)require(fingerprint(a.get(i)).equals(fingerprint(b.get(i+skip))),"instruction changed "+key(m)+" #"+i);
   count++;
  }
  require(defs.isEmpty(),"Unexpected new methods "+defs.keySet());
  System.out.println("PASS: GesturePlayerActivity onCreate/onPause/onDestroy call PlaybackOrientation; "+count+" original method bodies preserved instruction-for-instruction.");
 }
 static String key(Method m){return m.getDefiningClass()+"->"+m.getName()+m.getParameterTypes()+m.getReturnType();}
 static List<Instruction> list(MethodImplementation code){List<Instruction> out=new ArrayList<>();for(Instruction i:code.getInstructions())out.add(i);return out;}
 static void getters(Class<?> c,Map<String,java.lang.reflect.Method> out){for(Class<?> i:c.getInterfaces()){for(java.lang.reflect.Method m:i.getMethods())if(m.getParameterCount()==0&&m.getName().startsWith("get"))out.put(m.getName(),m);getters(i,out);}if(c.getSuperclass()!=null)getters(c.getSuperclass(),out);}
 static String fingerprint(Instruction ins)throws Exception{Map<String,java.lang.reflect.Method> g=new TreeMap<>();getters(ins.getClass(),g);StringBuilder s=new StringBuilder();for(Map.Entry<String,java.lang.reflect.Method> e:g.entrySet()){Object v=e.getValue().invoke(ins);s.append(e.getKey()).append('=');if(v instanceof Iterable){for(Object x:(Iterable<?>)v){if(x instanceof SwitchElement)s.append(((SwitchElement)x).getKey()).append(':').append(((SwitchElement)x).getOffset());else s.append(x);s.append(',');}}else s.append(v);s.append(';');}return s.toString();}
}
