package com.guocheng.mediapocket;
import android.app.Activity;import android.content.SharedPreferences;import java.util.*;
/**
 * 0.1.111 杜比视界片源怎么播：按「我的 → 播放 → 杜比视界片源」的设置，或开播前问一次（可记住）。
 * 系统解码：安卓自带解码器，手机支持杜比视界时颜色正确，功能精简；服务器转码：转成普通画面，要服务器支持色调映射；VLC：功能全，但 P5 片源会发紫发绿。
 */
final class DolbyChoice {
 private DolbyChoice(){}
 static final String KEY="dovi.mode";
 static SharedPreferences prefs(Activity a){return a.getSharedPreferences("data",0);}
 static void decide(Activity a,String kind,Runnable system,Runnable transcode,Runnable vlc){
  String mode=DolbyRules.mode(prefs(a).getString(KEY,"ask"));
  if(mode.equals("system")){FullscreenMode.hud(a,"杜比视界片源 · 系统解码");system.run();return;}
  if(mode.equals("transcode")){transcode.run();return;}
  if(mode.equals("vlc")){vlc.run();return;}
  String what=DolbyRules.P5.equals(kind)?"这是杜比视界 Profile 5 片源。":"这个片源标着杜比视界。";
  List<CinemaMenu.Item> rows=new ArrayList<>();
  rows.add(CinemaMenu.item("系统解码播放（推荐，颜色正确）",system));
  rows.add(CinemaMenu.item("以后杜比视界都用系统解码",()->{prefs(a).edit().putString(KEY,"system").apply();system.run();}));
  rows.add(CinemaMenu.item("服务器转码（需服务器支持色调映射）",transcode));
  rows.add(CinemaMenu.item("仍用 VLC（颜色可能发紫发绿）",vlc));
  android.widget.TextView body=new android.widget.TextView(a);body.setText(what+"VLC 播放会整体发紫发绿；系统解码颜色正确，但没有弹幕、外挂字幕和倍速。");body.setTextColor(PlayerTheme.TEXT_SECONDARY);body.setTextSize(14);
  if(!CinemaMenu.open(a,"杜比视界片源",body,rows,null))system.run();
 }
 static String label(Activity a){String m=DolbyRules.mode(prefs(a).getString(KEY,"ask"));return "杜比视界片源："+(m.equals("system")?"系统解码":m.equals("transcode")?"服务器转码":m.equals("vlc")?"VLC":"每次询问");}
 static void settings(Activity a,Runnable done){String[] labels={"每次询问","系统解码（颜色正确，功能精简）","服务器转码","VLC（P5 片源会发紫发绿）"},values={"ask","system","transcode","vlc"};String cur=DolbyRules.mode(prefs(a).getString(KEY,"ask"));List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<values.length;i++){final String v=values[i];rows.add(CinemaMenu.choice(labels[i],v.equals(cur),()->{prefs(a).edit().putString(KEY,v).apply();if(done!=null)done.run();}));}CinemaMenu.show(a,"杜比视界片源",rows);}
}
