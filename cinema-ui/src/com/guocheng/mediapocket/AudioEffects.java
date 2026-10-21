package com.guocheng.mediapocket;
import android.app.Activity;import android.content.SharedPreferences;import java.lang.reflect.*;import java.util.*;
/**
 * 0.1.110 播放页「更多 → 音效」：对白增强、夜间模式、VLC 均衡器预设。选一次一直记住，每次开播自动套用。
 * 通过反射调用原版自带 libVLC 的 MediaPlayer.Equalizer（setEqualizer），不改原版播放器。
 */
final class AudioEffects {
 private AudioEffects(){}
 static final String KEY="audio.effect";
 static final Map<Object,String> APPLIED=new WeakHashMap<>();
 static SharedPreferences prefs(Activity a){return a.getSharedPreferences("data",0);}
 static String saved(Activity a){return AudioEffectRules.normalize(prefs(a).getString(KEY,AudioEffectRules.OFF));}

 static Object player(Activity a){Object video=OnlineSubtitleSearch.field(a,"video");return video==null?null:OnlineSubtitleSearch.field(video,"player");}
 static Class<?> eq()throws ClassNotFoundException{return Class.forName("org.videolan.libvlc.MediaPlayer$Equalizer");}

 /** 播放页轮询时调用：换片 / 换源后播放器对象变了就重新套用一次。 */
 static void tick(Activity a){try{Object p=player(a);if(p==null)return;String want=saved(a);if(want.equals(APPLIED.get(p)))return;if(apply(p,want))APPLIED.put(p,want);}catch(Throwable ignored){}}

 static boolean apply(Object player,String saved){try{Object e=null;String s=AudioEffectRules.normalize(saved);Class<?> c=eq();
   if(s.startsWith(AudioEffectRules.PRESET)){e=c.getMethod("createFromPreset",int.class).invoke(null,AudioEffectRules.presetIndex(s));}
   else if(!s.equals(AudioEffectRules.OFF)){int bands=(Integer)c.getMethod("getBandCount").invoke(null);float[] curve=AudioEffectRules.curve(s,bands);if(curve==null)return false;e=c.getMethod("create").invoke(null);c.getMethod("setPreAmp",float.class).invoke(e,curve[0]);Method amp=c.getMethod("setAmp",int.class,float.class);for(int i=1;i<curve.length;i++)amp.invoke(e,i-1,curve[i]);}
   Object r=player.getClass().getMethod("setEqualizer",c).invoke(player,e);return !Boolean.FALSE.equals(r);}catch(Throwable t){return false;}}

 static String presetName(int i){try{return (String)eq().getMethod("getPresetName",int.class).invoke(null,i);}catch(Throwable t){return "预设 "+(i+1);}}
 static int presetCount(){try{return (Integer)eq().getMethod("getPresetCount").invoke(null);}catch(Throwable t){return 0;}}
 static String label(Activity a){String s=saved(a);int i=AudioEffectRules.presetIndex(s);return "音效："+AudioEffectRules.label(s,i>=0?presetName(i):"");}

 static void menu(Activity a){String cur=saved(a);List<CinemaMenu.Item> rows=new ArrayList<>();
  rows.add(CinemaMenu.choice("关闭",cur.equals(AudioEffectRules.OFF),()->choose(a,AudioEffectRules.OFF)));
  rows.add(CinemaMenu.choice("对白增强（压低轰鸣，人声更清楚）",cur.equals(AudioEffectRules.DIALOG),()->choose(a,AudioEffectRules.DIALOG)));
  rows.add(CinemaMenu.choice("夜间模式（大幅压低爆炸和重低音）",cur.equals(AudioEffectRules.NIGHT),()->choose(a,AudioEffectRules.NIGHT)));
  int n=presetCount();for(int i=0;i<n;i++){final int at=i;String v=AudioEffectRules.PRESET+i;String name=AudioEffectRules.chinese(presetName(i));if(name.equals("平直"))continue;rows.add(CinemaMenu.choice("均衡器 · "+name,cur.equals(v),()->choose(a,v)));}
  CinemaMenu.show(a,"音效",rows);}
 static void choose(Activity a,String value){prefs(a).edit().putString(KEY,value).apply();Object p=player(a);boolean ok=p!=null&&apply(p,value);if(p!=null&&ok)APPLIED.put(p,value);
  int i=AudioEffectRules.presetIndex(value);FullscreenMode.hud(a,ok||p==null?"音效 · "+AudioEffectRules.label(value,i>=0?presetName(i):""):"这个片源暂时无法套用音效");}
}
