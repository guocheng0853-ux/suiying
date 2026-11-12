package com.guocheng.mediapocket;

/**
 * 首页底栏：等宽标签 + 一块会滑动的选中底。
 * 手机是底部胶囊，平板横屏仍是左缘侧轨。不引用 Android，方便 JVM 测位置。
 */
final class BottomNavRules {
 private BottomNavRules(){}

 static final int RAIL_MIN_DP=600;
 static final int PILL_HEIGHT_DP=64;
 static final int PILL_RADIUS_DP=32;
 static final int PILL_PAD_DP=4;
 static final int MARK_INSET_DP=3;
 static final int MARK_RADIUS_DP=18;
 static final int NAV_PAD_H_DP=12;
 static final int NAV_PAD_BOTTOM_DP=8;
 /** 胶囊本身加上一点空隙，滚到底时最后一行停在胶囊上方。没有背后的大渐隐。 */
 static final int PILL_CLEARANCE_DP=PILL_HEIGHT_DP+NAV_PAD_BOTTOM_DP+12;
 /** 键盘打开时只留胶囊本身，不再额外垫一截。 */
 static final int PILL_KEYBOARD_DP=PILL_HEIGHT_DP+NAV_PAD_BOTTOM_DP;
 static final int RAIL_WIDTH_DP=80;
 static final int RAIL_PAD_DP=4;
 static final int RAIL_LEFT_DP=20;
 static final int RAIL_RADIUS_DP=28;
 static final int CONTENT_LEFT_DP=112;
 static final int PAGE_INSET_DP=16;
 static final int DETAIL_BOTTOM_DP=24;
 static final int ICON_DP=24;
 static final int LABEL_SP=12;
 /** 选中块滑动。落在 220–280ms，减速，不回弹。 */
 static final int SLIDE_MS=260;
 static final float EASE_X1=.2f,EASE_Y1=0f,EASE_X2=0f,EASE_Y2=1f;

 static final int NIGHT_PAGE=0xff0b0b0c,NIGHT_SURF=0xff141416,NIGHT_MARK=0xff2b2b2e,NIGHT_MARK_EDGE=0x24f4f1ea,NIGHT_INK=0xfff4f1ea,NIGHT_MUTED=0xffa8a397,NIGHT_EDGE=0x1ff4f1ea,NIGHT_FOCUS=0xffe0b25c,NIGHT_SHADOW=0x66000000;
 static final int DAY_PAGE=0xfffaf9f5,DAY_SURF=0xfffffefa,DAY_MARK=0xffe8e6dc,DAY_MARK_EDGE=0x66a58b74,DAY_INK=0xff141413,DAY_MUTED=0xff45423c,DAY_EDGE=0x33a58b74,DAY_FOCUS=0xffb5573a,DAY_SHADOW=0x243c2814;

