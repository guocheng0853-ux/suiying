package com.guocheng.mediapocket;
import android.graphics.drawable.GradientDrawable;import android.app.Activity;
/** Warm charcoal playback surfaces with paper-white text and a clay accent.
 * Paper-theme menus outside playback use the corresponding light palette.
 * Video controls keep a dark surface regardless of the page appearance.
 */
final class PlayerTheme {
 static final int PANEL=0xF5141413;
 static final int PANEL_SOFT=0xE6252522;
 static final int CARD=0xF0272724;
 static final int PILL_BG=0xE62C2723;
 static final int ACCENT=0xffe6a58d;
 static final int ACCENT_DIM=0x33d97757;
 static final int SCRIM=0x66000000;
 static final int TEXT_SECONDARY=0xffb0aea5;
 static final int TRACK=0x33FFFFFF,TRACK_BUFFER=0x66FFFFFF,TRACK_PLAYED=0xffe6a58d;
 static final int MARK_INTRO=0xcc6eb6e0,MARK_CREDITS=0xccd0bfff,MARK_CHAPTER=0xeeffffff;
 static final int RADIUS_PANEL_DP=20,RADIUS_CARD_DP=14,RADIUS_MENU_ROW_DP=12,RADIUS_HUD_DP=12;
 static final int MENU_WIDTH_MAX_DP=360;
 static final int FADE_TOP=0x99000000,FADE_BOTTOM=0xC0000000;
 private PlayerTheme(){}
 static boolean paper(Activity a){return CinemaTheme.clay(a)&&!(a instanceof PlayerActivity)&&!(a instanceof SystemPlayerActivity);}
 static int surface(Activity a){return paper(a)?CinemaTheme.PAPER:PANEL;}
 static int text(Activity a){return paper(a)?CinemaTheme.SLATE:0xfffaf9f5;}
 static int secondary(Activity a){return paper(a)?CinemaTheme.STONE:TEXT_SECONDARY;}
 /** 播放页强调色。影院暗金外观用 #e0b25c，暖白陶土和其他页面仍用暖陶。 */
 static int playbackAccent(boolean gold){return PlayerGlassRules.playbackAccent(gold);}
 static int playbackAccentDim(boolean gold){return gold?0x33e0b25c:ACCENT_DIM;}
 static int playbackAccent(Activity a){return playbackAccent(a instanceof PlayerActivity||a instanceof SystemPlayerActivity?CinemaTheme.gold(a):false);}
 static int selectedText(Activity a){return paper(a)?CinemaTheme.CLAY_DEEP:playbackAccent(a);}
 static int selection(Activity a){return paper(a)?CinemaTheme.CLAY_CHIP:(a instanceof PlayerActivity||a instanceof SystemPlayerActivity?playbackAccentDim(CinemaTheme.gold(a)):ACCENT_DIM);}
 static int ripple(Activity a){return paper(a)?0x22141413:0x33faf9f5;}

 static int rowText(boolean selected){return selected?ACCENT:0xfffaf9f5;}
 static int rowText(boolean selected,boolean gold){return selected?playbackAccent(gold):0xfffaf9f5;}
 static int rowFill(boolean selected){return selected?ACCENT_DIM:0x00000000;}
 static int rowFill(boolean selected,boolean gold){return selected?playbackAccentDim(gold):0x00000000;}
 static int chipText(boolean on){return on?CinemaTheme.SLATE:0xfffaf9f5;}
 static int chipFill(boolean on){return on?ACCENT:0x00000000;}
 static int chipFill(boolean on,boolean gold){return on?playbackAccent(gold):0x00000000;}
 static int note(){return TEXT_SECONDARY;}
 static GradientDrawable panel(float density,int radiusDp){GradientDrawable d=new GradientDrawable();d.setColor(PANEL);d.setCornerRadius(Math.max(1,Math.round(radiusDp*density)));return d;}
 static GradientDrawable card(float density){GradientDrawable d=new GradientDrawable();d.setColor(CARD);d.setCornerRadius(Math.max(1,Math.round(RADIUS_CARD_DP*density)));return d;}
}
