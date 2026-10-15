import javax.tools.*;import java.util.*;
/** ecj 命令行兼容层：把 build_ui.py 使用的 ecj 参数翻译给 JDK javac。 */
public class EcjShim{public static void main(String[] a){List<String> o=new ArrayList<>();for(String s:a){switch(s){case "-8":o.addAll(List.of("-source","8","-target","8","-Xlint:-options"));break;case "-17":o.addAll(List.of("--release","17"));break;case "-nowarn":o.add("-nowarn");o.add("-Xlint:none");break;default:o.add(s);}}
 o.add(0,"-encoding");o.add(1,"UTF-8");int r=ToolProvider.getSystemJavaCompiler().run(null,null,null,o.toArray(new String[0]));System.exit(r);}}
