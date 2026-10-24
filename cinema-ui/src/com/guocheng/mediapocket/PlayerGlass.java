package com.guocheng.mediapocket;
import android.app.Activity;import android.content.res.ColorStateList;import android.graphics.drawable.*;import android.os.Build;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.139 播放器「三层悬浮面板」（花叔Design 方向 2，参照 Infuse 播放页）：
 * 顶部左右两组半透明胶囊（返回 + 片名 ／ 解码、比例、画中画、更多），正中三颗圆键（−10、播放、+10），
 * 底部一整块圆角进度面板（集名、剩余时间、进度、传输与音轨字幕选集）。整页不再铺通栏黑色渐变。
 * 两个播放器共用这里的颜色和形状；VLC 播放器的原版点击只交给 Button / SeekBar，所以新加的可点控件一律是 Button。
 */
final class PlayerGlass {
 private PlayerGlass(){}
 /** 暖炭色玻璃：约 55% 不透明，细白描边。CSS 的 backdrop 模糊在安卓 12 以下做不到，这里用更实的底色保证字看得清。 */
 static final int GLASS=0x8C1C1A18,GLASS_STRONG=0xB31C1A18,STROKE=0x26ffffff,DISC=0xEBF4F1EA,DISC_ICON=0xff141413;
 static final int CIRCLE_DP=44,CENTER_DP=56,PLAY_DP=76,CENTER_GAP_DP=36,PANEL_RADIUS_DP=22;
 static final Map<View,String> STYLED=new WeakHashMap<>();
 static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 static Drawable shape(Activity a,int color,float radiusDp,boolean oval){GradientDrawable g=new GradientDrawable();g.setColor(color);if(oval)g.setShape(GradientDrawable.OVAL);else g.setCornerRadius(dp(a,radiusDp));g.setStroke(Math.max(1,dp(a,1)),color==DISC?0x00000000:STROKE);return new RippleDrawable(ColorStateList.valueOf(0x33ffffff),g,null);}
 static Drawable circle(Activity a){return shape(a,GLASS,0,true);}
 static Drawable pill(Activity a){return shape(a,GLASS,CIRCLE_DP/2f,false);}
 /** 只在第一次或换了样式时设背景，轮询里反复调用不重复创建 Drawable。 */
 static void dress(Activity a,View v,String kind){if(v==null||kind.equals(STYLED.get(v)))return;STYLED.put(v,kind);v.setBackground("circle".equals(kind)?circle(a):"disc".equals(kind)?shape(a,DISC,0,true):pill(a));}
 /** 底部面板：圆角玻璃，左右各比内容宽出 12dp，底部离屏幕边 6dp 以上。 */
 static void panel(Activity a,View bottom,int side,int bottomPad){if(bottom==null)return;int inX=Math.max(0,side-dp(a,12)),inB=Math.max(0,bottomPad-dp(a,10));String key="panel:"+inX+":"+inB;if(key.equals(STYLED.get(bottom)))return;STYLED.put(bottom,key);GradientDrawable g=new GradientDrawable();g.setColor(GLASS);g.setCornerRadius(dp(a,PANEL_RADIUS_DP));g.setStroke(Math.max(1,dp(a,1)),STROKE);bottom.setBackground(new InsetDrawable(g,inX,0,inX,inB));}
 /** 顶部只留一道很淡的压暗，让状态栏和胶囊在亮画面上也看得清。 */
 static GradientDrawable topFade(){return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x66000000,0x00000000});}

 static String remaining(long posMs,long durMs,float speed,long nowMs,TimeZone zone){return PlayerGlassRules.remaining(posMs,durMs,speed,nowMs,zone);}

 /** 正中三颗键。host 是播放器根 FrameLayout；按钮都是 Button。返回 {后退, 播放, 前进}。 */
 static Button[] center(Activity a,FrameLayout host,int seconds,View.OnClickListener back,View.OnClickListener play,View.OnClickListener forward){
  View found=host.findViewWithTag("mp-center");LinearLayout row;
  if(found instanceof LinearLayout)row=(LinearLayout)found;else{row=new LinearLayout(a);row.setTag("mp-center");row.setGravity(Gravity.CENTER);row.setOrientation(LinearLayout.HORIZONTAL);
   row.addView(seekKey(a,"−"+seconds,"后退 "+seconds+" 秒",back),centerLp(a,CENTER_DP,true));
   Button p=new Button(a);p.setTag("mp-center-play");p.setAllCaps(false);p.setText("");p.setMinWidth(0);p.setMinimumWidth(0);p.setMinHeight(0);p.setMinimumHeight(0);p.setContentDescription("播放或暂停");p.setOnClickListener(play);dress(a,p,"disc");p.setStateListAnimator(null);row.addView(p,centerLp(a,PLAY_DP,true));
   row.addView(seekKey(a,"+"+seconds,"前进 "+seconds+" 秒",forward),centerLp(a,CENTER_DP,false));
   host.addView(row,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));}
  return new Button[]{(Button)row.getChildAt(0),(Button)row.getChildAt(1),(Button)row.getChildAt(2)};}
 static Button seekKey(Activity a,String label,String description,View.OnClickListener l){Button b=new Button(a);b.setAllCaps(false);b.setText(label);b.setTextSize(14);b.setTextColor(0xffffffff);b.setTypeface(android.graphics.Typeface.MONOSPACE);b.setIncludeFontPadding(false);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setContentDescription(description);b.setOnClickListener(l);b.setStateListAnimator(null);dress(a,b,"circle");return b;}
 static LinearLayout.LayoutParams centerLp(Activity a,int sizeDp,boolean gapAfter){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(a,sizeDp),dp(a,sizeDp));p.gravity=Gravity.CENTER_VERTICAL;if(gapAfter)p.rightMargin=dp(a,CENTER_GAP_DP);return p;}
 static final Map<View,Boolean> PAINTED=new WeakHashMap<>();
 static void paintPlay(Activity a,Button play,boolean playing){if(play==null||Boolean.valueOf(playing).equals(PAINTED.get(play)))return;PAINTED.put(play,playing);PlayerOverlay.paint(a,play,playing?PlayerIcons.PAUSE:PlayerIcons.PLAY,DISC_ICON,30);play.setContentDescription(playing?"暂停":"播放");}
}
