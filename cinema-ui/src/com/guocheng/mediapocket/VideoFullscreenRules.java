package com.guocheng.mediapocket;
/**
 * Tablet video fullscreen. Shortest side ≥ 600dp is a tablet, the same cutoff as sw600dp.
 * A tablet watch uses the phone-landscape immersive path even when the system will not rotate:
 * hide the bars, fill the window, and keep controls clear of the cutout.
 * Phone portrait still letterboxes under a reserved status bar. Phone landscape is unchanged.
 */
final class VideoFullscreenRules {
 static final int TABLET_SW=600;
 private VideoFullscreenRules(){}
 static boolean tablet(int smallestWidthDp){return smallestWidthDp>=TABLET_SW;}
 /** Phone landscape, and every tablet, watch immersively. */
 static boolean immersive(int smallestWidthDp,boolean landscape){return tablet(smallestWidthDp)||landscape;}
 /** Phone portrait keeps a status-bar band. Tablets and landscape do not. */
 static boolean reserveStatusBar(int smallestWidthDp,boolean portrait){return portrait&&!tablet(smallestWidthDp);}
 /**
  * Video stage in window pixels: x, y, width, height.
  * Immersive fills the window. Phone portrait letterboxes the picture under the status bar.
  */
 static int[] stage(int windowW,int windowH,int statusTop,int navBottom,int smallestWidthDp,boolean landscape,int videoW,int videoH){
  if(windowW<=0||windowH<=0)return new int[]{0,0,0,0};
  if(immersive(smallestWidthDp,landscape))return new int[]{0,0,windowW,windowH};
  int top=Math.max(0,statusTop),bottom=Math.max(0,navBottom);
  if(top+bottom>=windowH){top=0;bottom=0;}
  int availW=windowW,availH=Math.max(1,windowH-top-bottom);
  double ratio=videoW>0&&videoH>0?(double)videoW/videoH:16.0/9.0;
  double w=availW,h=w/ratio;
  if(h>availH){h=availH;w=h*ratio;}
  int iw=Math.max(1,(int)Math.round(w)),ih=Math.max(1,(int)Math.round(h));
  if(iw>availW)iw=availW;
  int x=Math.max(0,(availW-iw)/2),y=top+Math.max(0,(availH-ih)/2);
  return new int[]{x,y,iw,ih};
 }
 /** One edge of chrome padding. Phone portrait keeps the system bar; immersive uses the cutout only. */
 static int chromeEdge(int barAndCutout,int cutoutOnly,int smallestWidthDp,boolean portrait){
  if(reserveStatusBar(smallestWidthDp,portrait))return Math.max(0,barAndCutout);
  return Math.max(0,cutoutOnly);
 }
 /**
  * Fullscreen dialog window size. A tablet fills the pad (or the split window).
  * A phone returns -1,-1 so the existing match-parent dialog path is left alone.
  */
 static int[] dialogWindow(int screenW,int screenH,int windowW,int windowH,boolean multi,int smallestWidthDp){
  if(!tablet(smallestWidthDp))return new int[]{-1,-1};
  if(multi)return new int[]{Math.max(0,windowW>0?windowW:screenW),Math.max(0,windowH>0?windowH:screenH)};
  return new int[]{Math.max(Math.max(0,screenW),Math.max(0,windowW)),Math.max(Math.max(0,screenH),Math.max(0,windowH))};
 }
 /** -1 = unspecified. A tablet does not force a rotation the system may turn into a centered hole. */
 static int orientation(boolean multi,int smallestWidthDp,int desired){if(multi||tablet(smallestWidthDp))return -1;return desired;}
 /** Controls stay on their existing pad, and move further in when the cutout is deeper. */
 static int clearOf(int basePx,int cutoutPx){return Math.max(Math.max(0,basePx),Math.max(0,cutoutPx));}
}
