package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.os.Build;import android.view.*;import android.widget.ScrollView;
/**
 * 毛玻璃顶栏（Android 12+）：把顶栏正下方的页面内容按 1/SCALE 画进小位图，再用 RenderEffect 模糊；
 * 只在页面（含横向书架）滚动时重画，每帧最多一次。低于 Android 12 或截取失败（如硬件位图）时自动隐藏，
 * 由原来的半透明深色底栏兜底。
 */
final class FrostedBar extends View {
 static final int SCALE=4;
 final ScrollView scroll;Bitmap bitmap;Canvas canvas;final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);final Rect dest=new Rect();
 final int[] mine=new int[2],theirs=new int[2];boolean failed,pending;int base;
 final ViewTreeObserver.OnScrollChangedListener onScroll=this::request;
 static boolean supported(){return Build.VERSION.SDK_INT>=31;}
 FrostedBar(Context c,ScrollView scroll,int radiusPx){
  super(c);this.scroll=scroll;
  if(supported())setRenderEffect(RenderEffect.createBlurEffect(radiusPx,radiusPx,Shader.TileMode.CLAMP));else{failed=true;setVisibility(GONE);}
 }
 boolean active(){return !failed&&supported();}
 void setBase(int color){base=color|0xff000000;}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnScrollChangedListener(onScroll);}
 @Override protected void onDetachedFromWindow(){getViewTreeObserver().removeOnScrollChangedListener(onScroll);bitmap=null;canvas=null;super.onDetachedFromWindow();}
 /** 合并到下一帧再截取，避免一帧内多次滚动回调重复绘制。 */
 void request(){if(failed||pending||getAlpha()<.01f||getWidth()==0||getHeight()==0)return;pending=true;postOnAnimation(()->{pending=false;capture();});}
 void capture(){
  if(failed||scroll.getChildCount()==0)return;View content=scroll.getChildAt(0);
  int w=Math.max(1,getWidth()/SCALE),h=Math.max(1,getHeight()/SCALE);
  if(bitmap==null||bitmap.getWidth()!=w||bitmap.getHeight()!=h){bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);canvas=new Canvas(bitmap);}
  bitmap.eraseColor(base);
  getLocationInWindow(mine);content.getLocationInWindow(theirs);
  int save=canvas.save();
  try{canvas.scale(1f/SCALE,1f/SCALE);canvas.translate(theirs[0]-mine[0],theirs[1]-mine[1]);content.draw(canvas);}
  catch(Throwable t){failed=true;bitmap=null;canvas=null;setVisibility(GONE);if(fallback!=null)fallback.run();return;}
  finally{if(canvas!=null)canvas.restoreToCount(save);}
  invalidate();
 }
 Runnable fallback;
 @Override protected void onDraw(Canvas c){if(bitmap==null)return;dest.set(0,0,getWidth(),getHeight());c.drawBitmap(bitmap,null,dest,paint);}
}
