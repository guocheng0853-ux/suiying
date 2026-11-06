package com.guocheng.mediapocket;
import android.app.Activity;
import android.content.SharedPreferences;
import java.util.WeakHashMap;
/** 一部片子自己的字幕选择。关掉之后，下次打开同一条不再自动打开。本次播放里的手动操作记在 HOLD，轮询不再接管。 */
final class SubtitleMemory {
 private SubtitleMemory(){}
 static final WeakHashMap<Activity,Boolean> HOLD=new WeakHashMap<>();
 static String id(Activity a){if(a==null)return "";Object v=OnlineSubtitleSearch.field(a,"id");if(v==null)return "";String s=v.toString();return s.isEmpty()||"null".equals(s)?"":s;}
 static SharedPreferences prefs(Activity a){return a.getSharedPreferences("data",0);}
 static void hold(Activity a){if(a!=null)HOLD.put(a,Boolean.TRUE);}
 static boolean held(Activity a){return a!=null&&Boolean.TRUE.equals(HOLD.get(a));}
 static void release(Activity a){if(a!=null)HOLD.remove(a);}
 static void off(Activity a){hold(a);String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,true).remove("subtitleSpu:"+id).remove("subtitleName:"+id).remove("subtitleUrl:"+id).apply();try{SourceSubtitleLoader.stopAuto(a);}catch(Exception ignored){}}
 static void embedded(Activity a,int spu,String name){hold(a);String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,false).putInt("subtitleSpu:"+id,spu).putString("subtitleName:"+id,name==null?"":name).remove("subtitleUrl:"+id).apply();try{SourceSubtitleLoader.stopAuto(a);}catch(Exception ignored){}}
 static void named(Activity a,String name){hold(a);String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,false).remove("subtitleSpu:"+id).putString("subtitleName:"+id,name==null?"":name).remove("subtitleUrl:"+id).apply();try{SourceSubtitleLoader.stopAuto(a);}catch(Exception ignored){}}
 /** 用户选了没存成文件的外挂。清掉本集内嵌记忆和关闭标记，避免下一拍把正在加载的外挂停掉。 */
 static void external(Activity a){hold(a);String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,false).remove("subtitleSpu:"+id).remove("subtitleName:"+id).apply();}
}
