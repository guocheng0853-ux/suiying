package com.guocheng.mediapocket;
import android.view.*;import android.widget.*;
/**
 * 0.1.127 首页下拉刷新。没有 androidx 的 SwipeRefreshLayout，这里在 Activity.dispatchTouchEvent 里旁听触摸：
 * 首页滚到顶时往下拉，顶部出现「下拉刷新 / 松开刷新」小胶囊，松手后清掉各目录的缓存时间戳并重建首页。
 * 只旁听不吃事件，横滑轮播、点海报都不受影响；横向位移大于纵向时不算下拉。
 */
final class PullRefresh {
 final ScrapedMainActivity a;final TextView pill;float startY=-1,startX;boolean armed,busy;
 PullRefresh(ScrapedMainActivity a,FrameLayout stage){this.a=a;pill=a.text("下拉刷新",13,a.gold?CinemaTheme.ON_GOLD:0xffffffff);pill.setGravity(Gravity.CENTER);pill.setPadding(a.dp(16),0,a.dp(16),0);pill.setBackground(a.rounded(a.gold?CinemaTheme.GOLD:0xee2c4a55,18));pill.setElevation(a.dp(8));pill.setAlpha(0);stage.addView(pill,new FrameLayout.LayoutParams(-2,a.dp(36),Gravity.TOP|Gravity.CENTER_HORIZONTAL));}
 boolean atTop(){return a.homeCanvas&&a.scroll!=null&&a.scroll.getScrollY()<=0;}
 void track(MotionEvent e){if(busy)return;switch(e.getActionMasked()){
  case MotionEvent.ACTION_DOWN:startY=atTop()?e.getRawY():-1;startX=e.getRawX();armed=false;break;
  case MotionEvent.ACTION_MOVE:{if(startY<0){if(atTop()){startY=e.getRawY();startX=e.getRawX();}break;}float dy=e.getRawY()-startY,dx=Math.abs(e.getRawX()-startX);
   if(!atTop()||dy<=0||dx>dy){hide();startY=-1;break;}float pull=Math.min(dy*.5f,a.dp(120));pill.setAlpha(Math.min(1f,pull/a.dp(48)));pill.setTranslationY(a.safeTop+pull-a.dp(24));armed=pull>=a.dp(80);pill.setText(armed?"松开刷新":"下拉刷新");break;}
  case MotionEvent.ACTION_UP:if(armed&&atTop())refresh();else hide();startY=-1;armed=false;break;
  case MotionEvent.ACTION_CANCEL:hide();startY=-1;armed=false;break;
  default:break;}}
 void hide(){pill.animate().alpha(0).setDuration(150).start();}
 void refresh(){busy=true;pill.setText("正在刷新…");pill.animate().translationY(a.safeTop+a.dp(16)).alpha(1).setDuration(150).start();
  a.forceRefresh();
  pill.postDelayed(()->{pill.setText("已刷新");pill.animate().alpha(0).setStartDelay(600).setDuration(250).withEndAction(()->busy=false).start();},700);}
}
