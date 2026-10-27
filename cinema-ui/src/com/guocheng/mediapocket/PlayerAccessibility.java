package com.guocheng.mediapocket;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

/** Keep playback actions available while a screen reader explores the controls. */
final class PlayerAccessibility {
 private PlayerAccessibility(){}
 static boolean exploring(Context context){AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);return manager!=null&&manager.isEnabled()&&manager.isTouchExplorationEnabled();}
 static boolean retainNative(Activity activity){
  if(!exploring(activity)||activity.isFinishing()||activity.isInPictureInPictureMode()||PlayerGestures.locked(activity))return false;
  Object handler=OnlineSubtitleSearch.field(activity,"handler"),hide=OnlineSubtitleSearch.field(activity,"hideControls");
  if(handler instanceof Handler&&hide instanceof Runnable)((Handler)handler).removeCallbacks((Runnable)hide);
  View top=PlayerOverlay.bar(activity,"top");
  if(top!=null&&top.getVisibility()!=View.VISIBLE&&!CinemaMenu.isOpen(activity))try{OnlineSubtitleSearch.call(activity,"controls",new Class<?>[]{boolean.class},true);}catch(Exception ignored){}
  return true;
 }
}
