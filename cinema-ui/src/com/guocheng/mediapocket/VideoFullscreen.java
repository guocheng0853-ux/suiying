package com.guocheng.mediapocket;
import android.app.Activity;
import android.os.Build;
import android.view.Gravity;
import java.util.WeakHashMap;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
/**
 * Applies the tablet immersive watch. Phone portrait and phone landscape are not adjusted here:
 * the base player already hides bars in landscape, and phone portrait keeps its letterbox.
 */
final class VideoFullscreen {
 private VideoFullscreen(){}
 static final WeakHashMap<Activity,Integer> SEEN=new WeakHashMap<>();
 /** Hide bars again only when the window size or orientation changes, so a swipe can still reveal them briefly. */
 static void apply(Activity a){try{if(a==null||a.isFinishing())return;int sw=a.getResources().getConfiguration().smallestScreenWidthDp;if(!VideoFullscreenRules.tablet(sw))return;int sig=a.getResources().getConfiguration().orientation*100000+a.getResources().getDisplayMetrics().widthPixels*10000+a.getResources().getDisplayMetrics().heightPixels;if(!Integer.valueOf(sig).equals(SEEN.get(a))){SEEN.put(a,sig);hideBars(a.getWindow());FullscreenMode.immersive(a);}fill(a);}catch(Exception ignored){}}
 static void hideBars(Window w){if(w==null)return;if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(false);WindowInsetsController c=w.getInsetsController();if(c!=null){c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);c.hide(WindowInsets.Type.systemBars());}}View decor=w.getDecorView();decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
 /** Dialog window. Phones return immediately. A tablet fills the pad, including when the activity window is a centered hole. */
 static void dialog(Activity a,Window w){try{if(a==null||w==null)return;int sw=a.getResources().getConfiguration().smallestScreenWidthDp;if(!VideoFullscreenRules.tablet(sw))return;int[] box=bounds(a);boolean multi=Build.VERSION.SDK_INT>=24&&a.isInMultiWindowMode();int[] size=VideoFullscreenRules.dialogWindow(box[0],box[1],box[2],box[3],multi,sw);if(size[0]<=0||size[1]<=0)return;w.setLayout(size[0],size[1]);if(Build.VERSION.SDK_INT>=30)w.setDecorFitsSystemWindows(false);WindowManager.LayoutParams lp=w.getAttributes();lp.width=size[0];lp.height=size[1];lp.gravity=Gravity.TOP|Gravity.START;lp.horizontalMargin=0f;lp.verticalMargin=0f;if(Build.VERSION.SDK_INT>=28)lp.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS:WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;w.setAttributes(lp);hideBars(w);}catch(Exception ignored){}}
 /** screenW, screenH, windowW, windowH. The screen is the whole pad; the window may be the compat hole. */
 static int[] bounds(Activity a){android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics();int screenW=dm.widthPixels,screenH=dm.heightPixels;View decor=a.getWindow()==null?null:a.getWindow().getDecorView();int windowW=decor!=null&&decor.getWidth()>0?decor.getWidth():screenW,windowH=decor!=null&&decor.getHeight()>0?decor.getHeight():screenH;if(Build.VERSION.SDK_INT>=30){try{android.graphics.Rect max=a.getWindowManager().getMaximumWindowMetrics().getBounds();if(max.width()>0&&max.height()>0){screenW=max.width();screenH=max.height();}}catch(Exception ignored){}}return new int[]{screenW,screenH,windowW,windowH};}
 static void fill(Activity a){View frame=view(a,"frame"),video=view(a,"video");boolean changed=match(frame);changed=match(video)||changed;if(a instanceof SystemPlayerActivity){SystemPlayerActivity s=(SystemPlayerActivity)a;changed=match(s.root)||changed;changed=match(s.video)||changed;}if(changed){try{Object videoObj=OnlineSubtitleSearch.field(a,"video");if(videoObj!=null)OnlineSubtitleSearch.call(videoObj,"layoutVideo",new Class<?>[0]);}catch(Exception ignored){}}}
 static View view(Activity a,String name){Object v=OnlineSubtitleSearch.field(a,name);return v instanceof View?(View)v:null;}
 static boolean match(View v){if(v==null||!(v.getLayoutParams() instanceof FrameLayout.LayoutParams))return false;FrameLayout.LayoutParams f=(FrameLayout.LayoutParams)v.getLayoutParams();if(f.width==-1&&f.height==-1&&f.leftMargin==0&&f.topMargin==0&&f.rightMargin==0&&f.bottomMargin==0)return false;f.width=-1;f.height=-1;f.leftMargin=0;f.topMargin=0;f.rightMargin=0;f.bottomMargin=0;v.setLayoutParams(f);return true;}
 /** Media3 chrome. Phones keep the padding set in the constructor. A tablet clears the cutout. */
 static void padMedia3(Activity a,View top,View bottom,View side){try{apply(a);if(a==null||!VideoFullscreenRules.tablet(a.getResources().getConfiguration().smallestScreenWidthDp))return;int[] cut=PlayerOverlay.cutoutInsets(a.getWindow().getDecorView());float d=a.getResources().getDisplayMetrics().density;int sidePx=PlayerLayoutMetrics.sideMargin(d,cut[0],cut[2]);int left=VideoFullscreenRules.clearOf(Math.round(16f*d),sidePx),right=left;int topPad=VideoFullscreenRules.clearOf(Math.round(14f*d),cut[1]);pad(top,left,topPad,right,top==null?0:top.getPaddingBottom());int bottomSide=VideoFullscreenRules.clearOf(Math.round(24f*d),sidePx),bottomPad=VideoFullscreenRules.clearOf(Math.round(12f*d),cut[3]);pad(bottom,bottomSide,bottom==null?0:bottom.getPaddingTop(),bottomSide,bottomPad);if(side!=null&&side.getLayoutParams() instanceof FrameLayout.LayoutParams){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)side.getLayoutParams();int margin=VideoFullscreenRules.clearOf(Math.round(16f*d),sidePx);if(lp.leftMargin!=margin){lp.leftMargin=margin;side.setLayoutParams(lp);}}}catch(Exception ignored){}}
 static void pad(View v,int l,int t,int r,int b){if(v==null)return;if(v.getPaddingLeft()==l&&v.getPaddingTop()==t&&v.getPaddingRight()==r&&v.getPaddingBottom()==b)return;v.setPadding(l,t,r,b);}
}
