package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.view.*;import android.widget.*;
import java.util.List;
/**
 * 自绘深色菜单，替代系统白底 AlertDialog（0.1.83 Hills 语言）。
 * 行=左侧文字 + 右对齐当前值；单选项整行淡青。挂在 android.R.id.content 上，
 * 播放页和「我的」都能用；画中画与锁屏时不弹。
 */
final class CinemaMenu {
 static final class Item {
  final String label,value;final boolean selected;final Runnable action;String hint;
  Item(String label,String value,boolean selected,Runnable action){this.label=label;this.value=value;this.selected=selected;this.action=action;}
 }
 /** 0.1.133：带副标题的菜单项——短标题一行，长解释放在下面一行小字里，不再把解释塞进标题导致被省略号截断。 */
 static Item detail(String label,String hint,Runnable action){Item it=new Item(label,null,false,action);it.hint=hint;return it;}
 static Item item(String label,Runnable action){return new Item(label,null,false,action);}
 static Item value(String label,String value,Runnable action){return new Item(label,value,false,action);}
 static Item choice(String label,boolean selected,Runnable action){return new Item(label,null,selected,action);}
 private CinemaMenu(){}
 static boolean blocked(Activity a){return (Build.VERSION.SDK_INT>=24&&a.isInPictureInPictureMode())||PlayerGestures.locked(a);}
 /**
  * 弹出。rows 里可以混用 item/value/choice；点任意一行都会先收起再执行动作。
  * 0.1.92：放在独立的透明全屏 Dialog 窗口里。原来挂在 android.R.id.content 上，播放页的
  * GesturePlayerActivity.dispatchTouchEvent 只把落在播放器 root 里 Button/SeekBar 上的触摸交给视图，
  * 其余全当画面手势吞掉——菜单看得见却点不动。独立窗口的触摸不经过 Activity 分发，也不再怕视频面重挂把菜单压住。
  */
 static void show(final Activity a,String title,List<Item> rows){open(a,title,null,rows,null);}
 /**
  * 通用形态：标题 + 可选正文（说明文字或输入表单）+ 行。没点任何行就关掉（点外面、返回键）时执行 onCancel。
  * 返回 false 表示没弹出（画中画、锁屏、页面已关闭），调用方可以改用系统对话框。
  */
 static boolean open(final Activity a,String title,View body,List<Item> rows,final Runnable onCancel){
  if(rows==null)rows=new java.util.ArrayList<>();
  if(a.isFinishing()||blocked(a)||(rows.isEmpty()&&body==null))return false;
  dismissNow(a);
  final View decor=a.getWindow().getDecorView();
  final android.app.Dialog dialog=new android.app.Dialog(a,android.R.style.Theme_Translucent_NoTitleBar);
  final FrameLayout scrim=new FrameLayout(a);scrim.setBackgroundColor(PlayerTheme.SCRIM);scrim.setClickable(true);
  final LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
  GradientDrawable bg=new GradientDrawable();bg.setColor(PlayerTheme.surface(a));if(PlayerTheme.paper(a))bg.setStroke(dp(a,1),CinemaTheme.LINE);bg.setCornerRadius(dp(a,PlayerTheme.RADIUS_PANEL_DP));panel.setBackground(bg);
  panel.setPadding(dp(a,14),dp(a,12),dp(a,14),dp(a,14));
  int screen=decor.getWidth()>0?decor.getWidth():a.getResources().getDisplayMetrics().widthPixels;
  int width=Math.min(dp(a,PlayerTheme.MENU_WIDTH_MAX_DP),screen-dp(a,40));
  if(title!=null&&!title.isEmpty()){
   TextView head=new TextView(a);head.setText(title);head.setTextColor(PlayerTheme.text(a));head.setTextSize(PlayerMotionRules.MENU_TITLE_SP);boolean serifHead=!(a instanceof PlayerActivity)&&!(a instanceof SystemPlayerActivity);head.setTypeface(serifHead?CinemaTheme.paperTitle(a):android.graphics.Typeface.DEFAULT_BOLD);if(serifHead)head.setTextSize(Ui.SECTION);
   head.setPadding(dp(a,8),dp(a,6),dp(a,8),dp(a,4));panel.addView(head);
  }
  if(body!=null){if(body.getParent() instanceof ViewGroup)((ViewGroup)body.getParent()).removeView(body);LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(-1,-2);bl.setMargins(dp(a,8),dp(a,4),dp(a,8),dp(a,8));panel.addView(body,bl);}
  for(final Item it:rows){
   LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
   row.setMinimumHeight(dp(a,PlayerMotionRules.MENU_ROW_DP));row.setPadding(dp(a,10),dp(a,4),dp(a,10),dp(a,4));
   GradientDrawable line=new GradientDrawable();line.setColor(it.selected?PlayerTheme.selection(a):0x00000000);line.setCornerRadius(dp(a,PlayerTheme.RADIUS_MENU_ROW_DP));
   android.graphics.drawable.RippleDrawable ripple=new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(PlayerTheme.ripple(a)),line,null);row.setBackground(ripple);
   TextView label=new TextView(a);label.setText(it.label);label.setTextColor(it.selected?PlayerTheme.selectedText(a):PlayerTheme.text(a));label.setTextSize(PlayerMotionRules.MENU_LABEL_SP);
   label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
   if(it.hint!=null&&!it.hint.isEmpty()){LinearLayout texts=new LinearLayout(a);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(0,dp(a,4),0,dp(a,4));texts.addView(label);TextView sub=new TextView(a);sub.setText(it.hint);sub.setTextColor(PlayerTheme.secondary(a));sub.setTextSize(PlayerMotionRules.MENU_VALUE_SP);sub.setMaxLines(2);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);sub.setPadding(0,dp(a,2),0,0);texts.addView(sub);row.addView(texts,new LinearLayout.LayoutParams(0,-2,1f));}
   else row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
   if(it.value!=null&&!it.value.isEmpty()){TextView value=new TextView(a);value.setText(it.value);value.setTextColor(PlayerTheme.secondary(a));value.setTextSize(PlayerMotionRules.MENU_VALUE_SP);value.setPadding(dp(a,8),0,0,0);value.setMaxWidth(Math.round(width*.38f));value.setMaxLines(2);value.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(value);}
   CinemaControls.group(row,it.label+(it.value==null?"":"，"+it.value)+(it.hint==null?"":"，"+it.hint));row.setSelected(it.selected);
   row.setOnClickListener(v->{
    dismiss(a);
    // 动作挂在 Activity 的窗口上执行：菜单窗口关掉后它自己的 Handler 回调不一定还在
    if(it.action!=null)decor.postDelayed(()->{if(!a.isFinishing())it.action.run();},PlayerMotionRules.MENU_OUT_MS);
   });
   panel.addView(row);
  }
  // 行加完再量面板高度：超过屏幕 78% 就套 ScrollView，长菜单（如「更多」19 项）可滚动。
  panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
  float vh=decor.getHeight()>0?decor.getHeight():a.getResources().getDisplayMetrics().heightPixels;
  int menuH=Math.min(panel.getMeasuredHeight(),Math.round(vh*0.78f));
  FrameLayout center=new FrameLayout(a);
  ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(panel,new FrameLayout.LayoutParams(width,-2));
  center.addView(scroll,new FrameLayout.LayoutParams(width,menuH,Gravity.CENTER));
  scrim.addView(center,new FrameLayout.LayoutParams(-1,-1));
  scrim.setOnClickListener(v->cancel(a));
  dialog.setContentView(scrim);
  dialog.setCancelable(true);
  dialog.setOnKeyListener((d,code,e)->{if(code==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)cancel(a);return true;}return false;});
  Window w=dialog.getWindow();
  if(w!=null){
   w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
   w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
   w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_FULLSCREEN);
   w.setLayout(-1,-1);w.setWindowAnimations(0);
   if(body!=null&&hasInput(body))w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
   if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams lp=w.getAttributes();lp.layoutInDisplayCutoutMode=a.getWindow().getAttributes().layoutInDisplayCutoutMode;w.setAttributes(lp);}
   // 先不可聚焦地显示并复制沉浸式标志，再恢复可聚焦（返回键），避免弹出时状态栏/导航栏闪出来
   w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
  }
  try{dialog.show();}catch(Exception e){return false;}
  if(w!=null){w.getDecorView().setSystemUiVisibility(decor.getSystemUiVisibility());w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);}
  OPEN.put(a,new Open(dialog,scrim,onCancel));
  scrim.setAlpha(0f);panel.setScaleX(0.92f);panel.setScaleY(0.92f);
  scrim.animate().alpha(1f).setDuration(PlayerMotionRules.MENU_DIM_MS).start();
  panel.animate().scaleX(1f).scaleY(1f).setDuration(PlayerMotionRules.MENU_DIM_MS).start();
  return true;
 }
 static boolean hasInput(View v){if(v instanceof EditText)return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasInput(g.getChildAt(i)))return true;}return false;}
 static final class Open {final android.app.Dialog dialog;final FrameLayout scrim;final Runnable onCancel;Open(android.app.Dialog d,FrameLayout s,Runnable c){dialog=d;scrim=s;onCancel=c;}}
 /** 没选任何行就关掉：收起并执行 onCancel。 */
 static void cancel(final Activity a){Open o=OPEN.get(a);final Runnable after=o==null?null:o.onCancel;dismiss(a);if(after!=null)a.getWindow().getDecorView().postDelayed(()->{if(!a.isFinishing())after.run();},PlayerMotionRules.MENU_OUT_MS);}
 static final java.util.Map<Activity,Open> OPEN=new java.util.WeakHashMap<>();
 static boolean isOpen(Activity a){Open o=OPEN.get(a);return o!=null&&o.dialog.isShowing();}
 static void dismiss(final Activity a){
  final Open o=OPEN.remove(a);if(o==null)return;
  o.scrim.setEnabled(false);
  o.scrim.animate().alpha(0f).setDuration(PlayerMotionRules.MENU_OUT_MS).withEndAction(()->close(a,o)).start();
 }
 static void dismissNow(Activity a){Open o=OPEN.remove(a);if(o!=null)close(a,o);}
 static void close(Activity a,Open o){try{if(o.dialog.isShowing()&&!a.isDestroyed())o.dialog.dismiss();}catch(Exception ignored){}}
 /** 旧接口：菜单在独立窗口里，不会再被视频面压住，这里不用做事。 */
 static void front(Activity a){}
 static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
}
