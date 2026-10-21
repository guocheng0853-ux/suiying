package com.guocheng.mediapocket;
import android.app.Activity;import android.content.SharedPreferences;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.123 字幕样式（参考 Wholphin）：字号、外观（阴影 / 粗描边 / 半透明底框）、高度。VLC 播放页和内置杜比播放页共用。
 * 只改文字字幕的显示；图形字幕（PGS 等）不受影响。
 */
final class SubtitleStyle {
 private SubtitleStyle(){}
 static final String[] SIZES={"small","normal","large","huge"},SIZE_LABELS={"小","标准","大","特大"};static final int[] SIZE_SP={17,21,25,30};
 static final String[] LOOKS={"shadow","outline","box"},LOOK_LABELS={"阴影","粗描边（亮场景更清楚）","半透明底框"};
 static final String[] LIFTS={"0","1","2","3"},LIFT_LABELS={"贴底","稍高","中等","较高"};static final int[] LIFT_DP={0,24,56,96};
 static final int TAG=0x7f0a7302;
 static SharedPreferences prefs(Activity a){return a.getSharedPreferences("data",0);}
 static int index(String[] values,String v,int def){for(int i=0;i<values.length;i++)if(values[i].equals(v))return i;return def;}
 static int sizeSp(SharedPreferences p){return SIZE_SP[index(SIZES,p.getString("sub.size","normal"),1)];}
 static String look(SharedPreferences p){return LOOKS[index(LOOKS,p.getString("sub.look","shadow"),0)];}
 static int liftDp(SharedPreferences p){return LIFT_DP[index(LIFTS,p.getString("sub.lift","0"),0)];}
 static String label(Activity a){SharedPreferences p=prefs(a);return "字幕样式："+SIZE_LABELS[index(SIZES,p.getString("sub.size","normal"),1)]+" · "+LOOK_LABELS[index(LOOKS,look(p),0)].replaceAll("（.*）","")+" · "+LIFT_LABELS[index(LIFTS,p.getString("sub.lift","0"),0)];}

 /** baseHiddenPx：控制栏收起时字幕原本离底部的距离。 */
 static void style(Activity a,TextView tv,boolean controlsShown,int baseHiddenPx){if(tv==null)return;SharedPreferences p=prefs(a);float d=a.getResources().getDisplayMetrics().density;int size=sizeSp(p);String look=look(p);int lift=Math.round(liftDp(p)*d);
  String key=size+"|"+look;if(!key.equals(tv.getTag(TAG))){tv.setTag(TAG,key);tv.setTextSize(size);
   if("box".equals(look)){GradientDrawable g=new GradientDrawable();g.setColor(0x99000000);g.setCornerRadius(6*d);tv.setBackground(g);tv.setShadowLayer(0,0,0,0);tv.setTypeface(Typeface.DEFAULT);}
   else if("outline".equals(look)){tv.setBackground(null);tv.setShadowLayer(4*d,0,0,0xff000000);tv.setTypeface(Typeface.DEFAULT_BOLD);}
   else{tv.setBackground(null);tv.setShadowLayer(3*d,1,1,0xff000000);tv.setTypeface(Typeface.DEFAULT);}}
  if(!controlsShown&&tv.getLayoutParams() instanceof FrameLayout.LayoutParams){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)tv.getLayoutParams();int want=baseHiddenPx+lift;if(lp.bottomMargin!=want){lp.bottomMargin=want;tv.setLayoutParams(lp);}}}
 static void apply(Activity a,TextView tv,boolean controlsShown){style(a,tv,controlsShown,Math.round(20*a.getResources().getDisplayMetrics().density));}
 /** VLC 播放页：原版字幕控件 sub，控制栏收起时原版把它放在离底 24 像素处。 */
 static void applyVlc(Activity a){try{Object s=OnlineSubtitleSearch.field(a,"sub");if(!(s instanceof TextView))return;Object top=OnlineSubtitleSearch.field(a,"top");boolean shown=top instanceof View&&((View)top).getVisibility()==View.VISIBLE;style(a,(TextView)s,shown,24);}catch(Exception ignored){}}

 static void menu(Activity a,Runnable done){List<CinemaMenu.Item> rows=new ArrayList<>();SharedPreferences p=prefs(a);
  rows.add(CinemaMenu.item("字号："+SIZE_LABELS[index(SIZES,p.getString("sub.size","normal"),1)],()->pick(a,"sub.size","字幕字号",SIZES,SIZE_LABELS,"normal",done)));
  rows.add(CinemaMenu.item("外观："+LOOK_LABELS[index(LOOKS,look(p),0)],()->pick(a,"sub.look","字幕外观",LOOKS,LOOK_LABELS,"shadow",done)));
  rows.add(CinemaMenu.item("高度："+LIFT_LABELS[index(LIFTS,p.getString("sub.lift","0"),0)],()->pick(a,"sub.lift","字幕高度",LIFTS,LIFT_LABELS,"0",done)));
  CinemaMenu.show(a,"字幕样式",rows);}
 static void pick(Activity a,String key,String title,String[] values,String[] labels,String def,Runnable done){List<CinemaMenu.Item> rows=new ArrayList<>();String cur=prefs(a).getString(key,def);for(int i=0;i<values.length;i++){final String v=values[i];rows.add(CinemaMenu.choice(labels[i],v.equals(cur),()->{prefs(a).edit().putString(key,v).apply();if(done!=null)done.run();else menu(a,null);}));}CinemaMenu.show(a,title,rows);}
}