 /** 选中块在轨道里的位置。track 是滑动方向的长度，cross 是另一边。 */
 static final class Box {
  final float x,y,w,h;
  Box(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;}
 }
 static Box indicator(boolean vertical,int index,int count,float track,float cross,float pad,float inset){
  int n=count<=0?1:count,i=keptIndex(index,n);
  float inner=Math.max(0f,track-pad*2f),slot=inner/n;
  float along=pad+i*slot+inset,size=Math.max(0f,slot-inset*2f);
  float crossPos=pad+inset,crossSize=Math.max(0f,cross-pad*2f-inset*2f);
  return vertical?new Box(crossPos,along,crossSize,size):new Box(along,crossPos,size,crossSize);
 }
 static Box lerp(Box a,Box b,float t){
  float u=t<0f?0f:t>1f?1f:t;
  if(a==null)return b;if(b==null)return a;
  return new Box(a.x+(b.x-a.x)*u,a.y+(b.y-a.y)*u,a.w+(b.w-a.w)*u,a.h+(b.h-a.h)*u);
 }
 /** 宽或高还没量出来时不要画选中块，避免先闪出 1px。 */
 static boolean markVisible(Box box){return box!=null&&box.w>=1f&&box.h>=1f;}
 /**
  * 选中块的滑动状态。连点改道时 from 取当前 shown，不回到上一段的起点或终点。
  * settle 直接落到当前目标。motion 为 false 时不播，立刻到目标。
  */
 static final class Slide {
  final Box from,to,shown;
  final float progress;
  final boolean running;
  Slide(Box from,Box to,Box shown,float progress,boolean running){this.from=from;this.to=to;this.shown=shown;this.progress=progress;this.running=running;}
  static Slide resting(Box at){return new Slide(at,at,at,1f,false);}
  Slide play(Box target,boolean motion){
   if(target==null)return this;
   if(!motion)return resting(target);
   Box start=shown!=null?shown:target;
   return new Slide(start,target,start,0f,true);
  }
  Slide advance(float t){
   if(!running||from==null||to==null)return this;
   float u=t<0f?0f:t>1f?1f:t;
   if(u>=1f)return new Slide(from,to,to,1f,false);
   return new Slide(from,to,lerp(from,to,u),u,true);
  }
  /** 从当前已经画出的位置改去新目标。 */
  Slide retarget(Box target,boolean motion){
   if(target==null)return this;
   Box start=shown!=null?shown:target;
   if(!motion)return resting(target);
   return new Slide(start,target,start,0f,true);
  }
  Slide settle(){return resting(to!=null?to:shown);}
 }
 /**
  * select 的决定。move 为 false（去掉动画，或 animate=false）时停在新槽，
  * 不要 settle 回上一段的目标。正在滑时改道从当前 shown 出发。
  */
 static Slide selectMark(Slide current,Box nextSlot,boolean move){
  Slide base=current==null?Slide.resting(null):current;
  if(!move)return base.play(nextSlot,false);
  if(base.running)return base.retarget(nextSlot,true);
  return base.play(nextSlot,true);
 }
 static int railRightDp(){return RAIL_LEFT_DP+RAIL_WIDTH_DP;}
 /** 海报行的屏幕左边。侧轨时不小于侧轨右缘。 */
 static int contentScreenLeftDp(boolean rail,int safeLeftDp){return safeLeftDp+contentLeftDp(rail);}
 /**
  * 英雄图的 leftMargin。侧轨是浮在图上的，图必须回到屏幕左缘，否则左边会露出另一张海报。
  * 横滑行不走这条，它们用 rowStartPadding。
  */
 static int fullBleedLeftMargin(int paddingLeftPx,int railRightPx,boolean rail){return -paddingLeftPx;}
 static int heroScreenLeft(int paddingLeftPx){return paddingLeftPx+fullBleedLeftMargin(paddingLeftPx,0,true);}
 /** 横滑容器可以伸到侧轨底下，静止时第一张从侧轨右缘开始。 */
 static int rowStartPadding(int scrollerScreenLeft,int railRight,boolean rail){return rail?Math.max(0,railRight-scrollerScreenLeft):0;}
 static int rowLeftMargin(int bodyPaddingLeft,int scrollerScreenLeft){return scrollerScreenLeft-bodyPaddingLeft;}
 /** 没有侧轨时边距和起始内边距都回到 0，不能留下横屏时的负边距。 */
 static int rowLeftMarginFor(boolean rail,int bodyPaddingLeft,int scrollerScreenLeft){return rail?rowLeftMargin(bodyPaddingLeft,scrollerScreenLeft):0;}
 static int rowStartPaddingFor(boolean rail,int scrollerScreenLeft,int railRight){return rowStartPadding(scrollerScreenLeft,railRight,rail);}
 static int rowRestScreenLeft(int scrollerScreenLeft,int railRight,boolean rail){return scrollerScreenLeft+rowStartPadding(scrollerScreenLeft,railRight,rail);}
 /** 夜间侧轨 75% 表面色。日间压在亮海报上要更实，标签才过 4.5:1。 */
 static final int RAIL_SCRIM_ALPHA=0xC0,RAIL_SCRIM_ALPHA_LIGHT=0xE6;
 static int railScrimAlpha(boolean day){return day?RAIL_SCRIM_ALPHA_LIGHT:RAIL_SCRIM_ALPHA;}
 static int railSurface(boolean day){return (railScrimAlpha(day)<<24)|(surface(day)&0xffffff);}
 /** 把带透明度的侧轨底叠到海报上，用来测最亮底上的字。 */
 static int composite(int fg,int bg){
  int a=(fg>>>24)&255;if(a>=255)return 0xff000000|(fg&0xffffff);
  int inv=255-a;
  int r=(((fg>>>16)&255)*a+((bg>>>16)&255)*inv)/255;
  int g=(((fg>>>8)&255)*a+((bg>>>8)&255)*inv)/255;
  int b=((fg&255)*a+(bg&255)*inv)/255;
  return 0xff000000|(r<<16)|(g<<8)|b;
 }
 /**
  * 手机胶囊整格可点。侧轨才把热区收成居中的 48dp，格子四角让给底下的海报。
  */
 static boolean insideHit(boolean rail,float x,float y,float viewW,float viewH,float minPx){
  if(viewW<=0f||viewH<=0f)return false;
  if(!rail)return x>=0f&&y>=0f&&x<=viewW&&y<=viewH;
  if(viewW<minPx||viewH<minPx)return x>=0f&&y>=0f&&x<=viewW&&y<=viewH;
  float left=(viewW-minPx)/2f,top=(viewH-minPx)/2f;
  return x>=left&&x<=left+minPx&&y>=top&&y<=top+minPx;
 }
 static boolean useRail(int windowWidthDp,int windowHeightDp,int smallestWidthDp){
  if(windowWidthDp<=windowHeightDp)return false;
  if(windowWidthDp<RAIL_MIN_DP)return false;
  if(smallestWidthDp>=RAIL_MIN_DP)return true;
  return smallestWidthDp<=0&&windowHeightDp>=RAIL_MIN_DP;
 }
 static int keptIndex(int index,int count){if(count<=0)return 0;if(index<0||index>=count)return 0;return index;}
 /** 每一项都显示文字，不再把未选中项收成图标。 */
 static boolean labelVisible(boolean rail,boolean selected){return true;}
 static int labelSp(boolean rail){return LABEL_SP;}
 static int contentLeftDp(boolean railVisible){return railVisible?Math.max(CONTENT_LEFT_DP,railRightDp()):PAGE_INSET_DP;}
 static int contentBottomDp(boolean railVisible,boolean detail){return contentBottomDp(railVisible,detail,false);}
 static int contentBottomDp(boolean railVisible,boolean detail,boolean keyboard){
  if(detail)return DETAIL_BOTTOM_DP;
  if(railVisible)return keyboard?8:DETAIL_BOTTOM_DP;
  return keyboard?PILL_KEYBOARD_DP:PILL_CLEARANCE_DP;
 }
 /** 导航栏和输入法叠在同一条底边上，取较高的那个，不要相加。 */
 static int bottomObstacle(int barBottom,int imeBottom){return Math.max(0,Math.max(barBottom,imeBottom));}
 static int pillBottomMarginDp(int safeBottomDp){return safeBottomDp+NAV_PAD_BOTTOM_DP;}
 static int railLeftDp(int safeLeftDp){return safeLeftDp+RAIL_LEFT_DP;}
 static int railTop(int parentHeight,int railHeight,int safeTop,int safeBottom){
  int space=parentHeight-safeTop-safeBottom;
  if(space<=railHeight)return safeTop;
  return safeTop+(space-railHeight)/2;
 }
 static int pillItemMinDp(){return PILL_HEIGHT_DP-2*PILL_PAD_DP;}
 static int railItemWidthDp(){return RAIL_WIDTH_DP-2*RAIL_PAD_DP;}
 static int railItemMinDp(){return 48;}
 static int page(boolean day){return day?DAY_PAGE:NIGHT_PAGE;}
 static int surface(boolean day){return day?DAY_SURF:NIGHT_SURF;}
 static int mark(boolean day){return day?DAY_MARK:NIGHT_MARK;}
 static int markEdge(boolean day){return day?DAY_MARK_EDGE:NIGHT_MARK_EDGE;}
 static int ink(boolean day){return day?DAY_INK:NIGHT_INK;}
 static int muted(boolean day){return day?DAY_MUTED:NIGHT_MUTED;}
 static int edge(boolean day){return day?DAY_EDGE:NIGHT_EDGE;}
 static int focus(boolean day){return day?DAY_FOCUS:NIGHT_FOCUS;}
 static int shadow(boolean day){return day?DAY_SHADOW:NIGHT_SHADOW;}
 /** 电视蓝框。首页导航自己有一圈，这里排除掉。 */
 static boolean drawsTvFocusRing(boolean touchMode,boolean textField,int width,boolean insideHomeNav){return !touchMode&&!textField&&width>0&&!insideHomeNav;}
 static boolean motionEnabled(float animatorScale,float transitionScale,float windowScale){
  return animatorScale>0f&&transitionScale>0f&&windowScale>0f;
 }
 static int slideMs(boolean motion){return motion?SLIDE_MS:0;}
}
