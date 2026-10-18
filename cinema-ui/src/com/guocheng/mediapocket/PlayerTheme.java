package com.guocheng.mediapocket;
import android.graphics.drawable.GradientDrawable;
/**
 * Hills 语言的统一颜色与形状 token（0.1.83 真机截图基准）。
 * 深色面板只有两种：PANEL 是侧面板/菜单底，PANEL_SOFT 是短暂提示 HUD；
 * 强调色只有 ACCENT 一种淡青。圆角：面板 20、卡片 14、菜单行 12。
 * 所有深色 UI 字面量都从这里取，不许再散落。
 */
final class PlayerTheme {
 static final int PANEL=0xF0121418;
 static final int PANEL_SOFT=0xCC1C1B22;
 static final int CARD=0xF0101820;
 static final int PILL_BG=0xE61A2C50;
 static final int ACCENT=0xffd7e7f5;
 static final int ACCENT_DIM=0x33d7e7f5;
 static final int SCRIM=0x66000000;
 static final int TEXT_SECONDARY=0x99FFFFFF;
 static final int TRACK=0x33FFFFFF,TRACK_BUFFER=0x66FFFFFF,TRACK_PLAYED=0xFFFFFFFF;
 static final int MARK_INTRO=0xcc6eb6e0,MARK_CREDITS=0xccd0bfff,MARK_CHAPTER=0xeeffffff;
 static final int RADIUS_PANEL_DP=20,RADIUS_CARD_DP=14,RADIUS_MENU_ROW_DP=12,RADIUS_HUD_DP=12;
 static final int MENU_WIDTH_MAX_DP=360;
 static final int FADE_TOP=0x99000000,FADE_BOTTOM=0xC0000000;
 private PlayerTheme(){}
 static GradientDrawable panel(float density,int radiusDp){GradientDrawable d=new GradientDrawable();d.setColor(PANEL);d.setCornerRadius(Math.max(1,Math.round(radiusDp*density)));return d;}
 static GradientDrawable card(float density){GradientDrawable d=new GradientDrawable();d.setColor(CARD);d.setCornerRadius(Math.max(1,Math.round(RADIUS_CARD_DP*density)));return d;}
}
