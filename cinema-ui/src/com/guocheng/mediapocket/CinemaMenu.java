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
  final String label,value;final boolean selected;final Runnable action;
  Item(String label,String value,boolean selected,Runnable action){this.label=label;this.value=value;this.selected=selected;this.action=action;}
 }
 static Item item(String label,Runnable action){return new Item(label,null,false,action);}
 static Item value(String label,String value,Runnable action){return new Item(label,value,false,action);}
 static Item choice(String label,boolean selected,Runnable action){return new Item(label,null,selected,action);}
 private CinemaMenu(){}
 static boolean blocked(Activity a){return (Build.VERSION.SDK_INT>=24&&a.isInPictureInPictureMode())||PlayerGestures.locked(a);}
 /** 弹出。rows 里可以混用 item/value/choice；点任意一行都会先收起再执行动作。 */
 static void show(final Activity a,String title,List<Item> rows){
  if(a.isFinishing()||blocked(a)||rows==null||rows.isEmpty())return;
  ViewGroup content=a.findViewById(android.R.id.content);
  if(!(content instanceof FrameLayout))return;
  final FrameLayout host=(FrameLayout)content;
  final FrameLayout scrim=new FrameLayout(a);scrim.setBackgroundColor(PlayerTheme.SCRIM);scrim.setClickable(true);
  final LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
  GradientDrawable bg=new GradientDrawable();bg.setColor(PlayerTheme.PANEL);bg.setCornerRadius(dp(a,PlayerTheme.RADIUS_PANEL_DP));panel.setBackground(bg);
  panel.setPadding(dp(a,10),dp(a,8),dp(a,10),dp(a,10));
  int screen=host.getWidth()>0?host.getWidth():a.getResources().getDisplayMetrics().widthPixels;
  int width=Math.min(dp(a,PlayerTheme.MENU_WIDTH_MAX_DP),screen-dp(a,40));
  if(title!=null&&!title.isEmpty()){
   TextView head=new TextView(a);head.setText(title);head.setTextColor(0xffffffff);head.setTextSize(PlayerMotionRules.MENU_TITLE_SP);head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
   head.setPadding(dp(a,8),dp(a,6),dp(a,8),dp(a,4));panel.addView(head);
  }
  for(final Item it:rows){
   LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
   row.setMinimumHeight(dp(a,PlayerMotionRules.MENU_ROW_DP));row.setPadding(dp(a,10),dp(a,4),dp(a,10),dp(a,4));
   GradientDrawable line=new GradientDrawable();line.setColor(it.selected?PlayerTheme.ACCENT_DIM:0x00000000);line.setCornerRadius(dp(a,PlayerTheme.RADIUS_MENU_ROW_DP));row.setBackground(line);
   TextView label=new TextView(a);label.setText(it.label);label.setTextColor(it.selected?PlayerTheme.ACCENT:0xffffffff);label.setTextSize(PlayerMotionRules.MENU_LABEL_SP);
   label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
   if(it.value!=null&&!it.value.isEmpty()){TextView value=new TextView(a);value.setText(it.value);value.setTextColor(PlayerTheme.TEXT_SECONDARY);value.setTextSize(PlayerMotionRules.MENU_VALUE_SP);value.setPadding(dp(a,8),0,0,0);row.addView(value);}
   row.setOnClickListener(v->{
    dismiss(a,scrim);
    if(it.action!=null)panel.postDelayed(it.action,PlayerMotionRules.MENU_OUT_MS);
   });
   panel.addView(row);
  }
  // 行加完再量面板高度：超过屏幕 78% 就套 ScrollView，长菜单（如「更多」19 项）可滚动。
  panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
  float vh=host.getHeight()>0?host.getHeight():a.getResources().getDisplayMetrics().heightPixels;
  int menuH=Math.min(panel.getMeasuredHeight(),Math.round(vh*0.78f));
  FrameLayout center=new FrameLayout(a);
  ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.addView(panel,new FrameLayout.LayoutParams(width,-2));
  center.addView(scroll,new FrameLayout.LayoutParams(width,menuH,Gravity.CENTER));
  scrim.addView(center,new FrameLayout.LayoutParams(-1,-1));
  scrim.setOnClickListener(v->dismiss(a,scrim));
  host.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
  scrim.bringToFront();
  OPEN.put(a,scrim);
  scrim.setAlpha(0f);panel.setScaleX(0.92f);panel.setScaleY(0.92f);
  scrim.animate().alpha(1f).setDuration(PlayerMotionRules.MENU_DIM_MS).start();
  panel.animate().scaleX(1f).scaleY(1f).setDuration(PlayerMotionRules.MENU_DIM_MS).start();
 }
 static void dismiss(final Activity a,final FrameLayout scrim){
  OPEN.remove(a);
  scrim.animate().alpha(0f).setDuration(PlayerMotionRules.MENU_OUT_MS).withEndAction(()->{
   if(scrim.getParent() instanceof ViewGroup)((ViewGroup)scrim.getParent()).removeView(scrim);
  }).start();
 }
 static final java.util.Map<Activity,FrameLayout> OPEN=new java.util.WeakHashMap<>();
 /** 播放器根视图可能被视频面重建挤到最上层，菜单打开期间持续把自己顶回最前。 */
 static void front(Activity a){FrameLayout s=OPEN.get(a);if(s!=null&&s.getParent()!=null)s.bringToFront();}
 static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
}
