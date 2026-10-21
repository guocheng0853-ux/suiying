package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;
/**
 * 播放时上下滑的音量 / 亮度指示。VLC 播放页和内置 Media3 播放页共用。
 * 圆角半透明药丸、矢量图标、进度条和百分比；亮画面和暗画面都靠深色底加浅色描边分开。
 * 指示挂在窗口 DecorView 的 tag 上，不放进静态表，避免值反过来抓住 Activity。
 */
final class LevelHud {
 static final int TAG=0x7f0e1201;
 final Activity a;final FrameLayout panel;final ImageView icon;final Bar bar;final TextView percent;final Runnable hideTask;int generation;boolean brightness;int value,scale=100;
 private LevelHud(Activity a,FrameLayout host){
  this.a=a;float d=a.getResources().getDisplayMetrics().density;int dp=LevelHudMetrics.WIDTH;
  panel=new FrameLayout(a);panel.setClickable(false);panel.setFocusable(false);
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
  int ph=Math.round(LevelHudMetrics.PAD_H*d),pv=Math.round(LevelHudMetrics.PAD_V*d);row.setPadding(ph,pv,ph,pv);
  GradientDrawable bg=new GradientDrawable();bg.setColor(LevelHudMetrics.PILL);bg.setCornerRadius(LevelHudMetrics.RADIUS*d);bg.setStroke(Math.max(1,Math.round(d)),LevelHudMetrics.STROKE);row.setBackground(bg);
  icon=new ImageView(a);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);int is=Math.round(LevelHudMetrics.ICON*d);row.addView(icon,new LinearLayout.LayoutParams(is,is));
  bar=new Bar(a);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,Math.round(LevelHudMetrics.BAR_H*d),1f);bp.leftMargin=Math.round(LevelHudMetrics.GAP*d);bp.rightMargin=Math.round(LevelHudMetrics.GAP*d);row.addView(bar,bp);
  percent=new TextView(a);percent.setTextColor(LevelHudMetrics.FG);percent.setTextSize(14);percent.setGravity(Gravity.CENTER);percent.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));percent.setIncludeFontPadding(false);
  row.addView(percent,new LinearLayout.LayoutParams(Math.round(LevelHudMetrics.TEXT_W*d),-2));
  panel.addView(row,new FrameLayout.LayoutParams(Math.round(dp*d),Math.round(LevelHudMetrics.HEIGHT*d),Gravity.TOP|Gravity.START));
  panel.setVisibility(View.GONE);panel.setAlpha(0f);panel.setElevation(8*d);
  host.addView(panel,new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START));
  hideTask=()->hide(true);
 }
 static LevelHud find(Activity a){if(a==null||a.getWindow()==null)return null;Object tag=a.getWindow().getDecorView().getTag(TAG);return tag instanceof LevelHud?(LevelHud)tag:null;}
 static void show(Activity a,FrameLayout host,boolean brightness,int value,int scale){if(a==null||host==null||a.getWindow()==null)return;LevelHud h=find(a);if(h==null||h.panel.getParent()!=host){if(h!=null)h.detach();h=new LevelHud(a,host);a.getWindow().getDecorView().setTag(TAG,h);}h.update(brightness,value,scale);}
 static void hideSoon(Activity a,long delay){LevelHud h=find(a);if(h==null||h.panel.getVisibility()!=View.VISIBLE)return;h.panel.removeCallbacks(h.hideTask);h.panel.postDelayed(h.hideTask,delay);}
 static void hide(Activity a,boolean animate){LevelHud h=find(a);if(h!=null)h.hide(animate);}
 static void dismiss(Activity a){LevelHud h=find(a);if(h==null)return;h.detach();if(a.getWindow()!=null)a.getWindow().getDecorView().setTag(TAG,null);}
 void detach(){panel.removeCallbacks(hideTask);panel.animate().cancel();if(panel.getParent() instanceof ViewGroup)((ViewGroup)panel.getParent()).removeView(panel);}
 void update(boolean brightness,int value,int scale){this.brightness=brightness;this.value=value;this.scale=scale<=0?100:scale;int iconType=brightness?LevelIconGeometry.SUN:LevelIconGeometry.volume(value);int px=Math.round(LevelHudMetrics.ICON*a.getResources().getDisplayMetrics().density);icon.setImageDrawable(new LevelIcons(iconType,px));bar.set(value,this.scale);percent.setText(value+"%");panel.setContentDescription((brightness?"亮度 ":"音量 ")+value+"%");place();panel.removeCallbacks(hideTask);boolean wasGone=panel.getVisibility()!=View.VISIBLE;int token=++generation;panel.animate().cancel();panel.setVisibility(View.VISIBLE);panel.bringToFront();if(wasGone)panel.setAlpha(0f);if(wasGone||panel.getAlpha()<0.99f)panel.animate().alpha(1f).setDuration(LevelHudMetrics.FADE_IN_MS).withEndAction(()->{if(token!=generation)return;panel.setAlpha(1f);}).start();else panel.setAlpha(1f);}
 void hide(boolean animate){panel.removeCallbacks(hideTask);int token=++generation;panel.animate().cancel();if(!animate){panel.setAlpha(0f);panel.setVisibility(View.GONE);return;}panel.animate().alpha(0f).setDuration(LevelHudMetrics.FADE_OUT_MS).withEndAction(()->{if(token!=generation)return;panel.setVisibility(View.GONE);}).start();}
 void place(){View host=(View)panel.getParent();if(host==null)return;int[] cut=PlayerOverlay.insets(host);float d=a.getResources().getDisplayMetrics().density;int hostW=host.getWidth()>0?host.getWidth():a.getResources().getDisplayMetrics().widthPixels;int hostH=host.getHeight()>0?host.getHeight():a.getResources().getDisplayMetrics().heightPixels;int pill=Math.round(LevelHudMetrics.WIDTH*d);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)panel.getLayoutParams();lp.gravity=Gravity.TOP|Gravity.START;lp.leftMargin=LevelHudMetrics.x(hostW,pill,d,cut[0],cut[2]);lp.topMargin=LevelHudMetrics.top(d,cut[1],hostH>=hostW);lp.rightMargin=0;lp.bottomMargin=0;panel.setLayoutParams(lp);}
 static final class Bar extends View {
  int value=0,scale=100;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  Bar(android.content.Context c){super(c);}
  void set(int v,int s){value=v;scale=Math.max(1,s);invalidate();}
  @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;float r=h/2f;Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),r,r,Path.Direction.CW);c.save();c.clipPath(clip);paint.setColor(LevelHudMetrics.TRACK);c.drawRect(0,0,w,h,paint);float fill=w*Math.max(0,Math.min(scale,value))/(float)scale;float mark=scale>100?w*100f/scale:w;paint.setColor(LevelHudMetrics.FILL);c.drawRect(0,0,Math.min(fill,mark),h,paint);if(fill>mark){paint.setColor(LevelHudMetrics.BOOST);c.drawRect(mark,0,fill,h,paint);}c.restore();}
 }
}
