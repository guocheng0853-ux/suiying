package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.widget.FrameLayout;
/**
 * Fade artwork (including portrait decorations) into the SAME backdrop as the shelves.
 * Mask only this page's offscreen layer: never erase the parent or the captions.
 * No opaque colour overlay: the backdrop includes texture, not just its average tint.
 */
final class FadeFrame extends FrameLayout {
 private final Paint mask=new Paint(Paint.ANTI_ALIAS_FLAG);
 private boolean portrait;
 FadeFrame(Context c){super(c);setBackgroundColor(0);}
 void setPortraitFade(boolean value){if(portrait!=value){portrait=value;updateMask();invalidate();}}
 private void updateMask(){if(getHeight()<=0)return;mask.setShader(new LinearGradient(0,0,0,getHeight(),HomeHeroRules.artworkMask(),HomeHeroRules.fadePositions(portrait),Shader.TileMode.CLAMP));mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);updateMask();}
 @Override protected void dispatchDraw(Canvas canvas){
  if(getWidth()<=0||getHeight()<=0)return;
  int saved=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
  super.dispatchDraw(canvas);
  canvas.drawRect(0,0,getWidth(),getHeight(),mask);
  canvas.restoreToCount(saved);
 }
}
