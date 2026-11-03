package com.guocheng.mediapocket;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
/** 字幕设置页。从「我的」进完整页面，从播放页进同一份内容的窗口，改完马上作用到正在播的字。 */
final class SubtitleSettings {
 private SubtitleSettings(){}
 static boolean dirty;
 static boolean consumeDirty(){boolean v=dirty;dirty=false;return v;}
 static String entry(Activity a){return SubtitleStyle.label(a);}
 static void open(Activity a){if(a instanceof ScrapedMainActivity)a.startActivity(new Intent(a,SubtitleSettingsActivity.class));else show(a);}
 static void show(Activity a){boolean light=CinemaTheme.light(a);final Dialog[] box=new Dialog[1];box[0]=new Dialog(a,light?android.R.style.Theme_Material_Light_NoActionBar:android.R.style.Theme_Material_NoActionBar);box[0].setContentView(shell(a,()->box[0].dismiss()));box[0].show();}
 static View shell(Activity a,Runnable close){
  boolean light=CinemaTheme.light(a),gold=!light&&CinemaTheme.gold(a),clay=light&&CinemaTheme.clay(a);
  int ink=light?(clay?CinemaTheme.SLATE:0xff173740):gold?CinemaTheme.INK:0xfff5fafb,muted=light?(clay?CinemaTheme.STONE:0xff52757c):gold?CinemaTheme.MUTED:0xffafcbd2,surface=light?(clay?CinemaTheme.PAPER:0xffedf5f5):gold?CinemaTheme.BG:0xff10343e;
  LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(surface);int pad=dp(a,16);root.setPadding(pad,dp(a,8),pad,0);
  LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);Button back=new Button(a);back.setText("‹ 返回");back.setAllCaps(false);back.setTextColor(ink);back.setBackgroundColor(light?(clay?CinemaTheme.SHEET:0xffffffff):gold?CinemaTheme.CARD:0xff19434e);back.setOnClickListener(v->close.run());header.addView(back);TextView title=text(a,"字幕设置",20,ink);title.setTypeface(CinemaTheme.paperTitle(a));title.setPadding(dp(a,12),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
  ScrollView scroll=new ScrollView(a);LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);list.setTag("subtitle-settings");scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));fill(a,list,ink,muted);return root;
 }
 static void fill(Activity a,LinearLayout list,int ink,int muted){
  list.removeAllViews();SharedPreferences p=a.getSharedPreferences("data",0);SubtitlePrefs.Values v=SubtitleStyle.values(p);
  note(a,list,"没手动加字幕、也没记住选择时，开播会先打开偏好语言的内嵌字幕。关掉某一集之后，这一集不会再被自动打开。",muted);
  row(a,list,"偏好语言",SubtitlePrefs.languageLabel(v.language),ink,muted,()->choose(a,list,"偏好语言",SubtitlePrefs.LANGS,SubtitlePrefs.LANG_LABELS,v.language,value->PlaybackPrefs.setSubtitle(p,value)));
  row(a,list,"字号",SubtitlePrefs.sizeLabel(v.size),ink,muted,()->choose(a,list,"字号",SubtitlePrefs.SIZES,SubtitlePrefs.SIZE_LABELS,v.size,value->p.edit().putString("sub.size",value).apply()));
  row(a,list,"颜色",SubtitlePrefs.colorLabel(v.color),ink,muted,()->choose(a,list,"字体颜色",SubtitlePrefs.COLORS,SubtitlePrefs.COLOR_LABELS,v.color,value->p.edit().putString("sub.color",value).apply()));
  row(a,list,"粗体",v.bold?"开":"关",ink,muted,()->{p.edit().putBoolean("sub.bold",!v.bold).apply();refresh(a,list);});
  row(a,list,"样式",SubtitlePrefs.lookLabel(v.look),ink,muted,()->choose(a,list,"样式",SubtitlePrefs.LOOKS,SubtitlePrefs.LOOK_LABELS,v.look,value->p.edit().putString("sub.look",value).apply()));
  row(a,list,"第一字幕位置",SubtitlePrefs.liftLabel(v.lift),ink,muted,()->choose(a,list,"第一字幕位置",SubtitlePrefs.LIFTS,SubtitlePrefs.LIFT_LABELS,v.lift,value->p.edit().putString("sub.lift",value).apply()));
  row(a,list,"第二字幕字号",SubtitlePrefs.sizeLabel(v.secondarySize),ink,muted,()->choose(a,list,"第二字幕字号",SubtitlePrefs.SIZES,SubtitlePrefs.SIZE_LABELS,v.secondarySize,value->p.edit().putString("sub.secondarySize",value).apply()));
  row(a,list,"第二字幕位置",SubtitlePrefs.liftLabel(v.secondaryLift),ink,muted,()->choose(a,list,"第二字幕位置",SubtitlePrefs.LIFTS,SubtitlePrefs.LIFT_LABELS,v.secondaryLift,value->p.edit().putString("sub.secondaryLift",value).apply()));
  note(a,list,"第二字幕在播放页的字幕面板里选外挂文本。VLC 和内置播放器一次只能解一条内嵌字幕，两条内嵌不能叠在一起。外挂的 SRT、ASS、VTT 可以和正在显示的第一字幕一起出现，字号和位置分开记。颜色、粗体和描边两边共用。",muted);
  int offset=p.getInt("sub.offsetMs",0);note(a,list,"字幕延迟  "+(offset>0?"+":"")+offset+" ms。正数让字幕更晚出现。内嵌字幕走播放器延迟；外挂文本按时间轴挪。内置播放器的内嵌字幕只能往后延，不能提前。",muted);
  LinearLayout sync=new LinearLayout(a);
  sync.addView(chip(a,"−100ms",()->shift(a,list,p,-1)));sync.addView(chip(a,"归零",()->{p.edit().putInt("sub.offsetMs",0).apply();refresh(a,list);}));sync.addView(chip(a,"+100ms",()->shift(a,list,p,1)));
  list.addView(sync);
  row(a,list,"HDR 片源字幕调暗",v.hdrDim?"开":"关",ink,muted,()->{p.edit().putBoolean("sub.hdrDim",!v.hdrDim).apply();refresh(a,list);});
 }
 static void shift(Activity a,LinearLayout list,SharedPreferences p,int direction){p.edit().putInt("sub.offsetMs",EmbyCue.stepSubtitleOffset(p.getInt("sub.offsetMs",0),direction)).apply();refresh(a,list);}
 static void choose(Activity a,LinearLayout list,String title,String[] values,String[] labels,String current,Saver save){List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<values.length;i++){final String value=values[i];rows.add(CinemaMenu.choice(labels[i],value.equals(current),()->{save.save(value);refresh(a,list);}));}CinemaMenu.show(a,title,rows);}
 interface Saver {void save(String value);}
 static void refresh(Activity a,LinearLayout list){boolean light=CinemaTheme.light(a),gold=!light&&CinemaTheme.gold(a),clay=light&&CinemaTheme.clay(a);int ink=light?(clay?CinemaTheme.SLATE:0xff173740):gold?CinemaTheme.INK:0xfff5fafb,muted=light?(clay?CinemaTheme.STONE:0xff52757c):gold?CinemaTheme.MUTED:0xffafcbd2;fill(a,list,ink,muted);nudge(a);}
 static void nudge(Activity a){dirty=true;try{Object video=OnlineSubtitleSearch.field(a,"video");Object player=video==null?null:OnlineSubtitleSearch.field(video,"player");TrackSelection.applyDelay(player,a.getSharedPreferences("data",0));}catch(Exception ignored){}try{SubtitleStyle.applyVlc(a);}catch(Exception ignored){}if(a instanceof SystemPlayerActivity){Media3Controls chrome=((SystemPlayerActivity)a).chrome;if(chrome!=null)chrome.restyle();}SecondarySubtitle.tick(a);}
 static void note(Activity a,LinearLayout list,String message,int muted){TextView t=text(a,message,13,muted);t.setPadding(0,dp(a,12),0,dp(a,6));list.addView(t);}
 static void row(Activity a,LinearLayout list,String name,String value,int ink,int muted,Runnable action){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,14),dp(a,12),dp(a,14),dp(a,12));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();boolean light=CinemaTheme.light(a),gold=!light&&CinemaTheme.gold(a),clay=light&&CinemaTheme.clay(a);bg.setColor(light?(clay?CinemaTheme.SHEET:0xffffffff):gold?CinemaTheme.CARD:0xff19434e);bg.setCornerRadius(dp(a,12));row.setBackground(bg);TextView n=text(a,name,15,ink);row.addView(n);TextView v=text(a,value,13,muted);v.setPadding(0,dp(a,4),0,0);row.addView(v);row.setOnClickListener(x->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(a,8);row.setLayoutParams(lp);list.addView(row);}
 static TextView chip(Activity a,String label,Runnable action){TextView t=text(a,label,14,CinemaTheme.light(a)?CinemaTheme.SLATE:0xffffffff);t.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));t.setOnClickListener(v->action.run());return t;}
 static TextView text(Activity a,String s,int size,int color){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
}
