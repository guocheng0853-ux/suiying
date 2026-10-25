package com.guocheng.mediapocket;
import android.app.Activity;import android.content.res.ColorStateList;import android.graphics.Typeface;import android.graphics.drawable.*;import android.view.*;import android.widget.*;
/** Persistent, non-modal recovery panel below the video, shared by original player engines. */
final class PlaybackFailureCard {
 static final String TAG="mp-playback-failure";
 private PlaybackFailureCard(){}
 static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 static final class Card extends LinearLayout {
  final TextView title,message;final Button again;String last="";
  Card(Activity a,Runnable retry,Runnable choose){super(a);setTag(TAG);setOrientation(VERTICAL);setGravity(Gravity.START);setPadding(dp(a,20),dp(a,20),dp(a,20),dp(a,20));setBackground(PlayerTheme.card(a.getResources().getDisplayMetrics().density));
   title=new TextView(a);title.setText(PlaybackFailureRules.title());title.setTextColor(0xfffaf9f5);title.setTextSize(Ui.SECTION);title.setTypeface(Typeface.DEFAULT_BOLD);title.setIncludeFontPadding(false);if(android.os.Build.VERSION.SDK_INT>=28)title.setAccessibilityHeading(true);addView(title,new LinearLayout.LayoutParams(-1,-2));
   message=new TextView(a);message.setTextColor(PlayerTheme.TEXT_SECONDARY);message.setTextSize(Ui.SMALL);message.setLineSpacing(dp(a,3),1f);message.setIncludeFontPadding(false);LinearLayout.LayoutParams ml=new LinearLayout.LayoutParams(-1,-2);ml.topMargin=dp(a,8);addView(message,ml);
   LinearLayout actions=new LinearLayout(a);actions.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(-1,-2);al.topMargin=dp(a,20);addView(actions,al);
   again=button(a,"重新连接",true,retry);Button source=button(a,"选择片源",false,choose);LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,dp(a,PlaybackFailureRules.TOUCH),1f);first.rightMargin=dp(a,8);actions.addView(again,first);actions.addView(source,new LinearLayout.LayoutParams(0,dp(a,PlaybackFailureRules.TOUCH),1f));
  }
 }
 static Button button(Activity a,String label,boolean primary,Runnable action){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setTextSize(Ui.BODY);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setIncludeFontPadding(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(a,48));b.setMinimumHeight(dp(a,48));b.setPadding(dp(a,12),0,dp(a,12),0);b.setStateListAnimator(null);int fill=primary?PlayerTheme.playbackAccent(CinemaTheme.gold(a)):0xff343430;GradientDrawable face=new GradientDrawable();face.setColor(fill);face.setCornerRadius(dp(a,Ui.R_M));b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x26141413:0x33faf9f5),face,null));b.setTextColor(primary?Ui.onFill(fill):0xfffaf9f5);b.setContentDescription(label);b.setOnClickListener(v->action.run());return b;}
 static void show(Activity a,FrameLayout host,String detail,Runnable retry,Runnable choose){Card c=host.findViewWithTag(TAG);if(c==null){c=new Card(a,retry,choose);host.addView(c,new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL));}
  boolean pending=EmbyAutoRecovery.pending(a);int count=EmbyAutoRecovery.count(a);String description=(detail==null?"":detail.trim())+"|"+pending+"|"+count;if(!description.equals(c.last)){c.last=description;String title=pending?"正在重新连接":PlaybackFailureRules.title(),hint=pending?"正在重新获取播放信息（1/1）…":count>0?"已尝试自动重新连接。可以手动重试，或选择其他片源。":PlaybackFailureRules.hint();c.title.setText(title);c.message.setText(hint);c.again.setText(pending?"取消重连":"重新连接");c.again.setContentDescription(pending?"取消重连":"重新连接");c.setContentDescription(title+"。"+hint);c.announceForAccessibility(title+"。"+hint);}
  position(a,host,c);if(c.getVisibility()!=View.VISIBLE)c.setVisibility(View.VISIBLE);c.bringToFront();
 }
 static void position(Activity a,FrameLayout host,Card c){float density=a.getResources().getDisplayMetrics().density;int[] in=PlayerOverlay.insets(host);int w=host.getWidth()>0?host.getWidth():a.getResources().getDisplayMetrics().widthPixels;int h=host.getHeight()>0?host.getHeight():a.getResources().getDisplayMetrics().heightPixels;int width=dp(a,PlaybackFailureRules.width(Math.round(w/density),Math.round(in[0]/density),Math.round(in[2]/density)));
  c.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));View controls=PlayerOverlay.bar(a,"bottom");int reserve=controls!=null&&controls.getVisibility()==View.VISIBLE?controls.getHeight():0;int bottom=dp(a,PlaybackFailureRules.bottom(Math.round(h/density),Math.round(in[1]/density),Math.round(in[3]/density),Math.round(reserve/density),Math.round(c.getMeasuredHeight()/density)));
  FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)c.getLayoutParams();int left=in[0]+dp(a,PlaybackFailureRules.SIDE),right=in[2]+dp(a,PlaybackFailureRules.SIDE);if(p.width!=width||p.bottomMargin!=bottom||p.leftMargin!=left||p.rightMargin!=right){p.width=width;p.height=-2;p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;p.leftMargin=left;p.rightMargin=right;p.bottomMargin=bottom;c.setLayoutParams(p);}
 }
 static void hide(FrameLayout host){View c=host.findViewWithTag(TAG);if(c!=null&&c.getVisibility()!=View.GONE)c.setVisibility(View.GONE);}
}
