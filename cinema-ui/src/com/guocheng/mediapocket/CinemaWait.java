package com.guocheng.mediapocket;
import android.app.Activity;import android.app.ProgressDialog;import android.content.DialogInterface;import android.view.Gravity;import android.widget.*;
/**
 * 0.1.96：替代播放页里的 ProgressDialog.show(...)，用法相同（dismiss / isShowing / setMessage / setOnCancelListener），
 * 显示为深色菜单里的转圈 + 一行说明。点外面或返回键 = 取消。只关自己：等待期间如果已经弹出了下一个菜单，dismiss() 不会把它关掉。
 * 弹不出来时（画中画、锁屏）退回系统 ProgressDialog。
 */
final class CinemaWait {
 final Activity a;final TextView text;CinemaMenu.Open handle;ProgressDialog system;DialogInterface.OnCancelListener cancelListener;boolean cancelled;
 private CinemaWait(Activity a,CharSequence message){this.a=a;text=new TextView(a);text.setText(message==null?"":message);text.setTextColor(PlayerTheme.TEXT_SECONDARY);text.setTextSize(14);}
 static CinemaWait show(Activity a,CharSequence title,CharSequence message,boolean indeterminate,boolean cancelable){
  CinemaWait w=new CinemaWait(a,message);
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
  ProgressBar spin=new ProgressBar(a);int s=CinemaMenu.dp(a,24);row.addView(spin,new LinearLayout.LayoutParams(s,s));
  LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(0,-2,1f);tl.leftMargin=CinemaMenu.dp(a,12);row.addView(w.text,tl);
  if(CinemaMenu.open(a,title==null?null:title.toString(),row,null,()->{w.cancelled=true;if(w.cancelListener!=null)w.cancelListener.onCancel(null);})){w.handle=CinemaMenu.OPEN.get(a);}
  else{try{w.system=ProgressDialog.show(a,title,message,indeterminate,cancelable);w.system.setOnCancelListener(d->{w.cancelled=true;if(w.cancelListener!=null)w.cancelListener.onCancel(d);});}catch(Exception ignored){}}
  return w;
 }
 void setOnCancelListener(DialogInterface.OnCancelListener l){cancelListener=l;}
 void setMessage(CharSequence m){text.setText(m);if(system!=null)system.setMessage(m);}
 boolean isShowing(){if(system!=null)return system.isShowing();return handle!=null&&!cancelled&&CinemaMenu.OPEN.get(a)==handle&&handle.dialog.isShowing();}
 void dismiss(){if(system!=null){try{if(system.isShowing())system.dismiss();}catch(Exception ignored){}return;}if(handle!=null&&CinemaMenu.OPEN.get(a)==handle)CinemaMenu.dismiss(a);handle=null;}
}
