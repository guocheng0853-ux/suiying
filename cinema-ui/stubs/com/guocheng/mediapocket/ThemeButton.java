package com.guocheng.mediapocket;
import android.content.Context;import android.content.res.ColorStateList;import android.widget.Button;
/** Keeps the ink readable even when legacy screens replace a button's tint. */
public final class ThemeButton extends Button {
 final boolean light;
 public ThemeButton(Context context,boolean light){super(context);this.light=light;setAllCaps(false);setIncludeFontPadding(false);setTextSize(14);setStateListAnimator(null);}
 @Override public void setBackgroundTintList(ColorStateList tint){if(tint==null){super.setBackgroundTintList(null);return;}int color=tint.getDefaultColor();boolean selected=color==0xff8d68f5||color==0xff6844ce;if(color==0xff1b1b29||color==0xff11111c||color==0xffe9ebf3)color=light?0xffe9ebf3:0xff1b1b29;else if(selected)color=light?0xff6844ce:0xff8d68f5;super.setBackgroundTintList(ColorStateList.valueOf(color));setTextColor(selected?(light?0xffffffff:0xff0b0b12):(light?0xff202131:0xfff4f3fa));}
}
