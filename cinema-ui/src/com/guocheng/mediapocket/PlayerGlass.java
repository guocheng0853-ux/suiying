package com.guocheng.mediapocket;
import android.app.Activity;import android.content.res.ColorStateList;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.141 播放控制层，参考 Apple TV App 的播放页（按花叔Design 规范收敛，见 docs/design/huashu-audit-0141.md）：
 *  · 控件出现时整块画面压暗约 55%，不再用一块块玻璃面板托字；顶部、底部再各加一道渐暗。
 *  · 顶部：左返回，右解码 / 比例 / 画中画 / 更多，全部是不带底的白色图标，可点区域 48dp。
 *  · 正中：后退 N 秒、播放 / 暂停、前进 N 秒三个白色图形，没有圆底，始终三个一起出现。
 *  · 底部（各播放器自己排）：左集名，右一排小图标；通栏进度条；进度条下左已播、右「剩…，…看完」。
 * 方法名沿用 0.1.139 / 0.1.140，调用方不用改；VLC 播放器原版只把点击交给 Button / SeekBar，可点控件一律是 Button。
 */
final class PlayerGlass {
 private PlayerGlass(){}
 static final int GLASS=0x8C1C1A18,GLASS_STRONG=0xB31C1A18,STROKE=0x26ffffff;
 static final int CIRCLE_DP=PlayerGlassRules.TOUCH_DP,CENTER_DP=64,PLAY_DP=80,CENTER_GAP_DP=56,PANEL_RADIUS_DP=20,PLAY_ICON_DP=44,SKIP_ICON_DP=34;
 static final Map<View,String> STYLED=new WeakHashMap<>();
 static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 /** 无底的点按反馈：只有按下时一圈淡白波纹。 */
 static Drawable plain(Activity a){return new RippleDrawable(ColorStateList.valueOf(0x33ffffff),null,new ColorDrawable(0xffffffff));}
 /** 0.1.141 起所有控件都不带圆底 / 胶囊底（kind 只为兼容旧调用）。 */
 static void dress(Activity a,View v,String kind){if(v==null||"plain".equals(STYLED.get(v)))return;STYLED.put(v,"plain");if(v instanceof Button)v.setBackground(plain(a));else{v.setBackground(null);if(v instanceof TextView)v.setPadding(dp(a,4),0,dp(a,4),0);}}
 /** 底部渐暗：从透明到 85% 黑，托住集名、进度和时间。 */
 static GradientDrawable panelDrawable(Activity a){return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x8C000000,0xD9000000});}
 static void panel(Activity a,View bottom,int side,int bottomPad){if(bottom==null||"fade".equals(STYLED.get(bottom)))return;STYLED.put(bottom,"fade");bottom.setBackground(panelDrawable(a));}
 static GradientDrawable topFade(){return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xB3000000,0x00000000});}
 static String remaining(long posMs,long durMs,float speed,long nowMs,TimeZone zone){return PlayerGlassRules.remaining(posMs,durMs,speed,nowMs,zone);}
 // 旧的「±N 短暂露面」不再需要：Apple TV 三个键始终一起出现
 static void flashSeek(View row){}
 static boolean seekVisible(View row){return true;}
 static void applySeek(Activity a,Button back,Button play,Button forward,boolean on){for(Button b:new Button[]{back,forward})if(b!=null&&b.getVisibility()!=View.VISIBLE)b.setVisibility(View.VISIBLE);}

 /** 整块画面压暗层，紧贴在视频画面之上、所有控件之下。videoChild 是根布局里装着视频画面的那个直接子 View。 */
 static View dim(Activity a,FrameLayout root,View videoChild){View found=root.findViewWithTag("mp-dim");if(found!=null)return found;View d=new View(a);d.setTag("mp-dim");d.setBackgroundColor(PlayerGlassRules.DIM_ALPHA<<24);d.setClickable(false);d.setFocusable(false);d.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);d.setVisibility(View.GONE);int at=videoChild==null?0:root.indexOfChild(videoChild)+1;root.addView(d,Math.max(0,Math.min(at,root.getChildCount())),new FrameLayout.LayoutParams(-1,-1));return d;}
 static void showDim(View dim,boolean on){if(dim==null)return;int v=on?View.VISIBLE:View.GONE;if(dim.getVisibility()!=v)dim.setVisibility(v);}

 /** 正中三个键：后退 N 秒、播放 / 暂停、前进 N 秒，都是 Button。返回 {后退, 播放, 前进}。 */
 static Button[] center(Activity a,FrameLayout host,int seconds,View.OnClickListener back,View.OnClickListener play,View.OnClickListener forward){
  View found=host.findViewWithTag("mp-center");LinearLayout row;
  if(found instanceof LinearLayout)row=(LinearLayout)found;else{row=new LinearLayout(a);row.setTag("mp-center");row.setGravity(Gravity.CENTER);row.setOrientation(LinearLayout.HORIZONTAL);
   row.addView(skipKey(a,false,seconds,back),centerLp(a,CENTER_DP,true));
   Button p=new Button(a);p.setTag("mp-center-play");p.setAllCaps(false);p.setText("");p.setMinWidth(0);p.setMinimumWidth(0);p.setMinHeight(0);p.setMinimumHeight(0);p.setContentDescription("播放或暂停");p.setOnClickListener(play);p.setStateListAnimator(null);p.setBackground(plain(a));row.addView(p,centerLp(a,PLAY_DP,true));
   row.addView(skipKey(a,true,seconds,forward),centerLp(a,CENTER_DP,false));
   host.addView(row,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));}
  return new Button[]{(Button)row.getChildAt(0),(Button)row.getChildAt(1),(Button)row.getChildAt(2)};}
 static Button skipKey(Activity a,boolean forward,int seconds,View.OnClickListener l){Button b=new Button(a);b.setAllCaps(false);b.setText("");b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setContentDescription((forward?"前进 ":"后退 ")+seconds+" 秒");b.setOnClickListener(l);b.setStateListAnimator(null);b.setBackground(plain(a));
  int s=dp(a,SKIP_ICON_DP);SkipIcon icon=new SkipIcon(forward,seconds,a.getResources().getDisplayMetrics().density);icon.setBounds(0,0,s,s);b.setCompoundDrawables(null,icon,null,null);int pad=Math.max(0,(dp(a,CENTER_DP)-s)/2);b.setPadding(0,pad,0,0);b.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);return b;}
 static LinearLayout.LayoutParams centerLp(Activity a,int sizeDp,boolean gapAfter){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(a,sizeDp),dp(a,sizeDp));p.gravity=Gravity.CENTER_VERTICAL;if(gapAfter)p.rightMargin=dp(a,CENTER_GAP_DP);return p;}
 /** 顶部图标在按钮里上下左右居中：按按钮实际高度算上边距（图标画在 drawableTop）。文字按钮不动。 */
 static void centerIcon(Activity a,Button b,int boxPx){if(b==null)return;Drawable[] d=b.getCompoundDrawables();Drawable icon=d[1];if(icon==null||!(b.getText()==null||b.getText().length()==0))return;int h=icon.getBounds().height();int pad=Math.max(0,(boxPx-h)/2);if(b.getPaddingTop()!=pad||b.getPaddingLeft()!=0)b.setPadding(0,pad,0,0);b.setCompoundDrawablePadding(0);b.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);}
 static final Map<View,Boolean> PAINTED=new WeakHashMap<>();
 /** 白色播放 / 暂停图形，画在按钮正中（0.1.139 的版本贴在顶上，这里按按钮高度算上边距）。 */
 static void paintPlay(Activity a,Button play,boolean playing){if(play==null||Boolean.valueOf(playing).equals(PAINTED.get(play)))return;PAINTED.put(play,playing);int s=dp(a,PLAY_ICON_DP);PlayerIcons icon=new PlayerIcons(playing?PlayerIcons.PAUSE:PlayerIcons.PLAY,s);icon.setBounds(0,0,s,s);play.setCompoundDrawables(null,icon,null,null);play.setCompoundDrawablePadding(0);int h=play.getLayoutParams()!=null&&play.getLayoutParams().height>0?play.getLayoutParams().height:dp(a,PLAY_DP);play.setPadding(0,Math.max(0,(h-s)/2),0,0);play.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);play.setContentDescription(playing?"暂停":"播放");}

 /** 带数字的环形箭头（「后退 15」「前进 15」），白色描边，数字居中。 */
 static final class SkipIcon extends Drawable {
  final boolean forward;final String number;final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG),text=new Paint(Paint.ANTI_ALIAS_FLAG);final float density;
  SkipIcon(boolean forward,int seconds,float density){this.forward=forward;this.number=String.valueOf(seconds);this.density=density<=0?1:density;stroke.setStyle(Paint.Style.STROKE);stroke.setColor(0xffffffff);stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeJoin(Paint.Join.ROUND);text.setColor(0xffffffff);text.setTextAlign(Paint.Align.CENTER);text.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
  @Override public void draw(Canvas c){Rect b=getBounds();float side=Math.min(b.width(),b.height());if(side<=0)return;float sw=Math.max(1.5f,side*0.07f);stroke.setStrokeWidth(sw);float cx=b.exactCenterX(),cy=b.exactCenterY(),r=side/2f-sw*1.6f;
   RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);
   // 后退：缺口在左上，箭头在正上方偏右、朝左（逆时针）；前进镜像
   float start=forward?-120f:-60f,sweep=forward?-300f:300f;c.drawArc(oval,start,sweep,false,stroke);
   double t=Math.toRadians(start);float ax=cx+(float)(r*Math.cos(t)),ay=cy+(float)(r*Math.sin(t));float dir=forward?1f:-1f,head=side*0.16f;
   Path arrow=new Path();arrow.moveTo(ax+dir*head,ay);arrow.lineTo(ax,ay);arrow.lineTo(ax+dir*head*0.15f,ay+head);c.drawPath(arrow,stroke);
   text.setTextSize(side*0.34f);Paint.FontMetrics f=text.getFontMetrics();c.drawText(number,cx,cy-(f.ascent+f.descent)/2f+side*0.03f,text);}
  @Override public void setAlpha(int alpha){stroke.setAlpha(alpha);text.setAlpha(alpha);}
  @Override public void setColorFilter(ColorFilter cf){stroke.setColorFilter(cf);text.setColorFilter(cf);}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  @Override public int getIntrinsicWidth(){return Math.round(SKIP_ICON_DP*density);}
  @Override public int getIntrinsicHeight(){return Math.round(SKIP_ICON_DP*density);}
 }
}
