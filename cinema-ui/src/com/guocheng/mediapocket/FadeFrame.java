package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.widget.FrameLayout;
/**
 * 首页海报页（横版大图，以及影视仓的模糊底 + 竖图）。
 * 底部一段把整页内容的 alpha 收到 0，露出后面同一层氛围背景。
 * 不铺实色：实色不管取成页面底色还是氛围取色，都和「取色 + 模糊海报」不是同一个颜色，底边会留下一条硬线。
 * 遮罩画在 draw() 里，背景和子视图一起被切掉（只在 dispatchDraw 里切会漏掉背景）。
 */
final class FadeFrame extends FrameLayout {
 final Paint mask=new Paint(Paint.ANTI_ALIAS_FLAG);
 int captionTop=-1,fadeSpan=1,maskHeight=-1,overlay;float fadeStart;
 FadeFrame(Context c){super(c);fadeSpan=Math.round(180*c.getResources().getDisplayMetrics().density);setBackgroundColor(0);setWillNotDraw(false);}
 void setCaptionTop(int top,int span){if(top==captionTop&&span==fadeSpan)return;captionTop=top;fadeSpan=span;maskHeight=-1;invalidate();}
 /** 底色到透明的普通渐变，盖在画面上。不再每帧 saveLayer + DST_IN。 */
 void setOverlay(int color){if(color==overlay)return;overlay=color;maskHeight=-1;invalidate();}
 void ensureMask(int h){if(h==maskHeight||h<=0)return;maskHeight=h;
  float end=HomeHeroRules.captionFadeEnd(h,captionTop);
  fadeStart=Math.max(0,end-fadeSpan);
  int solid=overlay==0?0xff000000:overlay;
  int clear=solid&0x00ffffff;
  mask.setShader(new LinearGradient(0,fadeStart,0,Math.max(fadeStart+1,end),new int[]{clear,solid},new float[]{0f,1f},Shader.TileMode.CLAMP));}
 @Override public void draw(Canvas c){
  int w=getWidth(),h=getHeight();if(w==0||h==0){super.draw(c);return;}
  super.draw(c);if(overlay==0)return;ensureMask(h);c.drawRect(0,fadeStart,w,h,mask);
 }
}
