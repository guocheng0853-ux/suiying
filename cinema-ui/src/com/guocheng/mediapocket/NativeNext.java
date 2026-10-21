package com.guocheng.mediapocket;
import android.os.SystemClock;import android.view.*;import android.widget.*;
/** Native player owns its next episode handoff; no pending request in a different Activity. */
final class NativeNext {
 final SystemPlayerActivity a;boolean cancelled;long shownAt=-1;LinearLayout card;TextView text;
 NativeNext(SystemPlayerActivity a){this.a=a;}
 String target(){return AutoNextRules.next(a.meta,a.id);}
 boolean eligible(){String next=target();return PlaybackReviewRules.series(a.meta,a.type)&&next!=null&&EmbyApi.isEmbyId(next)&&PlaybackPrefs.autoNext(a.getSharedPreferences("data",0))&&!cancelled&&!a.failed&&!a.changing;}
 boolean end(){if(!eligible())return false;tick();return true;}
 void suspend(){hide();shownAt=-1;}
 void cancel(){cancelled=true;hide();shownAt=-1;}
 void hide(){if(card!=null&&card.getParent() instanceof ViewGroup)((ViewGroup)card.getParent()).removeView(card);card=null;}
 void tick(){if(a.isFinishing()||!a.resumed||CinemaMenu.isOpen(a)){suspend();return;}boolean due=eligible()&&AutoNextRules.offer(true,PlaybackReviewRules.series(a.meta,a.type),true,cancelled,a.terminal,a.video.getCurrentPosition(),a.video.getDuration(),PlaybackPrefs.autoNextLead(a.getSharedPreferences("data",0)));if(!due||(!a.terminal&&!a.video.isPlaying())){suspend();return;}if(card==null)show();int left=AutoNextRules.secondsLeft(SystemClock.elapsedRealtime()-shownAt);text.setText("下一集 · "+AutoNextRules.label(a.meta,target())+"\n"+left+" 秒后播放");if(left==0){String next=target();cancel();a.playEpisode(next);}}
 void show(){shownAt=SystemClock.elapsedRealtime();card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));card.setBackground(PlayerTheme.panel(a.getResources().getDisplayMetrics().density,PlayerTheme.RADIUS_CARD_DP));text=new TextView(a);text.setTextColor(-1);text.setTextSize(15);card.addView(text);LinearLayout row=new LinearLayout(a);Button cancel=a.chrome.button("取消",()->cancel());row.addView(cancel);row.addView(a.chrome.button("立即播放",()->{String next=target();cancel();a.playEpisode(next);}));card.addView(row);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);lp.rightMargin=a.dp(20);lp.bottomMargin=Math.max(a.dp(150),a.bottom.getHeight()+a.dp(12));a.root.addView(card,lp);}
}
