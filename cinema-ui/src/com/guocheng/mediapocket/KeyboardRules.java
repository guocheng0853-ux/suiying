package com.guocheng.mediapocket;
/**
 * 0.1.169 平板外接键盘的方向键（纯逻辑）：上下 = 音量，左右 = 快退 / 快进。
 * 电视遥控器仍按原来的规则：控制栏收起时上下呼出控制栏，打开时方向键在按钮间移动焦点。
 */
final class KeyboardRules {
 private KeyboardRules(){}
 static final int NONE=0,VOLUME_UP=1,VOLUME_DOWN=2,BACK=3,FORWARD=4;
 /** 带字母键的实体键盘，或有触屏的设备（手机、平板）接的键盘，算「键盘」；没有触屏的电视盒子算遥控器。 */
 static boolean keyboard(boolean alphabetic,boolean touchscreen){return alphabetic||touchscreen;}
 /** 19/20/21/22 = DPAD_UP/DOWN/LEFT/RIGHT。侧栏或锁屏时不接管。 */
 static int action(int keyCode,boolean keyboard,boolean panelOpen,boolean locked){
  if(!keyboard||panelOpen||locked)return NONE;
  switch(keyCode){case 19:return VOLUME_UP;case 20:return VOLUME_DOWN;case 21:return BACK;case 22:return FORWARD;default:return NONE;}
 }
 /** 按住时加速：按住一会儿后每次跳 30 秒。 */
 static int seekStepMs(int baseMs,int repeat){return repeat>8?Math.max(baseMs,30000):Math.max(1000,baseMs);}
}
