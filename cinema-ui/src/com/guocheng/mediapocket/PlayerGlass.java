package com.guocheng.mediapocket;
import android.app.Activity;import android.content.res.ColorStateList;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.140 播放器「三层悬浮面板」（花叔Design 方向 2）：
 * 顶部左右两组玻璃胶囊，正中播放盘（±N 点开后短暂停一下），底部一块圆角进度面板。
 * 整页不再铺通栏黑色渐变。两个播放器共用这里的颜色和形状；新加的可点控件一律是 Button。
 */
final class PlayerGlass {
 private PlayerGlass(){}
 /** 暖炭玻璃。亮场实际用多少不透明，由 PlayerGlassRules 按对比算，这里留着暗场和加厚两档。 */
 static final int GLASS=0x8C1C1A18,GLASS_STRONG=0xB31C1A18,STROKE=0x26ffffff,DISC=0xEBF4F1EA,DISC_ICON=0xff141413;
 static final int CIRCLE_DP=PlayerGlassRules.TOUCH_DP,CENTER_DP=56,PLAY_DP=76,CENTER_GAP_DP=36,PANEL_RADIUS_DP=22;
 static final Map<View,String> STYLED=new WeakHashMap<>();
 static final Map<View,Long> SEEK_UNTIL=new WeakHashMap<>();
 static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 static int chipColor(){return PlayerGlassRules.glassColor(PlayerGlassRules.glassAlpha(255,PlayerGlassRules.WHITE));}
 static int panelColor(Activity a){return PlayerGlassRules.glassColor(PlayerGlassRules.panelAlpha(255,CinemaTheme.gold(a)));}
 static Drawable shape(Activity a,int color,float radiusDp,boolean oval){GradientDrawable g=new GradientDrawable();g.setColor(color);if(oval)g.setShape(GradientDrawable.OVAL);else g.setCornerRadius(dp(a,radiusDp));g.setStroke(Math.max(1,dp(a,1)),color==DISC?0x00000000:STROKE);return new RippleDrawable(ColorStateList.valueOf(0x33ffffff),g,null);}
 static Drawable circle(Activity a){return shape(a,chipColor(),0,true);}
 static Drawable pill(Activity a){return shape(a,chipColor(),CIRCLE_DP/2f,false);}
 /** 只在第一次或换了样式时设背景，轮询里反复调用不重复创建 Drawable。 */
 static void dress(Activity a,View v,String kind){if(v==null||kind.equals(STYLED.get(v)))return;STYLED.put(v,kind);v.setBackground("circle".equals(kind)?circle(a):"disc".equals(kind)?shape(a,DISC,0,true):pill(a));}
 static GradientDrawable panelDrawable(Activity a){GradientDrawable g=new GradientDrawable();g.setColor(panelColor(a));g.setCornerRadius(dp(a,PANEL_RADIUS_DP));g.setStroke(Math.max(1,dp(a,1)),STROKE);return g;}
 /** 底部面板：圆角玻璃，左右各比内容宽出 12dp，底部离屏幕边 6dp 以上。亮场加厚到字对比 ≥4.5:1。 */
 static void panel(Activity a,View bottom,int side,int bottomPad){if(bottom==null)return;int inX=Math.max(0,side-dp(a,12)),inB=Math.max(0,bottomPad-dp(a,10));int color=panelColor(a);String key="panel:"+inX+":"+inB+":"+color;if(key.equals(STYLED.get(bottom)))return;STYLED.put(bottom,key);bottom.setBackground(new InsetDrawable(panelDrawable(a),inX,0,inX,inB));}
 /** 顶部一道压暗，托状态栏。字主要靠胶囊自己的不透明度。 */
 static GradientDrawable topFade(){return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x99000000,0x00000000});}

 static String remaining(long posMs,long durMs,float speed,long nowMs,TimeZone zone){return PlayerGlassRules.remaining(posMs,durMs,speed,nowMs,zone);}

 /** 点开之后记下 ±N 该露到什么时候。 */
 static void flashSeek(View row){if(row!=null)SEEK_UNTIL.put(row,android.os.SystemClock.elapsedRealtime()+PlayerGlassRules.SEEK_FLASH_MS);}
 static boolean seekVisible(View row){Long until=row==null?null:SEEK_UNTIL.get(row);return until!=null&&PlayerGlassRules.seekKeysOn(android.os.SystemClock.elapsedRealtime(),until);}
 /** ±N 收起时播放盘回到正中，右边不再空出一截间距。 */
 static void applySeek(Activity a,Button back,Button play,Button forward,boolean on){
  int v=on?View.VISIBLE:View.GONE;
  if(back!=null&&back.getVisibility()!=v)back.setVisibility(v);
  if(forward!=null&&forward.getVisibility()!=v)forward.setVisibility(v);
  if(play!=null&&play.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)play.getLayoutParams();int gap=on?dp(a,CENTER_GAP_DP):0;if(p.rightMargin!=gap){p.rightMargin=gap;play.setLayoutParams(p);}}
 }

 /** 正中三颗键。默认先都建好；露面时只留播放盘，±N 交给短暂显示和双击。返回 {后退, 播放, 前进}。 */
 static Button[] center(Activity a,FrameLayout host,int seconds,View.OnClickListener back,View.OnClickListener play,View.OnClickListener forward){
  View found=host.findViewWithTag("mp-center");LinearLayout row;
  if(found instanceof LinearLayout)row=(LinearLayout)found;else{row=new LinearLayout(a);row.setTag("mp-center");row.setGravity(Gravity.CENTER);row.setOrientation(LinearLayout.HORIZONTAL);
   row.addView(seekKey(a,"−"+seconds,"后退 "+seconds+" 秒",back),centerLp(a,CENTER_DP,true));
   Button p=new Button(a);p.setTag("mp-center-play");p.setAllCaps(false);p.setText("");p.setMinWidth(0);p.setMinimumWidth(0);p.setMinHeight(0);p.setMinimumHeight(0);p.setContentDescription("播放或暂停");p.setOnClickListener(play);dress(a,p,"disc");p.setStateListAnimator(null);row.addView(p,centerLp(a,PLAY_DP,true));
   row.addView(seekKey(a,"+"+seconds,"前进 "+seconds+" 秒",forward),centerLp(a,CENTER_DP,false));
   host.addView(row,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));}
  return new Button[]{(Button)row.getChildAt(0),(Button)row.getChildAt(1),(Button)row.getChildAt(2)};}
 static Button seekKey(Activity a,String label,String description,View.OnClickListener l){Button b=new Button(a);b.setAllCaps(false);b.setText(label);b.setTextSize(14);b.setTextColor(0xffffffff);b.setTypeface(android.graphics.Typeface.SANS_SERIF);b.setFontFeatureSettings("tnum");b.setIncludeFontPadding(false);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setContentDescription(description);b.setOnClickListener(l);b.setStateListAnimator(null);dress(a,b,"circle");return b;}
 static LinearLayout.LayoutParams centerLp(Activity a,int sizeDp,boolean gapAfter){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(a,sizeDp),dp(a,sizeDp));p.gravity=Gravity.CENTER_VERTICAL;if(gapAfter)p.rightMargin=dp(a,CENTER_GAP_DP);return p;}
 static final Map<View,Boolean> PAINTED=new WeakHashMap<>();
 static void paintPlay(Activity a,Button play,boolean playing){if(play==null||Boolean.valueOf(playing).equals(PAINTED.get(play)))return;PAINTED.put(play,playing);PlayerOverlay.paint(a,play,playing?PlayerIcons.PAUSE:PlayerIcons.PLAY,DISC_ICON,30);play.setContentDescription(playing?"暂停":"播放");}
}
