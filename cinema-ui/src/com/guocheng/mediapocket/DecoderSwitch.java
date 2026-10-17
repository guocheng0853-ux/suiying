package com.guocheng.mediapocket;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.View;
import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
/**
 * 按档位重开当前 LibVLC 媒体。底座 VlcPlayback.configure 永远调用
 * setHWDecoderEnabled(!software, false)，HW+ 和 HW 因此没有区别，也不能打开直接渲染。
 * 这里自己建 Media：HW+ (true, true)，HW (true, false) 再加 :no-mediacodec-dr，SW (false, false)。
 * 进度、倍速、音轨和字幕轨在重开后写回去。
 */
final class DecoderSwitch {
 private static int token;
 private DecoderSwitch(){}
 /** 成功返回 null，否则返回能直接给用户看的原因。 */
 static String reopen(android.app.Activity a,String tier){
  String reason=once(a,tier);
  if(reason==null||!reason.startsWith("当前不是"))return reason;
  try{OnlineSubtitleSearch.call(a,"retryCurrentDecoder",new Class<?>[0]);}
  catch(Exception e){return "底座重开失败："+text(e);}
  return once(a,tier);
 }
 static String once(android.app.Activity a,String tier){
  Object video=OnlineSubtitleSearch.field(a,"video");
  if(video==null)return "播放器还没准备好";
  Object player=OnlineSubtitleSearch.field(video,"player"),lib=OnlineSubtitleSearch.field(video,"lib");
  if(player==null||lib==null)return "当前不是 LibVLC 播放器";
  int pos=number(callQuiet(video,"getCurrentPosition"));
  boolean wasPlaying=Boolean.TRUE.equals(callQuiet(video,"isPlaying"));
  int audio=number(callQuiet(player,"getAudioTrack")),spu=number(callQuiet(player,"getSpuTrack"));
  float rate=PlaybackPrefs.speed(a.getSharedPreferences("data",0));
  Object rawRate=callQuiet(player,"getRate");if(rawRate instanceof Float&&(Float)rawRate>0f)rate=(Float)rawRate;
  String url=string(OnlineSubtitleSearch.field(video,"sourceUrl"));
  if(url.isEmpty())url=string(OnlineSubtitleSearch.field(a,"url"));
  Object fd=OnlineSubtitleSearch.field(video,"mediaFd");
  boolean file=url.isEmpty()&&fd instanceof ParcelFileDescriptor;
  if(url.isEmpty()&&!file)return "源尚未连接，解码会在下次播放生效";
  @SuppressWarnings("unchecked") Map<String,String> headers=OnlineSubtitleSearch.field(video,"sourceHeaders") instanceof Map?(Map<String,String>)OnlineSubtitleSearch.field(video,"sourceHeaders"):null;
  try{
   set(video,"software",Boolean.valueOf("software".equals(tier)));
   set(video,"decodeMode","software".equals(tier)?"software":"hardware");
   set(video,"retried",Boolean.TRUE);
   Object media=newMedia(lib,url,file?fd:null);
   media.getClass().getMethod("setHWDecoderEnabled",boolean.class,boolean.class).invoke(media,PlayerOverlayRules.hwEnabled(tier),PlayerOverlayRules.directRendering(tier));
   Method add=media.getClass().getMethod("addOption",String.class);
   for(String option:PlayerOverlayRules.decodeOptions(tier,file,headers))add.invoke(media,option);
   invoke(player,"stop");
   Method setMedia=find(player.getClass(),"setMedia",1);if(setMedia==null)return "LibVLC 没有 setMedia";
   setMedia.invoke(player,media);
   try{media.getClass().getMethod("release").invoke(media);}catch(Exception ignored){}
   invoke(player,"play");
   restore(player,video,pos,audio,spu,rate,wasPlaying);
   final int mine=++token;final Object p=player,v=video;final int position=pos,audioId=audio,spuId=spu;final float speed=rate;final boolean playing=wasPlaying;
   if(video instanceof View)((View)video).postDelayed(()->{if(mine==token)restore(p,v,position,audioId,spuId,speed,playing);},400);
   return null;
  }catch(Exception e){return text(e);}
 }
 static void restore(Object player,Object video,int pos,int audio,int spu,float rate,boolean wasPlaying){
  if(pos>0){callQuiet(video,"seekTo",new Class<?>[]{int.class},pos);callQuiet(player,"setTime",new Class<?>[]{long.class},Long.valueOf(pos));}
  if(rate>0f){callQuiet(player,"setRate",new Class<?>[]{float.class},rate);callQuiet(video,"speed",new Class<?>[]{float.class},rate);}
  if(audio>=0)callQuiet(player,"setAudioTrack",new Class<?>[]{int.class},audio);
  if(spu>=0)callQuiet(player,"setSpuTrack",new Class<?>[]{int.class},spu);
  if(!wasPlaying)callQuiet(player,"pause");
 }
 static Object newMedia(Object lib,String url,Object fd)throws Exception{
  Class<?> media=Class.forName("org.videolan.libvlc.Media");
  if(fd instanceof ParcelFileDescriptor){FileDescriptor raw=((ParcelFileDescriptor)fd).getFileDescriptor();for(Constructor<?> c:media.getConstructors()){Class<?>[] p=c.getParameterTypes();if(p.length==2&&p[1]==FileDescriptor.class)return c.newInstance(lib,raw);}}
  Uri uri=Uri.parse(url);for(Constructor<?> c:media.getConstructors()){Class<?>[] p=c.getParameterTypes();if(p.length==2&&p[1]==Uri.class)return c.newInstance(lib,uri);}
  throw new NoSuchMethodException("Media");
 }
 static Method find(Class<?> type,String name,int args){for(Class<?> c=type;c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args){m.setAccessible(true);return m;}return null;}
 static void invoke(Object target,String name)throws Exception{Method m=find(target.getClass(),name,0);if(m!=null)m.invoke(target);}
 static Object callQuiet(Object target,String name){return callQuiet(target,name,new Class<?>[0]);}
 static Object callQuiet(Object target,String name,Class<?>[] types,Object... args){try{return target==null?null:OnlineSubtitleSearch.call(target,name,types,args);}catch(Exception e){return null;}}
 static void set(Object target,String name,Object value){for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{java.lang.reflect.Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(target,value);return;}catch(Exception ignored){}}
 static int number(Object v){return v instanceof Integer?(Integer)v:v instanceof Long?((Long)v).intValue():-1;}
 static String string(Object v){return v==null?"":v.toString();}
 static String text(Throwable e){Throwable t=e instanceof InvocationTargetException&&e.getCause()!=null?e.getCause():e;String m=t.getMessage();return t.getClass().getSimpleName()+(m==null||m.isEmpty()?"":"："+m);}
}
