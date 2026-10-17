package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.widget.FrameLayout;
/**
 * 首页海报页（横版大图，以及影视仓的模糊底 + 竖图）。
 * 底部一段把整页内容的 alpha 收到 0，露出后面同一层氛围背景。
 * 不铺实色：实色不管取成页面底色还是氛围取色，都和「取色 + 模糊海报」不是同一个颜色，底边会留下一条硬线。
 * 遮罩画在 draw() 里，背景和子视图一起被切掉（只在 dispatchDraw 里切会漏掉背景）。
 */
final class FadeFrame extends FrameLayout {
 final Paint mask=new Paint(Paint.ANTI_ALIAS_FLAG);int maskHeight=-1;
 FadeFrame(Context c){super(c);setBackgroundColor(0);mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));setWillNotDraw(false);}
 void ensureMask(int h){if(h==maskHeight||h<=0)return;maskHeight=h;float top=h*(1f-HomeHeroRules.FADE);float end=Math.max(top+1f,h-1f);
  mask.setShader(new LinearGradient(0,top,0,end,HomeHeroRules.fadeMask(),HomeHeroRules.fadeMaskPositions(),Shader.TileMode.CLAMP));}
 @Override public void draw(Canvas c){
  int w=getWidth(),h=getHeight();if(w==0||h==0){super.draw(c);return;}
  ensureMask(h);int save=c.saveLayer(0,0,w,h,null);super.draw(c);c.drawRect(0,h*(1f-HomeHeroRules.FADE),w,h,mask);c.restoreToCount(save);
 }
}
