package com.guocheng.mediapocket;
import android.content.Context;import android.content.res.ColorStateList;import android.graphics.Color;import android.widget.Button;
final class CinemaButton extends Button {
 final boolean light;
 CinemaButton(Context c,boolean light){super(c);this.light=light;setAllCaps(false);setTextSize(13);setIncludeFontPadding(false);setStateListAnimator(null);}
 @Override public void setBackgroundTintList(ColorStateList list){if(list==null){super.setBackgroundTintList(null);return;}int color=list.getDefaultColor();if(color==0xff8d68f5||color==0xff6844ce)color=0xff3c819b;else if(color==0xff1b1b29||color==0xff11111c||color==0xffe9ebf3)color=light?0xffffffff:0xff19434e;super.setBackgroundTintList(ColorStateList.valueOf(color));setTextColor(CinemaTheme.clay(getContext())?(Color.luminance(color)>0.45?CinemaTheme.SLATE:CinemaTheme.PAPER):(Color.luminance(color)>0.45?0xff173740:0xfff6fbfc));}
}
