package com.guocheng.mediapocket;
import android.content.Context;import android.content.res.ColorStateList;import android.widget.Button;
final class CinemaButton extends Button {
 final boolean light;
 CinemaButton(Context c,boolean light){super(c);this.light=light;setAllCaps(false);setTextSize(Ui.sp(13));setIncludeFontPadding(false);setStateListAnimator(null);int touch=Math.round(Ui.TOUCH*c.getResources().getDisplayMetrics().density);setMinHeight(touch);setMinimumHeight(touch);setMinWidth(touch);setMinimumWidth(touch);}
 @Override public void setBackgroundTintList(ColorStateList list){if(list==null){super.setBackgroundTintList(null);return;}int color=list.getDefaultColor();if(color==0xff8d68f5||color==0xff6844ce)color=0xff3c819b;else if(color==0xff1b1b29||color==0xff11111c||color==0xffe9ebf3)color=light?0xffffffff:0xff19434e;super.setBackgroundTintList(ColorStateList.valueOf(color));setTextColor(Ui.onFill(color));}
 @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);setAlpha(enabled?1f:.48f);}
}
