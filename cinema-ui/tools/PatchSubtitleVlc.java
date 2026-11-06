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
 * 0.1.169：VLC 自己画的文字字幕跟随「字幕设置」。
 *  · PlayerActivity 建 LibVLC 时的 Arrays.asList("--aout=android_audiotrack", …) 改调 SubtitleVlc.libOptions，追加全局 freetype 选项。
 *  · VlcPlayback 里紧跟 ":audio-language=eng,en" 的 Media.addOption 改调 SubtitleVlc.addOption，原选项照加，再追加媒体级选项。
 * 只替换 invoke 的目标（同为 35c、寄存器不变），其余指令逐条比对不变。
 */
public class PatchSubtitleVlc {
 static final String AS_LIST="Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;";
 static final String ADD="Lorg/videolan/libvlc/Media;->addOption(Ljava/lang/String;)V";
 static final ImmutableMethodReference LIB=new ImmutableMethodReference("Lcom/guocheng/mediapocket/SubtitleVlc;","libOptions",Arrays.asList("[Ljava/lang/Object;"),"Ljava/util/List;");
 static final ImmutableMethodReference MEDIA=new ImmutableMethodReference("Lcom/guocheng/mediapocket/SubtitleVlc;","addOption",Arrays.asList("Lorg/videolan/libvlc/Media;","Ljava/lang/String;"),"V");
 static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 static String str(Instruction ins){if(ins instanceof ReferenceInstruction&&((ReferenceInstruction)ins).getReference() instanceof StringReference)return ((StringReference)((ReferenceInstruction)ins).getReference()).getString();return null;}
 static String ref(Instruction ins){return ins instanceof ReferenceInstruction?((ReferenceInstruction)ins).getReference().toString():"";}
 public static void main(String[] args)throws Exception{
  DexFile dex=DexFileFactory.loadDexFile(new File(args[0]),Opcodes.forApi(26));List<ClassDef> classes=new ArrayList<>();Map<String,Set<Integer>> sites=new HashMap<>();int libs=0,medias=0;
  for(ClassDef c:dex.getClasses()){
   boolean player=c.getType().equals("Lcom/guocheng/mediapocket/PlayerActivity;"),vlc=c.getType().equals("Lcom/guocheng/mediapocket/VlcPlayback;");
   if(!player&&!vlc){classes.add(c);continue;}
   List<Method> methods=new ArrayList<>();boolean touched=false;
   for(Method m:c.getMethods()){
    MethodImplementation impl=m.getImplementation();if(impl==null){methods.add(m);continue;}
    List<Instruction> before=PatchPlaybackOrientation.list(impl);MutableMethodImplementation code=new MutableMethodImplementation(impl);Set<Integer> replaced=new HashSet<>();
    for(int i=0;i<before.size();i++){
     String s=str(before.get(i));
     if(player&&"--aout=android_audiotrack".equals(s)){
      for(int k=i+1;k<Math.min(before.size(),i+6);k++){Instruction x=before.get(k);if(x.getOpcode()==Opcode.INVOKE_STATIC&&AS_LIST.equals(ref(x))){FiveRegisterInstruction call=(FiveRegisterInstruction)x;code.replaceInstruction(k,new BuilderInstruction35c(Opcode.INVOKE_STATIC,call.getRegisterCount(),call.getRegisterC(),call.getRegisterD(),call.getRegisterE(),call.getRegisterF(),call.getRegisterG(),LIB));replaced.add(k);libs++;touched=true;break;}}
     }
     if(vlc&&":audio-language=eng,en".equals(s)&&i+1<before.size()){Instruction x=before.get(i+1);if(x.getOpcode()==Opcode.INVOKE_VIRTUAL&&ADD.equals(ref(x))){FiveRegisterInstruction call=(FiveRegisterInstruction)x;code.replaceInstruction(i+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,call.getRegisterCount(),call.getRegisterC(),call.getRegisterD(),call.getRegisterE(),call.getRegisterF(),call.getRegisterG(),MEDIA));replaced.add(i+1);medias++;touched=true;}}
    }
    if(!replaced.isEmpty())sites.put(PatchPlaybackOrientation.key(m),replaced);
    methods.add(replaced.isEmpty()?m:new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
   }
   classes.add(touched?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods):c);
  }
  require(libs==2,"Expected two LibVLC option lists (onCreate, retryCurrentDecoder); got "+libs);
  require(medias==3,"Expected three Media.addOption(:audio-language) sites; got "+medias);
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
    else{String t=ref(b.get(i));require(b.get(i).getOpcode()==Opcode.INVOKE_STATIC&&(t.equals(LIB.toString())||t.equals(MEDIA.toString())),"hook target "+t);}
   }
  }
  require(newer.isEmpty(),"unexpected methods");System.out.println("PASS: VLC subtitle style hooks (2 LibVLC lists, 3 media options); other instructions preserved.");
 }
}
