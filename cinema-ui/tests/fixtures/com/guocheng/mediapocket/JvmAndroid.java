package com.guocheng.mediapocket;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
/** SDK 的 android.jar 没有方法体。播放器测试在这个加载器里跑，android.* 换成最小替身。 */
public final class JvmAndroid {
 private JvmAndroid(){}
 public static void call(String className,String method)throws Exception{
  Path root=Path.of("").toAbsolutePath();
  List<Path> roots=new ArrayList<>();
  roots.add(root.resolve("build/tests"));
  roots.add(root.resolve("build/classes"));
  try(DirectoryStream<Path> ds=Files.newDirectoryStream(root.resolve("runtime-libs"),"*.jar")){for(Path p:ds)roots.add(p);}
  StubLoader loader=new StubLoader(JvmAndroid.class.getClassLoader(),roots.toArray(new Path[0]));
  try{loader.loadClass(className).getMethod(method).invoke(null);}
  catch(java.lang.reflect.InvocationTargetException e){Throwable c=e.getCause();if(c instanceof Exception)throw (Exception)c;if(c instanceof Error)throw (Error)c;throw e;}
 }
}

class StubLoader extends ClassLoader {
  final List<Path> roots=new ArrayList<>();
  StubLoader(ClassLoader parent, Path... roots){super(parent);Collections.addAll(this.roots,roots);}
  @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    synchronized(getClassLoadingLock(name)){
      Class<?> c=findLoadedClass(name);
      if(c==null){
        if(name.startsWith("android."))c=defineAndroid(name);
        else if(name.startsWith("androidx.media3.")||name.startsWith("com.guocheng.mediapocket.")||name.startsWith("com.google.common."))c=defineFromRoots(name);
        else c=getParent().loadClass(name);
      }
      if(resolve)resolveClass(c);
      return c;
    }
  }
  Class<?> defineAndroid(String name){
    String internal=name.replace('.','/');
    if(name.equals("android.os.Build"))return defineClass(name,buildClass(),0,buildClass().length);
    if(name.equals("android.os.Build$VERSION"))return defineClass(name,versionClass(),0,versionClass().length);
    if(name.equals("android.net.Uri"))return defineClass(name,uriClass(),0,uriClass().length);
    String superName=name.endsWith("Exception")||name.endsWith("Error")?"java/lang/Exception":"java/lang/Object";
    byte[] b=empty(internal,superName);
    return defineClass(name,b,0,b.length);
  }
  Class<?> defineFromRoots(String name)throws ClassNotFoundException{
    String rel=name.replace('.','/')+".class";
    for(Path root:roots){
      try{
        byte[] b;
        if(root.toString().endsWith(".jar")){
          try(JarFile jar=new JarFile(root.toFile())){JarEntry e=jar.getJarEntry(rel);if(e==null)continue;b=jar.getInputStream(e).readAllBytes();}
        }else{Path p=root.resolve(rel);if(!Files.exists(p))continue;b=Files.readAllBytes(p);}
        return defineClass(name,b,0,b.length);
      }catch(IOException e){throw new ClassNotFoundException(name,e);}
    }
    throw new ClassNotFoundException(name);
  }
  static byte[] empty(String internal,String superName){return klass(internal,superName,null);}
  static byte[] buildClass(){return klass("android/os/Build","java/lang/Object",new String[]{"DEVICE","MANUFACTURER","MODEL"});}
  static byte[] versionClass(){return klassInt("android/os/Build$VERSION","java/lang/Object","SDK_INT",35);}
  static byte[] uriClass(){return uri();}
  // --- tiny class-file writer: public class with default ctor, optional public static String fields ---
  static byte[] klass(String name,String sup,String[] strings){
    // Use a pre-baked approach via ASM-less constant pool. Implemented in writer below.
    return ClassBytes.object(name,sup,strings);
  }
  static byte[] klassInt(String name,String sup,String field,int value){return ClassBytes.intField(name,sup,field,value);}
  static byte[] uri(){return ClassBytes.uri();}
}

