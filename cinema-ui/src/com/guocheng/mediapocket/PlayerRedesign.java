package com.guocheng.mediapocket;
import android.app.Activity;import android.content.res.ColorStateList;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.61 播放页重新排版（在原播放器控件上整理，不重建播放逻辑）：
 *  顶栏：‹ 片名 …… 画中画 · 旋转（只留图标）；去掉与底栏重复的「视频」（同「播放器」菜单）。
 *  中间：只留播放/暂停。左右「后退 / 前进 10 秒」按钮不显示，双击左右三分之一直接跳转。
 *  底栏：状态 → 进度条 → 胶囊按钮行。倍速只留画面右侧；底栏那颗字是「倍速」、说明是「播放速度」，见 PlayerChromeRules。
 * 幂等：可反复调用（补齐后加入的按钮、横竖屏切换后重新定位）。
 */
final class PlayerRedesign {
 static final Map<View,Boolean> STYLED=new WeakHashMap<>();
 private PlayerRedesign(){}
 static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 static String desc(View v){CharSequence c=v.getContentDescription();return c==null?"":c.toString();}
 static Drawable circle(int color){ShapeDrawable d=new ShapeDrawable(new android.graphics.drawable.shapes.OvalShape());d.getPaint().setColor(color);return new RippleDrawable(ColorStateList.valueOf(0x40ffffff),d,null);}
 static Drawable pill(Activity a,int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(a,19));return new RippleDrawable(ColorStateList.valueOf(0x40ffffff),g,null);}

 static void apply(Activity a){try{hideSeekButtons(a);PlayerOverlay.layout(a);}catch(Exception ignored){}}

 /** 图标按钮：去掉文字，图标居中，44dp 圆形点击区。 */
 static void iconOnly(Activity a,Button b,Drawable icon){
  if(icon!=null){int s=dp(a,22);icon.setBounds(0,0,s,s);b.setCompoundDrawables(icon,null,null,null);}
  Drawable[] d=b.getCompoundDrawables();int w=d[0]==null?dp(a,22):d[0].getBounds().width();int side=dp(a,44),pad=Math.max(0,(side-w)/2);
  b.setText("");b.setCompoundDrawablePadding(0);b.setPadding(pad,0,pad,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setGravity(Gravity.CENTER);b.setBackground(circle(0x00000000));
  ViewGroup.LayoutParams lp=b.getLayoutParams();if(lp instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams l=(LinearLayout.LayoutParams)lp;l.width=side;l.height=side;l.leftMargin=dp(a,4);b.setLayoutParams(l);}
 }
 static void top(Activity a){
  Object t=OnlineSubtitleSearch.field(a,"top");if(!(t instanceof LinearLayout))return;LinearLayout top=(LinearLayout)t;
  top.setPadding(dp(a,12),dp(a,10),dp(a,16),dp(a,28));
  for(int i=0;i<top.getChildCount();i++){
   View v=top.getChildAt(i);String d=desc(v);
   if(v instanceof TextView&&!(v instanceof Button)){TextView title=(TextView)v;title.setTextSize(16);title.setTypeface(Typeface.DEFAULT_BOLD);continue;}
   if(!(v instanceof Button)||STYLED.containsKey(v))continue;Button b=(Button)v;
   if(d.equals("视频")){b.setVisibility(View.GONE);STYLED.put(v,true);continue;}// 与底栏「播放器」同一个菜单
   if(d.equals("旋转")){iconOnly(a,b,null);STYLED.put(v,true);continue;}
   if(d.equals("画中画")){iconOnly(a,b,new PlayerIcons(PlayerIcons.PIP,dp(a,22)));STYLED.put(v,true);continue;}
   if(d.equals("返回")){STYLED.put(v,true);continue;}
  }
 }
 /** 左右快进/快退按钮保持隐藏。双击左右区域直接跳 10 秒，不靠这两个按钮。 */
 static void hideSeekButtons(Activity a){Object c=OnlineSubtitleSearch.field(a,"center");if(!(c instanceof ViewGroup))return;ViewGroup center=(ViewGroup)c;for(int i=0;i<center.getChildCount();i++){View v=center.getChildAt(i);if(!(v instanceof Button))continue;String d=desc(v);if(d.contains("后退")||d.contains("前进")||d.contains("快退")||d.contains("快进")){v.setVisibility(View.GONE);v.setTranslationX(0);}}}
 static void center(Activity a){
  hideSeekButtons(a);
  Object c=OnlineSubtitleSearch.field(a,"center");if(!(c instanceof FrameLayout))return;FrameLayout center=(FrameLayout)c;
  for(int i=0;i<center.getChildCount();i++){
   View v=center.getChildAt(i);if(!(v instanceof Button))continue;String d=desc(v);
   if(!d.contains("播放或暂停"))continue;
   int size=dp(a,76);
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(size,size,Gravity.CENTER);v.setLayoutParams(lp);v.setTranslationX(0);
   if(!STYLED.containsKey(v)){v.setBackground(circle(0x59000000));v.setPadding(0,0,0,0);STYLED.put(v,true);}
  }
 }
 static void bottom(Activity a){
  Object b=OnlineSubtitleSearch.field(a,"bottom");if(!(b instanceof LinearLayout))return;LinearLayout bottom=(LinearLayout)b;
  bottom.setPadding(dp(a,20),dp(a,36),dp(a,20),dp(a,14));
  Object state=OnlineSubtitleSearch.field(a,"state");if(state instanceof TextView){TextView s=(TextView)state;s.setTextSize(12);s.setTextColor(0xb3ffffff);s.setPadding(dp(a,4),0,0,dp(a,2));}
  for(String name:new String[]{"elapsed","clock"}){Object t=OnlineSubtitleSearch.field(a,name);if(t instanceof TextView){TextView tv=(TextView)t;tv.setTextSize(13);tv.setFontFeatureSettings("tnum");tv.setTextColor(0xe6ffffff);}}
  for(int i=0;i<bottom.getChildCount();i++){
   View child=bottom.getChildAt(i);if(!(child instanceof HorizontalScrollView))continue;View inner=((HorizontalScrollView)child).getChildAt(0);if(!(inner instanceof LinearLayout))continue;
   LinearLayout row=(LinearLayout)inner;row.setGravity(Gravity.CENTER);row.setPadding(0,dp(a,6),0,0);
   for(int n=0;n<row.getChildCount();n++){
    View v=row.getChildAt(n);if(!(v instanceof Button))continue;Button btn=(Button)v;
    String text=btn.getText()==null?"":btn.getText().toString();
    if("mp-speed".equals(btn.getTag())||PlayerChromeRules.legacySpeed(text,desc(btn))){btn.setTag("mp-speed");btn.setVisibility(View.GONE);continue;}
    if(STYLED.containsKey(v))continue;
    btn.setTextSize(13);btn.setBackground(pill(a,0x29ffffff));btn.setPadding(dp(a,14),0,dp(a,16),0);btn.setMinHeight(0);btn.setMinimumHeight(0);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(a,38));lp.leftMargin=dp(a,4);lp.rightMargin=dp(a,4);btn.setLayoutParams(lp);
    STYLED.put(v,true);
   }
  }
 }
 static void refreshSpeed(Activity a){PlayerChrome c=PlayerChrome.ALL.get(a);if(c!=null)c.paintSpeed();}
}
