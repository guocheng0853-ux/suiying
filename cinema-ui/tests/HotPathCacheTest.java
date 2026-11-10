package com.guocheng.mediapocket;
import org.json.*;import java.lang.reflect.*;import java.util.regex.Pattern;
/** 0.1.182 反射缓存、正则缓存、图片缓存清理节流：结果和原来的逐次查找一致，并量一下快了多少。 */
public class HotPathCacheTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class Base{private int vw=1920;private Object video="v";private String hidden(int x){return "base"+x;}}
 static class Mid extends Base{private String top="mid-top";}
 static class Leaf extends Mid{private String top="leaf-top";void ping(){}}
 // 原来的写法，留作对照
 static Object oldField(Object target,String name){for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception ignored){}return null;}
 // 和 OnlineSubtitleSearch.field / call、SourceSubtitleLoader.set 现在的写法一致（那几个类依赖 Android，JVM 里加载不了）
 static Object field(Object target,String name){Field f=ReflectCache.field(target.getClass(),name);if(f==null)return null;try{return f.get(target);}catch(Exception e){return null;}}
 static Object call(Object target,String name,Class<?>[] types,Object...args)throws Exception{Method m=ReflectCache.method(target.getClass(),name,types);if(m==null)throw new NoSuchMethodException(name);return m.invoke(target,args);}
 static void set(Object target,String name,Object value)throws Exception{Field f=ReflectCache.field(target.getClass(),name);if(f==null)throw new NoSuchFieldException(name);f.set(target,value);}
 static boolean oldHas(String u,String re){return Pattern.compile(re).matcher(u).find();}
 public static void main(String[] x)throws Exception{
  Leaf leaf=new Leaf();
  check("leaf-top".equals(field(leaf,"top")),"subclass field shadows the parent one");
  check(Integer.valueOf(1920).equals(field(leaf,"vw")),"inherited private field");
  check(field(leaf,"nope")==null&&field(leaf,"nope")==null,"missing field stays null, also from the cache");
  check("mid-top".equals(field(new Mid(),"top")),"cache is per class, not per name");
  check("base7".equals(call(leaf,"hidden",new Class<?>[]{int.class},7)),"inherited private method");
  boolean threw=false;try{call(leaf,"hidden",new Class<?>[]{String.class},"a");}catch(NoSuchMethodException e){threw=true;}check(threw,"wrong parameter types still throw NoSuchMethodException");
  threw=false;try{call(leaf,"absent",new Class<?>[0]);}catch(NoSuchMethodException e){threw=true;}check(threw,"missing method throws, twice");
  threw=false;try{call(leaf,"absent",new Class<?>[0]);}catch(NoSuchMethodException e){threw=true;}check(threw,"missing method throws from the cache too");
  set(leaf,"vw",1280);check(Integer.valueOf(1280).equals(field(leaf,"vw")),"set through the cache");
  threw=false;try{set(leaf,"nope",1);}catch(NoSuchFieldException e){threw=true;}check(threw,"set on a missing field throws");
  check(ReflectCache.key("m",new Class<?>[]{int.class,String.class}).equals("m,int,java.lang.String")&&ReflectCache.key("m",null).equals("m"),"method key");

  check(Rx.of("a+b")==Rx.of("a+b"),"same regex compiled once");
  check(Rx.of("abc",Pattern.CASE_INSENSITIVE)!=Rx.of("abc")&&Rx.of("abc",Pattern.CASE_INSENSITIVE).matcher("ABC").find()&&!Rx.of("abc").matcher("ABC").find(),"flags are part of the key");
  int before=Rx.size();for(int i=0;i<Rx.LIMIT*2;i++)check(Rx.of("dyn"+i).matcher("dyn"+i).matches(),"dynamic regex still works");check(Rx.size()<=Rx.LIMIT&&Rx.size()>=before,"cache stops growing at the limit: "+Rx.size());

  JSONObject s=new JSONObject().put("name","Torrentio\n4k DV | HDR10+").put("title","Show.S01E03.2160p.WEB-DL.DV.HDR10+.HEVC.DDP5.1.Atmos-GRP\n👤 312 💾 8.42 GB ⚙️ ThePirateBay").put("infoHash","abc");
  StreamInfo i=StreamInfo.parse(s);check(i.height==2160&&"DV+HDR".equals(i.hdr)&&"HEVC".equals(i.codec)&&"Atmos".equals(i.audio)&&i.seeds==312&&i.p2p,"stream parse unchanged: "+i.height+" "+i.hdr+" "+i.codec+" "+i.audio+" "+i.seeds);
  check(i.bytes==8_420_000_000L,"size from the text: "+i.bytes);
  i.height=1;i.hdr="x";StreamInfo again=StreamInfo.parse(s);check(again!=i&&again.height==2160&&"DV+HDR".equals(again.hdr),"each parse returns its own copy; changing one does not poison the memo");
  JSONObject sized=new JSONObject(s.toString()).put("behaviorHints",new JSONObject().put("videoSize",1234567890L));check(StreamInfo.parse(sized).bytes==1234567890L&&StreamInfo.parse(s).bytes==8_420_000_000L,"videoSize hint is read from the entry itself, not from the memo");
  JSONObject direct=new JSONObject(s.toString());direct.remove("infoHash");direct.put("url","https://h/x.mkv");check(!StreamInfo.parse(direct).p2p&&StreamInfo.parse(s).p2p,"same text, different link kind: p2p is not memoized");
  JSONObject split=new JSONObject().put("name","A").put("title","B DV\nHDR10");JSONObject merged=new JSONObject().put("name","A").put("title","B DV HDR10");check("DV".equals(StreamInfo.parse(split).hdr)&&"DV+HDR".equals(StreamInfo.parse(merged).hdr),"first title line decides HDR even when the flattened text is identical");
  for(int k=0;k<StreamInfo.MEMO_LIMIT*2;k++)StreamInfo.parse(new JSONObject().put("title","Filler."+k+".1080p"));check(StreamInfo.parse(s).height==2160,"evicted entries are simply parsed again");
  check(StreamInfo.minutes("2h 14m")==134&&StreamInfo.minutes("47")==47,"runtime parse unchanged");

  check(PosterSample.trimDue(0,0)&&!PosterSample.trimDue(10,PosterSample.TRIM_GAP_MS)&&PosterSample.trimDue(PosterSample.TRIM_GAP_MS,PosterSample.TRIM_GAP_MS),"disk cache trim runs at first load, then at most once per gap");

  // 量一下：不作为通过条件（机器忙的时候会抖），只打印
  String[] names={"video","vw","top","nope","state"};int rounds=200_000;Object sink=null;
  for(int w=0;w<3;w++){for(int r=0;r<20_000;r++)for(String n:names){sink=oldField(leaf,n);sink=field(leaf,n);}}
  long t0=System.nanoTime();for(int r=0;r<rounds;r++)for(String n:names)sink=oldField(leaf,n);long oldNs=System.nanoTime()-t0;
  t0=System.nanoTime();for(int r=0;r<rounds;r++)for(String n:names)sink=field(leaf,n);long newNs=System.nanoTime()-t0;
  String u=StreamInfo.text(s).toUpperCase(java.util.Locale.ROOT);String[] res={"2160P|\\b4K\\b|\\bUHD\\b","HDR10\\+|HDR10PLUS","\\bAV1\\b","HEVC|X265|H\\.?265","DTS-?HD|DTS:?X","DDP|E-?AC-?3|DD\\+"};boolean b=false;
  for(int r=0;r<20_000;r++)for(String re:res){b^=oldHas(u,re);b^=StreamInfo.has(u,re);}
  t0=System.nanoTime();for(int r=0;r<100_000;r++)for(String re:res)b^=oldHas(u,re);long oldRx=System.nanoTime()-t0;
  t0=System.nanoTime();for(int r=0;r<100_000;r++)for(String re:res)b^=StreamInfo.has(u,re);long newRx=System.nanoTime()-t0;
  for(int r=0;r<5_000;r++)sink=StreamInfo.fromText(StreamInfo.text(s),StreamInfo.releaseText(s));
  t0=System.nanoTime();for(int r=0;r<20_000;r++)sink=StreamInfo.fromText(StreamInfo.text(s),StreamInfo.releaseText(s));long fullNs=(System.nanoTime()-t0)/20_000;
  t0=System.nanoTime();for(int r=0;r<20_000;r++)sink=StreamInfo.parse(s);long parseNs=(System.nanoTime()-t0)/20_000;
  System.out.println("PASS: reflection and regex caches match the uncached lookups; field lookup "+oldNs/(rounds*5)+"ns -> "+newNs/(rounds*5)+"ns, regex test "+oldRx/600_000+"ns -> "+newRx/600_000+"ns, repeated StreamInfo.parse "+fullNs+"ns -> "+parseNs+"ns"+(sink==null&&b?"":""));
 }
}
