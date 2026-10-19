package com.guocheng.mediapocket;
import android.app.Activity;import android.app.AlertDialog;import android.content.DialogInterface;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.96：和 AlertDialog.Builder 同样的写法，弹出的是深色 CinemaMenu（独立窗口，播放页里也点得动）。
 * 支持标题、说明文字、列表、单选、自定义视图（输入框会自动改成浅色字）、确定 / 中性 / 取消按钮和取消回调。
 * 弹不出来时（画中画、锁屏）退回系统对话框，保证需要用户决定的流程不会卡住。
 * 行为差异：点行后先收起菜单再执行回调；列表菜单里没有回调的「取消 / 关闭」按钮不单独占一行（点外面或返回键即可关闭）。
 */
final class CinemaDialog implements DialogInterface {
 final Activity a;CharSequence title,message;CharSequence[] items;DialogInterface.OnClickListener itemListener;int checked=-1;boolean single;View view;
 CharSequence posText,negText,neuText;DialogInterface.OnClickListener posListener,negListener,neuListener;DialogInterface.OnCancelListener cancelListener;
 boolean done;
 CinemaDialog(Activity a){this.a=a;}
 CinemaDialog setTitle(CharSequence t){title=t;return this;}
 CinemaDialog setMessage(CharSequence m){message=m;return this;}
 CinemaDialog setItems(CharSequence[] list,DialogInterface.OnClickListener l){items=list;itemListener=l;single=false;return this;}
 CinemaDialog setSingleChoiceItems(CharSequence[] list,int checkedItem,DialogInterface.OnClickListener l){items=list;itemListener=l;checked=checkedItem;single=true;return this;}
 CinemaDialog setView(View v){view=v;return this;}
 CinemaDialog setPositiveButton(CharSequence t,DialogInterface.OnClickListener l){posText=t;posListener=l;return this;}
 CinemaDialog setNegativeButton(CharSequence t,DialogInterface.OnClickListener l){negText=t;negListener=l;return this;}
 CinemaDialog setNeutralButton(CharSequence t,DialogInterface.OnClickListener l){neuText=t;neuListener=l;return this;}
 CinemaDialog setOnCancelListener(DialogInterface.OnCancelListener l){cancelListener=l;return this;}
 CinemaDialog setCancelable(boolean ignored){return this;}
 static boolean quietClose(CharSequence text,DialogInterface.OnClickListener l){if(l!=null||text==null)return false;String s=text.toString().trim();return s.equals("取消")||s.equals("关闭");}
 CinemaDialog show(){
  if(a.isFinishing())return this;
  List<CinemaMenu.Item> rows=new ArrayList<>();
  if(items!=null)for(int i=0;i<items.length;i++){final int at=i;String label=items[i]==null?"":items[i].toString();rows.add(single?CinemaMenu.choice(label,i==checked,()->{done=true;if(itemListener!=null)itemListener.onClick(this,at);}):CinemaMenu.item(label,()->{done=true;if(itemListener!=null)itemListener.onClick(this,at);}));}
  boolean list=items!=null&&items.length>0;
  if(posText!=null)rows.add(button(posText,posListener,DialogInterface.BUTTON_POSITIVE));
  if(neuText!=null)rows.add(button(neuText,neuListener,DialogInterface.BUTTON_NEUTRAL));
  if(negText!=null&&!(list&&quietClose(negText,negListener)))rows.add(button(negText,negListener,DialogInterface.BUTTON_NEGATIVE));
  View body=body();
  Runnable onCancel=()->{done=true;if(cancelListener!=null)cancelListener.onCancel(this);};
  if(!CinemaMenu.open(a,title==null?null:title.toString(),body,rows,onCancel))fallback();
  return this;
 }
 CinemaMenu.Item button(CharSequence text,DialogInterface.OnClickListener l,int which){return CinemaMenu.item(text.toString(),()->{done=true;if(l!=null)l.onClick(this,which);});}
 View body(){
  TextView msg=null;if(message!=null&&message.length()>0){msg=new TextView(a);msg.setText(message);msg.setTextColor(PlayerTheme.TEXT_SECONDARY);msg.setTextSize(14);msg.setLineSpacing(0,1.15f);msg.setTextIsSelectable(true);}
  if(view!=null)restyle(view);
  if(msg==null)return view;if(view==null)return msg;
  LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.addView(msg);if(view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);box.addView(view);return box;
 }
 /** 原来按白底对话框写的输入表单：字改成浅色，输入框下划线改成浅色。 */
 static void restyle(View v){
  if(v instanceof EditText){EditText e=(EditText)v;e.setTextColor(0xffffffff);e.setHintTextColor(0x80ffffff);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x80ffffff));return;}
  if(v instanceof Button||v instanceof Spinner)return;
  if(v instanceof TextView){TextView t=(TextView)v;if(luminance(t.getCurrentTextColor())<0.5)t.setTextColor(PlayerTheme.TEXT_SECONDARY);return;}
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)restyle(g.getChildAt(i));}
 }
 static double luminance(int c){return (0.299*((c>>16)&255)+0.587*((c>>8)&255)+0.114*(c&255))/255.0;}
 /** 深色菜单弹不出来时（画中画、锁屏），用系统对话框兜底。 */
 void fallback(){
  try{AlertDialog.Builder b=new AlertDialog.Builder(a);if(title!=null)b.setTitle(title);if(message!=null)b.setMessage(message);
   if(items!=null){if(single)b.setSingleChoiceItems(items,checked,itemListener);else b.setItems(items,itemListener);}
   if(view!=null){if(view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);b.setView(view);}
   if(posText!=null)b.setPositiveButton(posText,posListener);if(negText!=null)b.setNegativeButton(negText,negListener);if(neuText!=null)b.setNeutralButton(neuText,neuListener);
   if(cancelListener!=null)b.setOnCancelListener(cancelListener);b.show();}catch(Exception ignored){}
 }
 /** 行回调里再调 d.dismiss() / d.cancel() 时菜单已经收起，不能误关回调里新弹出的菜单。 */
 @Override public void cancel(){if(done)return;done=true;CinemaMenu.cancel(a);}
 @Override public void dismiss(){if(done)return;done=true;CinemaMenu.dismiss(a);}
}
