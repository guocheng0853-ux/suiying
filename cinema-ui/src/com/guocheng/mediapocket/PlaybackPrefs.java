package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.os.*;import java.util.*;
/** 详情页设置的音轨/字幕偏好。音轨在开播后匹配；字幕偏好交给 TrackSelection（简体优先于繁体，关闭则不自动加载）。 */
public final class PlaybackPrefs {
 static final String[] AUDIO={"auto","zh","yue","en"},AUDIO_LABELS={"自动（片源默认）","国语 / 普通话","粤语","英语"};
 static final String[] SUBS={"zho","eng","off"},SUB_LABELS={"中文优先","英文优先","关闭（不自动加载）"};
 static String audio(SharedPreferences p){return p.getString("pref.audio","auto");}
 static String subtitle(SharedPreferences p){return p.getBoolean("pref.subtitleOff",false)?"off":p.getString("subtitleLanguage","zho").equals("eng")?"eng":"zho";}
 static void setSubtitle(SharedPreferences p,String v){SharedPreferences.Editor e=p.edit().putBoolean("pref.subtitleOff",v.equals("off"));if(!v.equals("off"))e.putString("subtitleLanguage",v);e.apply();}
 static boolean subtitlesOff(SharedPreferences p){return p.getBoolean("pref.subtitleOff",false);}
 static int index(String[] values,String v){for(int i=0;i<values.length;i++)if(values[i].equals(v))return i;return 0;}
 /** 纯规则：按偏好挑选音轨 id；无偏好、无匹配或已在匹配轨上时返回 -1。 */
 static int pick(String[] names,int[] ids,int current,String pref){if(pref.equals("auto")||names==null||ids==null)return -1;for(int i=0;i<Math.min(names.length,ids.length);i++){if(ids[i]<0)continue;String n=names[i]==null?"":names[i].toLowerCase(Locale.ROOT);if(n.contains("commentary")||n.contains("评论"))continue;if(matches(n,pref))return ids[i]==current?-1:ids[i];}return -1;}
 static boolean matches(String n,String pref){switch(pref){case "zh":return !matches(n,"yue")&&(n.contains("国语")||n.contains("普通话")||n.contains("mandarin")||n.contains("chinese")||n.contains("中文")||n.contains("华语")||has(n,"chi")||has(n,"zho")||has(n,"cmn")||has(n,"zh"));case "yue":return n.contains("粤")||n.contains("cantonese")||has(n,"yue");case "en":return n.contains("english")||n.contains("英")||has(n,"eng")||has(n,"en");default:return false;}}
 static boolean has(String n,String code){return java.util.regex.Pattern.compile("(^|[^a-z])"+code+"([^a-z]|$)").matcher(n).find();}
 /** 由 PlayerSession.start 钩子调用：每秒检查一次，最多 40 秒，拿到音轨后按偏好切换一次。 */
 public static boolean skipSavedSubtitle(Object session){Object owner=OnlineSubtitleSearch.field(session,"a");return owner instanceof Activity&&(subtitlesOff(((Activity)owner).getSharedPreferences("data",0))||TrackSelection.hasSubtitle(TrackSelection.stream((Activity)owner)));}
 static void attach(Activity a){String pref=audio(a.getSharedPreferences("data",0));if(pref.equals("auto")||TrackSelection.hasAudio(TrackSelection.stream(a)))return;Handler h=new Handler(Looper.getMainLooper());Object first=OnlineSubtitleSearch.field(a,"video");h.postDelayed(new Runnable(){int tries;public void run(){if(a.isFinishing()||tries++>40)return;try{Object video=OnlineSubtitleSearch.field(a,"video");if(video!=null&&video!=first)return;Object t=video==null?null:OnlineSubtitleSearch.call(video,"audioTracks",new Class<?>[0]);if(t instanceof Object[]&&((Object[])t).length>1){Object[] tracks=(Object[])t;String[] names=new String[tracks.length];int[] ids=new int[tracks.length];for(int i=0;i<tracks.length;i++){Object id=OnlineSubtitleSearch.field(tracks[i],"id"),name=OnlineSubtitleSearch.field(tracks[i],"name");ids[i]=id instanceof Integer?(Integer)id:-1;names[i]=name==null?"":name.toString();}Object cur=null;try{Object player=OnlineSubtitleSearch.field(video,"player");cur=player==null?null:OnlineSubtitleSearch.call(player,"getAudioTrack",new Class<?>[0]);}catch(Exception ignored){}int target=pick(names,ids,cur instanceof Integer?(Integer)cur:-2,pref);if(target>=0){OnlineSubtitleSearch.call(video,"audio",new Class<?>[]{int.class},target);try{OnlineSubtitleSearch.call(a,"showHud",new Class<?>[]{String.class},"已按偏好切换音轨");}catch(Exception ignored){}}return;}}catch(Exception e){return;}h.postDelayed(this,1000);}},1500);}
 /** 倍速：与原播放器速度菜单共用 playbackSpeed 键（开播时由 PlayerSession 自动套用）。 */
 static final float[] SPEEDS={0.5f,0.75f,1f,1.25f,1.5f,2f};
 static float normalizeSpeed(float v){float best=1f,gap=Float.MAX_VALUE;if(Float.isNaN(v)||v<=0)return 1f;for(float s:SPEEDS){float d=Math.abs(s-v);if(d<gap){gap=d;best=s;}}return best;}
 static float nextSpeed(float v){float n=normalizeSpeed(v);for(int i=0;i<SPEEDS.length;i++)if(SPEEDS[i]==n)return SPEEDS[(i+1)%SPEEDS.length];return 1f;}
 /** 右侧竖条：最上最快、最下最慢。y 从条顶算起，落在哪一格就是哪一档。无效坐标返回 -1。 */
 static int indexFromTrack(float y,float height){int n=SPEEDS.length;if(n==0||!(height>0f)||Float.isNaN(y)||Float.isInfinite(y))return -1;if(y<0f)return n-1;if(y>=height)return 0;int fromTop=(int)(y/height*n);if(fromTop<0)fromTop=0;if(fromTop>=n)fromTop=n-1;return n-1-fromTop;}
 static float speedFromTrack(float y,float height){int i=indexFromTrack(y,height);return i<0?Float.NaN:SPEEDS[i];}
 static String speedLabel(float v){float n=normalizeSpeed(v);return (n==(int)n?String.valueOf((int)n):String.valueOf(n))+"×";}
 /** 记住的倍速按 0.1–8.0 原样保存，不再吸回旧的六档。非法值回到 1。 */
 static float clampRate(float v){if(Float.isNaN(v)||Float.isInfinite(v))return 1f;if(v<0.1f)v=0.1f;if(v>8f)v=8f;return Math.round(v*100f)/100f;}
 static float speed(SharedPreferences p){return clampRate(p.getFloat("playbackSpeed",1f));}
 static void setSpeed(SharedPreferences p,float v){p.edit().putFloat("playbackSpeed",clampRate(v)).apply();}
 /** 长按加速的基准倍率。默认 2，可选 1.5 / 2 / 2.5 / 3，不写入 playbackSpeed。 */
 static final float[] HOLD_RATES={1.5f,2f,2.5f,3f};
 static float holdRate(SharedPreferences p){return clampRate(p.getFloat("pref.holdRate",2f));}
 static void setHoldRate(SharedPreferences p,float v){p.edit().putFloat("pref.holdRate",clampRate(v)).apply();}
 static String holdLabel(float v){return String.format(java.util.Locale.ROOT,"长按加速：%.1f×",clampRate(v));}
 /** 双击中间播放/暂停。默认关，左右快进快退不受这个开关影响。 */
 static boolean doubleTapCenter(SharedPreferences p){return p.getBoolean("pref.doubleTapCenter",false);}
 static boolean statusTime(SharedPreferences p){return p.getBoolean("pref.statusTime",true);}
 static boolean statusNet(SharedPreferences p){return p.getBoolean("pref.statusNet",true);}
 static boolean statusBattery(SharedPreferences p){return p.getBoolean("pref.statusBattery",true);}
 static boolean autoNext(SharedPreferences p){return p.getBoolean("pref.autoNext",true);}
 static int autoNextLead(SharedPreferences p){return AutoNextRules.lead(p.getInt("pref.autoNextLead",AutoNextRules.DEFAULT_LEAD));}
 static boolean rememberAspect(SharedPreferences p){return p.getBoolean("pref.rememberAspect",true);}
 static boolean rememberBrightness(SharedPreferences p){return p.getBoolean("pref.rememberBrightness",false);}
 static boolean forceHighRefresh(SharedPreferences p){return p.getBoolean("pref.forceHighRefresh",false);}
 static boolean showBufferSpeed(SharedPreferences p){return p.getBoolean("pref.showBufferSpeed",false);}
 static int seekSeconds(SharedPreferences p){return PlayerSwitchRules.seekSeconds(p.getInt("pref.seekSeconds",15));}
 static int seekMs(SharedPreferences p){return PlayerSwitchRules.seekMs(seekSeconds(p));}
}