class ClassBytes {
  static byte[] object(String name,String sup,String[] strings){
    Cp cp=new Cp();
    int thisC=cp.cls(name),superC=cp.cls(sup),code=cp.utf("Code"),init=cp.utf("<init>"),vd=cp.utf("()V"),objInit=cp.nat(cp.utf("<init>"),vd),objM=cp.mtd(superC,objInit);
    int n=strings==null?0:strings.length;
    int[] fs=new int[n], fd=new int[n];
    for(int i=0;i<n;i++){fs[i]=cp.utf(strings[i]);fd[i]=cp.utf("Ljava/lang/String;");}
    ByteArrayOutputStream o=new ByteArrayOutputStream();
    // we'll fill after pool
    byte[] pool=cp.bytes();
    DataOutputStream d=new DataOutputStream(o);
    try{
      d.writeInt(0xCAFEBABE);d.writeShort(0);d.writeShort(55);d.write(pool);
      d.writeShort(0x0021);d.writeShort(thisC);d.writeShort(superC);d.writeShort(0);d.writeShort(n);
      for(int i=0;i<n;i++){d.writeShort(0x0009);d.writeShort(fs[i]);d.writeShort(fd[i]);d.writeShort(0);} // public static
      d.writeShort(1); // ctor
      d.writeShort(0x0001);d.writeShort(init);d.writeShort(vd);d.writeShort(1);d.writeShort(code);
      byte[] codeb=new byte[]{0x2a,(byte)0xb7,0,0,(byte)0xb1}; // aload_0 invokespecial return
      codeb[2]=(byte)(objM>>8);codeb[3]=(byte)objM;
      d.writeInt(codeb.length+12);d.writeShort(1);d.writeShort(1);d.writeInt(codeb.length);d.write(codeb);d.writeShort(0);d.writeShort(0);
      d.writeShort(0);
      d.flush();
    }catch(IOException e){throw new RuntimeException(e);}
    // static strings are null by default. clinit of Util only reads them. null is ok for String concat.
    return o.toByteArray();
  }
  static byte[] intField(String name,String sup,String field,int value){
    Cp cp=new Cp();
    int thisC=cp.cls(name),superC=cp.cls(sup),code=cp.utf("Code"),init=cp.utf("<init>"),vd=cp.utf("()V"),objInit=cp.nat(cp.utf("<init>"),vd),objM=cp.mtd(superC,objInit);
    int fn=cp.utf(field),fd=cp.utf("I"),clinit=cp.utf("<clinit>"),stat=cp.nat(fn,fd),put=cp.fld(thisC,stat);
    ByteArrayOutputStream o=new ByteArrayOutputStream();
    DataOutputStream d=new DataOutputStream(o);
    try{
      d.writeInt(0xCAFEBABE);d.writeShort(0);d.writeShort(55);d.write(poolBytes(cp));
      d.writeShort(0x0021);d.writeShort(thisC);d.writeShort(superC);d.writeShort(0);d.writeShort(1);
      d.writeShort(0x0019);d.writeShort(fn);d.writeShort(fd);d.writeShort(0); // public static final, value set in clinit
      d.writeShort(2);
      writeCtor(d,init,vd,code,objM);
      d.writeShort(0x0008);d.writeShort(clinit);d.writeShort(vd);d.writeShort(1);d.writeShort(code);
      // sipush value; putstatic; return
      byte[] cb=new byte[]{0x11,0,0,(byte)0xb3,0,0,(byte)0xb1};
      cb[1]=(byte)(value>>8);cb[2]=(byte)value;cb[4]=(byte)(put>>8);cb[5]=(byte)put;
      d.writeInt(cb.length+12);d.writeShort(1);d.writeShort(0);d.writeInt(cb.length);d.write(cb);d.writeShort(0);d.writeShort(0);
      d.writeShort(0);d.flush();
    }catch(IOException e){throw new RuntimeException(e);}
    return o.toByteArray();
  }
  static void writeCtor(DataOutputStream d,int init,int vd,int code,int objM)throws IOException{
    d.writeShort(0x0001);d.writeShort(init);d.writeShort(vd);d.writeShort(1);d.writeShort(code);
    byte[] codeb=new byte[]{0x2a,(byte)0xb7,0,0,(byte)0xb1};
    codeb[2]=(byte)(objM>>8);codeb[3]=(byte)objM;
    d.writeInt(codeb.length+12);d.writeShort(1);d.writeShort(1);d.writeInt(codeb.length);d.write(codeb);d.writeShort(0);d.writeShort(0);
  }
  static byte[] poolBytes(Cp cp){return cp.bytes();}
  static byte[] uri(){
    Cp cp=new Cp();
    int thisC=cp.cls("android/net/Uri"),superC=cp.cls("java/lang/Object");
    int code=cp.utf("Code"),init=cp.utf("<init>"),vd=cp.utf("()V"),strInitDesc=cp.utf("(Ljava/lang/String;)V");
    int objInit=cp.nat(cp.utf("<init>"),vd),objM=cp.mtd(superC,objInit);
    int value=cp.utf("value"),strT=cp.utf("Ljava/lang/String;"),field=cp.fld(thisC,cp.nat(value,strT));
    int parseN=cp.utf("parse"),parseD=cp.utf("(Ljava/lang/String;)Landroid/net/Uri;");
    int toString=cp.utf("toString"),toStringD=cp.utf("()Ljava/lang/String;");
    int uriInit=cp.mtd(thisC,cp.nat(init,strInitDesc));
    ByteArrayOutputStream o=new ByteArrayOutputStream();
    DataOutputStream d=new DataOutputStream(o);
    try{
      d.writeInt(0xCAFEBABE);d.writeShort(0);d.writeShort(55);d.write(cp.bytes());
      d.writeShort(0x0021);d.writeShort(thisC);d.writeShort(superC);d.writeShort(0);
      d.writeShort(1);d.writeShort(0x0002);d.writeShort(value);d.writeShort(strT);d.writeShort(0); // private value
      d.writeShort(3);
      // <init>(String)
      d.writeShort(0x0002);d.writeShort(init);d.writeShort(strInitDesc);d.writeShort(1);d.writeShort(code);
      byte[] ctor=new byte[]{0x2a,(byte)0xb7,0,0,0x2a,0x2b,(byte)0xb5,0,0,(byte)0xb1};
      ctor[2]=(byte)(objM>>8);ctor[3]=(byte)objM;ctor[7]=(byte)(field>>8);ctor[8]=(byte)field;
      d.writeInt(ctor.length+12);d.writeShort(2);d.writeShort(2);d.writeInt(ctor.length);d.write(ctor);d.writeShort(0);d.writeShort(0);
      // parse
      d.writeShort(0x0009);d.writeShort(parseN);d.writeShort(parseD);d.writeShort(1);d.writeShort(code);
      byte[] parse=new byte[]{(byte)0xbb,0,0,0x59,0x2a,(byte)0xb7,0,0,(byte)0xb0};
      parse[1]=(byte)(thisC>>8);parse[2]=(byte)thisC;parse[6]=(byte)(uriInit>>8);parse[7]=(byte)uriInit;
      d.writeInt(parse.length+12);d.writeShort(3);d.writeShort(1);d.writeInt(parse.length);d.write(parse);d.writeShort(0);d.writeShort(0);
      // toString
      d.writeShort(0x0001);d.writeShort(toString);d.writeShort(toStringD);d.writeShort(1);d.writeShort(code);
      byte[] ts=new byte[]{0x2a,(byte)0xb4,0,0,(byte)0xb0};
      ts[2]=(byte)(field>>8);ts[3]=(byte)field;
      d.writeInt(ts.length+12);d.writeShort(1);d.writeShort(1);d.writeInt(ts.length);d.write(ts);d.writeShort(0);d.writeShort(0);
      d.writeShort(0);d.flush();
    }catch(IOException e){throw new RuntimeException(e);}
    return o.toByteArray();
  }
}
class Cp {
  final ByteArrayOutputStream body=new ByteArrayOutputStream();
  final DataOutputStream d=new DataOutputStream(body);
  int n=1;
  final Map<String,Integer> utf=new HashMap<>(),cls=new HashMap<>();
  int utf(String s){Integer i=utf.get(s);if(i!=null)return i;try{d.writeByte(1);d.writeUTF(s);}catch(IOException e){throw new RuntimeException(e);}utf.put(s,n);return n++;}
  int cls(String name){Integer i=cls.get(name);if(i!=null)return i;int u=utf(name);try{d.writeByte(7);d.writeShort(u);}catch(IOException e){throw new RuntimeException(e);}cls.put(name,n);return n++;}
  int nat(int name,int desc){try{d.writeByte(12);d.writeShort(name);d.writeShort(desc);}catch(IOException e){throw new RuntimeException(e);}return n++;}
  int mtd(int cls,int nat){try{d.writeByte(10);d.writeShort(cls);d.writeShort(nat);}catch(IOException e){throw new RuntimeException(e);}return n++;}
  int fld(int cls,int nat){try{d.writeByte(9);d.writeShort(cls);d.writeShort(nat);}catch(IOException e){throw new RuntimeException(e);}return n++;}
  byte[] bytes(){ByteArrayOutputStream o=new ByteArrayOutputStream();DataOutputStream x=new DataOutputStream(o);try{x.writeShort(n);x.write(body.toByteArray());}catch(IOException e){throw new RuntimeException(e);}return o.toByteArray();}
}
