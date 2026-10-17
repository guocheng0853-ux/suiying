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
 int captionTop=-1,fadeSpan=1,maskHeight=-1;float fadeStart;
 FadeFrame(Context c){super(c);fadeSpan=Math.round(180*c.getResources().getDisplayMetrics().density);setBackgroundColor(0);mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));setWillNotDraw(false);}
 void setCaptionTop(int top,int span){if(top==captionTop&&span==fadeSpan)return;captionTop=top;fadeSpan=span;maskHeight=-1;invalidate();}
 void ensureMask(int h){if(h==maskHeight||h<=0)return;maskHeight=h;
  float end=HomeHeroRules.captionFadeEnd(h,captionTop);
  fadeStart=Math.max(0,end-fadeSpan);
  mask.setShader(new LinearGradient(0,fadeStart,0,Math.max(fadeStart+1,end),HomeHeroRules.fadeMask(),HomeHeroRules.fadeMaskPositions(),Shader.TileMode.CLAMP));}
 @Override public void draw(Canvas c){
  int w=getWidth(),h=getHeight();if(w==0||h==0){super.draw(c);return;}
  ensureMask(h);int save=c.saveLayer(0,0,w,h,null);super.draw(c);c.drawRect(0,fadeStart,w,h,mask);c.restoreToCount(save);
 }
}
