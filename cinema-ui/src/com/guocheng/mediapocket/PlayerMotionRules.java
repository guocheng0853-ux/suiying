package com.guocheng.mediapocket;
/**
 * 播放页动效时长，全部收敛到这里（0.1.83 Hills 基准）。
 * 显隐 280/260，侧面板 220/180，菜单 180 出 150，控件超时 4 秒。
 * 纯常量，JVM 可测，不含 Android 类。
 */
final class PlayerMotionRules {
 static final int CHROME_IN_MS=280,CHROME_OUT_MS=260,CONTROLS_TIMEOUT_MS=4000;
 static final int PANEL_IN_MS=220,PANEL_OUT_MS=180;
 static final int MENU_DIM_MS=180,MENU_OUT_MS=150;
 static final int MENU_ROW_DP=48,MENU_TITLE_SP=15,MENU_LABEL_SP=15,MENU_VALUE_SP=13;
 private PlayerMotionRules(){}
}
