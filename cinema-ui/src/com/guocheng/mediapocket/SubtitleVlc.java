package com.guocheng.mediapocket;
import android.content.Context;import android.content.SharedPreferences;import java.util.*;
/**
 * 0.1.169：底座 VLC 的两个入口由 PatchSubtitleVlc 改成调这里——
 * 建 LibVLC 时追加全局字幕样式；每个片源的 Media 在原来的 addOption 之后追加同一套媒体选项（重开片源时用最新设置）。
 */
public final class SubtitleVlc {
 private SubtitleVlc(){}
 static volatile Context app;
 static SubtitlePrefs.Values values(){Context c=app;if(c==null)return SubtitlePrefs.defaults();try{SharedPreferences p=c.getSharedPreferences("data",0);return SubtitleStyle.values(p);}catch(RuntimeException e){return SubtitlePrefs.defaults();}}
 /** 替换 PlayerActivity 里 Arrays.asList("--aout=…","--android-display-chroma=…")。 */
 public static List<Object> libOptions(Object[] base){ArrayList<Object> out=new ArrayList<>(Arrays.asList(base));try{List<String> extra=SubtitleVlcRules.global(values());if(accepted(base,extra))out.addAll(extra);}catch(RuntimeException ignored){}return out;}
 /**
  * 有一个选项这个 libVLC 不认，LibVLC 就建不起来，整个 VLC 播放器打不开。每个进程第一次用之前，先用同一组选项试建一次并马上释放；
  * 失败就不再加样式选项（字幕照常，只是不跟设置），并记一行日志。
  */
 static volatile Boolean probed;
 static boolean accepted(Object[] base,List<String> extra){Boolean known=probed;if(known!=null)return known;Context c=app;if(c==null)return false;boolean ok=false;Object lib=null;
  try{ArrayList<Object> all=new ArrayList<>(Arrays.asList(base));all.addAll(extra);lib=Class.forName("org.videolan.libvlc.LibVLC").getConstructor(Context.class,List.class).newInstance(c,all);ok=true;}
  catch(Throwable t){DebugLog.event("字幕","这个 VLC 不接受字幕样式选项，内嵌字幕保持默认样式");}
  finally{if(lib!=null)try{lib.getClass().getMethod("release").invoke(lib);}catch(Throwable ignored){}}
  probed=ok;return ok;}
 /** 替换 VlcPlayback 里 media.addOption(":audio-language=eng,en")。 */
 public static void addOption(org.videolan.libvlc.Media media,String option){media.addOption(option);if(Boolean.FALSE.equals(probed))return;try{for(String o:SubtitleVlcRules.media(values()))media.addOption(o);}catch(RuntimeException ignored){}}
 static List<String> mediaOptions(){if(Boolean.FALSE.equals(probed))return Collections.emptyList();try{return SubtitleVlcRules.media(values());}catch(RuntimeException e){return Collections.emptyList();}}
}
