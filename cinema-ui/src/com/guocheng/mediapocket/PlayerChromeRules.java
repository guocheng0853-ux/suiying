package com.guocheng.mediapocket;
/** 底栏倍速按钮的识别。只用于底栏那一行，不要拿去藏画面右侧的倍速按钮。 */
final class PlayerChromeRules {
 private PlayerChromeRules(){}
 /**
  * 基础播放器 GesturePlayerActivity.btn("播放速度")：
  * 画在按钮上的字改成「倍速」，随后又把 contentDescription 设回原来的「播放速度」。
  * 只比对说明文字「倍速」会漏掉这颗仍显示在底栏的按钮。
  */
 static boolean legacySpeed(String text,String description){return is(text,"倍速")||is(text,"播放速度")||is(description,"倍速")||is(description,"播放速度");}
 static boolean is(String value,String expected){return expected.equals(value==null?"":value);}
}
