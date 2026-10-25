package com.guocheng.mediapocket;
import android.view.View;import android.view.accessibility.AccessibilityNodeInfo;
/** Shared Android action semantics for clickable text and grouped rows. */
final class CinemaControls {
 private CinemaControls(){}
 static void action(View v,String label){v.setFocusable(true);v.setContentDescription(label);int touch=Math.round(Ui.TOUCH*v.getResources().getDisplayMetrics().density);v.setMinimumWidth(touch);v.setMinimumHeight(touch);v.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});}
 static void group(View v,String label){action(v,label);if(android.os.Build.VERSION.SDK_INT>=28)v.setScreenReaderFocusable(true);}
}
