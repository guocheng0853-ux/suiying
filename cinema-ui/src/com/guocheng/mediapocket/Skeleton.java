package com.guocheng.mediapocket;
import android.animation.ObjectAnimator;import android.animation.ValueAnimator;import android.content.Context;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;
/**
 * 0.1.123 加载骨架（参考 Streamyfin）：灰色占位块按真实布局排好，整体呼吸闪动；内容回来后由调用方替换。
 * 所有方法只需要 Context 和占位色，首页、媒体库、Emby / 影视仓详情页共用。
 */
final class Skeleton {
 private Skeleton(){}
 static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
 static View block(Context c,int color,int w,int h,int radius){View v=new View(c);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,radius));v.setBackground(g);v.setLayoutParams(new LinearLayout.LayoutParams(w,h));return v;}
 static <T extends View> T pulse(T v){ObjectAnimator a=ObjectAnimator.ofFloat(v,"alpha",1f,.45f);a.setDuration(700);a.setRepeatMode(ValueAnimator.REVERSE);a.setRepeatCount(ValueAnimator.INFINITE);a.start();v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View x){if(!a.isStarted())a.start();}public void onViewDetachedFromWindow(View x){a.cancel();}});v.setTag("mp-skeleton");return v;}

 /** 首页一个分区：标题条 + 一行横向卡片（landscape=true 为继续观看的横版卡）。 */
 static View shelf(Context c,int color,boolean landscape,int count){LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,dp(c,14),0,dp(c,6));
  View title=block(c,color,dp(c,120),dp(c,18),6);((LinearLayout.LayoutParams)title.getLayoutParams()).bottomMargin=dp(c,10);box.addView(title);
  LinearLayout row=new LinearLayout(c);int w=landscape?dp(c,200):dp(c,110),h=landscape?Math.round(w/1.72f):Math.round(w*1.5f);
  for(int i=0;i<count;i++){LinearLayout cell=new LinearLayout(c);cell.setOrientation(LinearLayout.VERTICAL);cell.addView(block(c,color,w,h,10));View t=block(c,color,Math.round(w*.7f),dp(c,10),4);((LinearLayout.LayoutParams)t.getLayoutParams()).topMargin=dp(c,8);cell.addView(t);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(w,-2);lp.rightMargin=dp(c,10);row.addView(cell,lp);}
  HorizontalScrollView hs=new HorizontalScrollView(c);hs.setHorizontalScrollBarEnabled(false);hs.addView(row);box.addView(hs);return pulse(box);}

 /** 海报网格（媒体库、影视仓分类）。 */
 static View grid(Context c,int color,int availableWidth,int rows){LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);int columns=Math.max(3,Math.min(6,availableWidth/dp(c,140))),w=(availableWidth-dp(c,10)*(columns-1))/columns;
  for(int r=0;r<rows;r++){LinearLayout line=new LinearLayout(c);for(int i=0;i<columns;i++){LinearLayout cell=new LinearLayout(c);cell.setOrientation(LinearLayout.VERTICAL);cell.addView(block(c,color,w,Math.round(w*1.5f),10));View t=block(c,color,Math.round(w*.75f),dp(c,10),4);((LinearLayout.LayoutParams)t.getLayoutParams()).topMargin=dp(c,6);cell.addView(t);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(w,-2);if(i<columns-1)lp.rightMargin=dp(c,10);lp.bottomMargin=dp(c,12);line.addView(cell,lp);}box.addView(line);}
  return pulse(box);}

 /** 详情页：大图 + 片名 + 信息行 + 主按钮 + 三行简介。heroWidth 用整页宽（负边距由调用方处理）。 */
 static View detail(Context c,int color,int heroHeight){LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);
  box.addView(block(c,color,-1,heroHeight,0));
  int[][] bars={{220,26,14},{160,12,10},{-1,48,12},{-1,12,6},{-1,12,6},{200,12,6}};for(int[] b:bars){View v=block(c,color,b[0]<0?-1:dp(c,b[0]),dp(c,b[1]),b[1]>=40?24:6);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)v.getLayoutParams();lp.topMargin=dp(c,b[2]);box.addView(v);}
  return pulse(box);}

 /** 去掉容器里所有骨架。 */
 static void clear(ViewGroup parent){if(parent==null)return;for(int i=parent.getChildCount()-1;i>=0;i--)if("mp-skeleton".equals(parent.getChildAt(i).getTag()))parent.removeViewAt(i);}
}
