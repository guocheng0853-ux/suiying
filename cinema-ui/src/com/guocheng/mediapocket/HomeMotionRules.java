package com.guocheng.mediapocket;
/** Pure motion math for the home scene: app-bar scrim, hero parallax, caption motion, edge rubber-band and menu timing. */
final class HomeMotionRules {
 static final float PARALLAX=0.5f;static final int MENU_MS=180;static final int MENU_DIM=0x80;
 static float clamp(float v){return Math.max(0,Math.min(1,v));}
 /** 0 while the hero fills the bar, ramps to 1 as the hero bottom passes under the bar. */
 static float scrimAlpha(int scrollY,int heroHeight,int barHeight){if(heroHeight<=0)return scrollY>0?1:0;int end=Math.max(1,heroHeight-barHeight),start=Math.max(0,end-Math.max(barHeight,1)*2);if(scrollY<=start)return 0;if(scrollY>=end)return 1;return clamp((scrollY-start)/(float)(end-start));}
 /** Gradient over the hero is strongest at rest and gives way to the solid bar. */
 static float gradientAlpha(float scrim){return 1-clamp(scrim);}
 /** Image inside a page lags the page by (1-PARALLAX): net movement = offset*PARALLAX. */
 static float imageCounterShift(float pageOffset){return -pageOffset*(1-PARALLAX);}
 /** Outgoing caption drifts at half speed and fades; incoming caption slides in from a quarter width. */
 static float outgoingText(float offset){return offset*PARALLAX;}
 static float incomingText(float offset,int width){if(width<=0)return 0;float p=clamp(Math.abs(offset)/width);float dir=offset<0?1:-1;return dir*width*.25f*(1-p);}
 /** Resistance when there is nothing to page to (single hero). */
 static float rubberBand(float drag,int width){if(width<=0)return 0;float limit=width*.18f,x=Math.abs(drag);float r=limit*(1-1/(x/limit*.55f+1));return drag<0?-r:r;}
 static float overshootTension(float velocity,int minVelocity){return Math.abs(velocity)>=minVelocity?1.2f:.8f;}
 /** 回弹越过目标的量：只有越界方向没有相邻页时才截住（返回需从页面位移中扣除的量）。direction 为动画方向，0 表示未在回弹。 */
 static float overshoot(float value,float target,int direction,boolean hasPrevious,boolean hasNext){
  if(direction==0)return 0;
  if(direction<0&&value<target&&!(target==0&&hasNext))return value-target;
  if(direction>0&&value>target&&!(target==0&&hasPrevious))return value-target;
  return 0;}
 /** 被截住的冲量交给图片在框内移动，最多 3% 屏宽。 */
 static float absorb(float over,int width){if(width<=0)return 0;float max=width*.03f;return Math.max(-max,Math.min(max,over*.5f));}
 /** 图片缩放随位移连续变化（原来 0 处在 1.0/1.04 间跳变）；有框内位移时放大到足以盖住两侧。 */
 static float pageScale(float pageOffset,float bounce,int width){if(width<=0)return 1;float drag=1+.04f*clamp(Math.abs(pageOffset)/(width*.15f));float cover=1+2*Math.abs(bounce)/width+.002f;return Math.max(drag,bounce==0?1:cover);}
 static int dimAlpha(float progress){return Math.round(MENU_DIM*clamp(progress));}
 static float menuScale(float progress){return .9f+.1f*clamp(progress);}
}
