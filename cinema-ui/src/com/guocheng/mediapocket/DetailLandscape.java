package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.view.*;import android.widget.*;
/**
 * 0.1.104：Emby / 影视仓详情页的横屏版式（参考 Hills）。
 * 竖屏不变。横屏时：窗口铺进挖孔区，正文避开挖孔；背景图通栏、约占屏高八成；
 * 片名、信息行、主按钮、简介叠在背景图左下，左侧加一道横向压暗保证字看得清；选集接在图下面。
 */
final class DetailLandscape {
 private DetailLandscape(){}
 static boolean on(Activity a){android.util.DisplayMetrics m=a.getResources().getDisplayMetrics();return m.widthPixels>m.heightPixels;}
 static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}

 /** 根布局不再带左右内边距（否则通栏背景会被 ScrollView 裁成两边留缝）；左右边距改给页头、状态行和正文，并加上挖孔 / 导航栏的安全区。 */
 static void window(Activity a,LinearLayout root,View header,View status,LinearLayout body){
  FullscreenMode.immersive(a);
  // 0.1.112：内容铺满整个窗口（含挖孔和状态栏下方），四边安全区自己留；窗口底色与页面同色，任何没盖到的缝都看不出来
  int bg=root.getBackground() instanceof android.graphics.drawable.ColorDrawable?((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor():0xff10343e;
  a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bg));
  if(Build.VERSION.SDK_INT>=30)a.getWindow().setDecorFitsSystemWindows(false);else{View decor=a.getWindow().getDecorView();decor.setSystemUiVisibility(decor.getSystemUiVisibility()|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
  int top=root.getPaddingTop();root.setPadding(0,top,0,0);
  apply(a,header,status,body,0,0);
  root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(0,top+insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());int l=insets.getSystemWindowInsetLeft(),r=insets.getSystemWindowInsetRight();
   if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){l=Math.max(l,insets.getDisplayCutout().getSafeInsetLeft());r=Math.max(r,insets.getDisplayCutout().getSafeInsetRight());}
   apply(a,header,status,body,l,r);return insets;});
 }
 static void apply(Activity a,View header,View status,LinearLayout body,int l,int r){int s=dp(a,16);
  if(header!=null)header.setPadding(l+s,header.getPaddingTop(),r+s,header.getPaddingBottom());
  if(status!=null)status.setPadding(l+s,status.getPaddingTop(),r+s,status.getPaddingBottom());
  body.setPadding(l+s,body.getPaddingTop(),r+s,body.getPaddingBottom());
  for(int i=0;i<body.getChildCount();i++){View c=body.getChildAt(i);if(!"mp-detail-hero".equals(c.getTag()))continue;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)c.getLayoutParams();p.leftMargin=-body.getPaddingLeft();p.rightMargin=-body.getPaddingRight();p.width=-1;c.setLayoutParams(p);}
 }

 /** 背景图高度：竖屏照旧；横屏约屏高的 78%，片名等叠在图上。 */
 static int heroHeight(Activity a,int portrait){if(!on(a))return portrait;return Math.round(a.getResources().getDisplayMetrics().heightPixels*.78f);}

 /** 背景图通栏：宽度跟随正文（负边距抵掉正文内边距），不再用屏幕像素宽度去猜。 */
 static LinearLayout.LayoutParams heroParams(LinearLayout body,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height);p.setMargins(-body.getPaddingLeft(),0,-body.getPaddingRight(),0);return p;}

 /**
  * 横屏时把片名、信息行、主按钮、简介挪到背景图左下：
  * 片名用负的上边距叠回图里，文字只占左边约 58%，主按钮固定宽度，图上加左侧横向压暗。
  */
 static void overlay(Activity a,FrameLayout hero,int heroH,int surface,TextView name,View facts,View primary,TextView overview){
  if(!on(a))return;
  View side=new View(a);int rgb=surface&0xffffff;side.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xe6000000|rgb,0xb0000000|rgb,0x40000000|rgb,0}));
  hero.addView(side,new FrameLayout.LayoutParams(-1,-1));
  int width=a.getResources().getDisplayMetrics().widthPixels,right=Math.round(width*.42f);
  if(overview!=null){overview.setMaxLines(3);narrow(overview,right);}
  if(facts!=null)narrow(facts,right);
  if(primary!=null){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)primary.getLayoutParams();if(p!=null){p.width=dp(a,240);p.gravity=Gravity.START;primary.setLayoutParams(p);}}
  if(name!=null){name.setMaxLines(2);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)name.getLayoutParams();if(p!=null){p.topMargin=-Math.min(heroH-dp(a,72),dp(a,overview==null?140:206));p.rightMargin=right;name.setLayoutParams(p);}}
 }
 static void narrow(View v,int right){ViewGroup.LayoutParams raw=v.getLayoutParams();if(raw instanceof LinearLayout.LayoutParams){((LinearLayout.LayoutParams)raw).rightMargin=right;v.setLayoutParams(raw);}else{LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.rightMargin=right;v.setLayoutParams(p);}}
}
