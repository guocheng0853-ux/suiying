package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.widget.FrameLayout;
/**
 * 首页海报页：底部 FADE 比例内把整页内容（图片与底色）渐隐为透明，露出下方同一层氛围背景，
 * 消除海报与下方内容之间的硬边。用 saveLayer 自带离屏层，不依赖 HeroSwipe 动画时切换的 layerType。
 */
final class FadeFrame extends FrameLayout {
 static final float FADE=.38f;
 final Paint mask=new Paint(Paint.ANTI_ALIAS_FLAG);
 FadeFrame(Context c){super(c);mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));setWillNotDraw(false);}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);float top=h*(1-FADE);
  mask.setShader(new LinearGradient(0,top,0,h,new int[]{0xff000000,0x99000000,0x00000000},new float[]{0f,.55f,1f},Shader.TileMode.CLAMP));}
 @Override protected void dispatchDraw(Canvas c){
  int w=getWidth(),h=getHeight();if(w==0||h==0){super.dispatchDraw(c);return;}
  int save=c.saveLayer(0,0,w,h,null);
  super.dispatchDraw(c);
  c.drawRect(0,h*(1-FADE),w,h,mask);
  c.restoreToCount(save);
 }
}
