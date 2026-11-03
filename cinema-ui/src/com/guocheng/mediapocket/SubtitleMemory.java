package com.guocheng.mediapocket;
import android.app.Activity;
import android.content.SharedPreferences;
/** 一部片子自己的字幕选择。关掉之后，下次打开同一条不再自动打开。 */
final class SubtitleMemory {
 private SubtitleMemory(){}
 static String id(Activity a){if(a==null)return "";Object v=OnlineSubtitleSearch.field(a,"id");if(v==null)return "";String s=v.toString();return s.isEmpty()||"null".equals(s)?"":s;}
 static SharedPreferences prefs(Activity a){return a.getSharedPreferences("data",0);}
 static void off(Activity a){String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,true).remove("subtitleSpu:"+id).remove("subtitleName:"+id).remove("subtitleUrl:"+id).apply();try{SourceSubtitleLoader.stopAuto(a);}catch(Exception ignored){}}
 static void embedded(Activity a,int spu,String name){String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,false).putInt("subtitleSpu:"+id,spu).putString("subtitleName:"+id,name==null?"":name).remove("subtitleUrl:"+id).apply();try{SourceSubtitleLoader.stopAuto(a);}catch(Exception ignored){}}
 static void named(Activity a,String name){String id=id(a);if(id.isEmpty())return;prefs(a).edit().putBoolean("subtitleDisabled:"+id,false).remove("subtitleSpu:"+id).putString("subtitleName:"+id,name==null?"":name).remove("subtitleUrl:"+id).apply();try{SourceSubtitleLoader.stopAuto(a);}catch(Exception ignored){}}
}
