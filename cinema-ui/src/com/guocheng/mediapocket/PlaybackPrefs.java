package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.os.*;import java.util.*;
/** 详情页设置的音轨/字幕偏好；开播后自动匹配音轨（字幕偏好写入原有 subtitleLanguage，由原自动字幕流程使用）。 */
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
 public static boolean skipSavedSubtitle(Object session){Object owner=OnlineSubtitleSearch.field(session,"a");return owner instanceof Activity&&subtitlesOff(((Activity)owner).getSharedPreferences("data",0));}
 static void attach(Activity a){String pref=audio(a.getSharedPreferences("data",0));if(pref.equals("auto"))return;Handler h=new Handler(Looper.getMainLooper());Object first=OnlineSubtitleSearch.field(a,"video");h.postDelayed(new Runnable(){int tries;public void run(){if(a.isFinishing()||tries++>40)return;try{Object video=OnlineSubtitleSearch.field(a,"video");if(video!=null&&video!=first)return;Object t=video==null?null:OnlineSubtitleSearch.call(video,"audioTracks",new Class<?>[0]);if(t instanceof Object[]&&((Object[])t).length>1){Object[] tracks=(Object[])t;String[] names=new String[tracks.length];int[] ids=new int[tracks.length];for(int i=0;i<tracks.length;i++){Object id=OnlineSubtitleSearch.field(tracks[i],"id"),name=OnlineSubtitleSearch.field(tracks[i],"name");ids[i]=id instanceof Integer?(Integer)id:-1;names[i]=name==null?"":name.toString();}Object cur=null;try{Object player=OnlineSubtitleSearch.field(video,"player");cur=player==null?null:OnlineSubtitleSearch.call(player,"getAudioTrack",new Class<?>[0]);}catch(Exception ignored){}int target=pick(names,ids,cur instanceof Integer?(Integer)cur:-2,pref);if(target>=0){OnlineSubtitleSearch.call(video,"audio",new Class<?>[]{int.class},target);try{OnlineSubtitleSearch.call(a,"showHud",new Class<?>[]{String.class},"已按偏好切换音轨");}catch(Exception ignored){}}return;}}catch(Exception e){return;}h.postDelayed(this,1000);}},1500);}
}
