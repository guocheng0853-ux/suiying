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
  View row=body.getParent()==null?null:(View)body.getParent().getParent();boolean split=row!=null&&"mp-split".equals(row.getTag());
  body.setPadding(split?dp(a,8):l+s,body.getPaddingTop(),r+s,body.getPaddingBottom());
  if(split&&row instanceof ViewGroup&&((ViewGroup)row).getChildCount()>0){View left=((ViewGroup)row).getChildAt(0);left.setPadding(l+s,left.getPaddingTop(),left.getPaddingRight(),left.getPaddingBottom());}
  for(int i=0;i<body.getChildCount();i++){View c=body.getChildAt(i);if(!"mp-detail-hero".equals(c.getTag()))continue;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)c.getLayoutParams();p.leftMargin=-body.getPaddingLeft();p.rightMargin=-body.getPaddingRight();p.width=-1;c.setLayoutParams(p);}
 }

 /** 背景图高度：竖屏照旧；横屏约屏高的 78%，片名等叠在图上。 */
 static int heroHeight(Activity a,int portrait){if(!on(a))return portrait;return Math.round(a.getResources().getDisplayMetrics().heightPixels*.78f);}

 /** 背景图通栏：宽度跟随正文（负边距抵掉正文内边距），不再用屏幕像素宽度去猜。 */
 static LinearLayout.LayoutParams heroParams(LinearLayout body,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height);p.setMargins(-body.getPaddingLeft(),0,-body.getPaddingRight(),0);return p;}

 /**
  * 0.1.133 横屏改成左右分栏：左边是封面图（固定不随正文滚动），右边是可滚动的正文——片名、信息、播放按钮、简介、选集。
  * 做法：把正文所在的 ScrollView 从根布局里拿出来，和左侧图片一起放进一行横排；之后往 body 里加的选集、更多类似都会进右栏。
  * 离开详情（screen() 换页）时调用 unsplit() 还原，媒体库、搜索等页面不受影响。参数沿用旧版 overlay 的签名。
  */
 static void overlay(Activity a,FrameLayout hero,int heroH,int surface,TextView name,View facts,View primary,TextView overview){
  if(!on(a)||hero==null||!(hero.getParent() instanceof LinearLayout))return;LinearLayout body=(LinearLayout)hero.getParent();
  if(!(body.getParent() instanceof ScrollView)||!(body.getParent().getParent() instanceof LinearLayout))return;
  ScrollView sv=(ScrollView)body.getParent();LinearLayout root=(LinearLayout)sv.getParent();if("mp-split".equals(root.getTag()))return;
  // 左栏图片：只留封面，去掉竖屏用的底部渐隐；圆角
  for(int i=hero.getChildCount()-1;i>=1;i--)hero.removeViewAt(i);GradientDrawable clip=new GradientDrawable();clip.setColor(0x22000000);clip.setCornerRadius(dp(a,14));hero.setBackground(clip);hero.setClipToOutline(true);
  body.removeView(hero);int width=a.getResources().getDisplayMetrics().widthPixels,leftW=Math.round(width*.36f);
  int idx=root.indexOfChild(sv);ViewGroup.LayoutParams svLp=sv.getLayoutParams();root.removeView(sv);
  LinearLayout row=new LinearLayout(a);row.setTag("mp-split");row.setOrientation(LinearLayout.HORIZONTAL);
  FrameLayout left=new FrameLayout(a);left.setTag("mp-split-left");int pad=dp(a,12);left.setPadding(body.getPaddingLeft(),dp(a,4),pad,dp(a,16));left.addView(hero,new FrameLayout.LayoutParams(-1,-1));
  row.addView(left,new LinearLayout.LayoutParams(leftW,-1));row.addView(sv,new LinearLayout.LayoutParams(0,-1,1f));root.addView(row,idx,svLp);
  body.setPadding(dp(a,8),body.getPaddingTop(),body.getPaddingRight(),body.getPaddingBottom());
  // 右栏顶部不留空：片名不再用负边距叠进图里；文字铺满右栏，简介最多 4 行
  if(name!=null){name.setMaxLines(2);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)name.getLayoutParams();if(p!=null){p.topMargin=dp(a,4);p.rightMargin=0;name.setLayoutParams(p);}}
  for(View v:new View[]{facts,overview})if(v!=null&&v.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();p.rightMargin=0;v.setLayoutParams(p);}
  if(overview!=null)overview.setMaxLines(4);
  if(primary!=null&&primary.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)primary.getLayoutParams();p.width=-1;primary.setLayoutParams(p);}
 }
 /** 还原分栏：把 ScrollView 放回根布局原位置，左栏图片丢掉（下一页会重新建）。 */
 static void unsplit(LinearLayout body){if(body==null||!(body.getParent() instanceof ScrollView))return;ScrollView sv=(ScrollView)body.getParent();if(!(sv.getParent() instanceof LinearLayout))return;LinearLayout row=(LinearLayout)sv.getParent();if(!"mp-split".equals(row.getTag())||!(row.getParent() instanceof LinearLayout))return;
  LinearLayout root=(LinearLayout)row.getParent();int idx=root.indexOfChild(row);ViewGroup.LayoutParams lp=row.getLayoutParams();row.removeView(sv);root.removeView(row);root.addView(sv,idx,lp);
  body.setPadding(body.getPaddingRight(),body.getPaddingTop(),body.getPaddingRight(),body.getPaddingBottom());}
 static void narrow(View v,int right){ViewGroup.LayoutParams raw=v.getLayoutParams();if(raw instanceof LinearLayout.LayoutParams){((LinearLayout.LayoutParams)raw).rightMargin=right;v.setLayoutParams(raw);}else{LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.rightMargin=right;v.setLayoutParams(p);}}
}
