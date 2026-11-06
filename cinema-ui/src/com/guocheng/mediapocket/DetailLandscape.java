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

 static final int POSTER_EDGE=0x6d705072,BASE_BOTTOM=0x6d705073,SAVED_BG=0x6d705074,BASE_TOP=0x6d705075;
 static boolean poster(View root){return root!=null&&"1".equals(String.valueOf(root.getTag(POSTER_EDGE)));}
 /** 详情海报铺到系统栏下面。根布局不再为状态栏 / 导航栏留白，否则浅色页面底会在栏和海报之间露出一条白边。返回按钮抬到海报之上，正文自己避开 inset。 */
 static void posterEdge(Activity a,LinearLayout root,boolean on){if(a==null||root==null||a.getWindow()==null)return;
  if(on){if(!(root.getTag(SAVED_BG) instanceof Integer)){int bg=root.getBackground() instanceof android.graphics.drawable.ColorDrawable?((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor():0xff080706;root.setTag(SAVED_BG,bg);}root.setTag(POSTER_EDGE,"1");root.setBackgroundColor(0xff080706);a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff080706));a.getWindow().getDecorView().setBackgroundColor(0xff080706);a.getWindow().setStatusBarColor(0);a.getWindow().setNavigationBarColor(0);if(Build.VERSION.SDK_INT>=29)a.getWindow().setNavigationBarContrastEnforced(false);if(Build.VERSION.SDK_INT>=35)a.getWindow().setStatusBarContrastEnforced(false);if(Build.VERSION.SDK_INT>=30)a.getWindow().setDecorFitsSystemWindows(false);}
  else{root.setTag(POSTER_EDGE,null);Object saved=root.getTag(SAVED_BG);int color=saved instanceof Integer?(Integer)saved:0xff080706;root.setBackgroundColor(color);a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(color));a.getWindow().getDecorView().setBackgroundColor(color);a.getWindow().setStatusBarColor(color);a.getWindow().setNavigationBarColor(color);if(Build.VERSION.SDK_INT>=29)a.getWindow().setNavigationBarContrastEnforced(true);if(Build.VERSION.SDK_INT>=35)a.getWindow().setStatusBarContrastEnforced(true);View header=root.getChildCount()>0?root.getChildAt(0):null;if(header!=null){header.setTranslationZ(0);header.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);}}
  WindowInsets current=root.getRootWindowInsets();if(current!=null)root.dispatchApplyWindowInsets(current);else root.requestApplyInsets();
 }
 static int inset(WindowInsets insets,int edge){int legacy=edge==0?insets.getSystemWindowInsetLeft():edge==1?insets.getSystemWindowInsetTop():edge==2?insets.getSystemWindowInsetRight():insets.getSystemWindowInsetBottom();if(Build.VERSION.SDK_INT>=30){android.graphics.Insets c=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());int typed=edge==0?c.left:edge==1?c.top:edge==2?c.right:c.bottom;return Math.max(legacy,typed);}if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout cut=insets.getDisplayCutout();int cutout=edge==0?cut.getSafeInsetLeft():edge==1?cut.getSafeInsetTop():edge==2?cut.getSafeInsetRight():cut.getSafeInsetBottom();return Math.max(legacy,cutout);}return legacy;}
 static void tuck(Activity a,LinearLayout root,View header){if(header!=null){header.setOutlineProvider(null);header.setTranslationZ(dp(a,8));}View stage=null;for(int i=0;i<root.getChildCount();i++){View c=root.getChildAt(i);if("mp-poster".equals(c.getTag()))stage=c;}if(stage==null||!(stage.getLayoutParams() instanceof LinearLayout.LayoutParams))return;int h=header==null?0:header.getHeight();if(h<=0)h=(header==null?0:header.getPaddingTop())+dp(a,48);LinearLayout.LayoutParams sl=(LinearLayout.LayoutParams)stage.getLayoutParams();int parentH=root.getHeight();boolean grow=parentH>0&&(sl.height!=parentH||sl.weight!=0f);if(sl.topMargin!=-h||grow){sl.topMargin=-h;if(parentH>0){sl.height=parentH;sl.weight=0f;}stage.setLayoutParams(sl);}}
 /** 根布局不再带左右内边距（否则通栏背景会被 ScrollView 裁成两边留缝）；左右边距改给页头、状态行和正文，并加上挖孔 / 导航栏的安全区。 */
 static void window(Activity a,LinearLayout root,View header,View status,LinearLayout body){
  FullscreenMode.immersive(a);
  // 0.1.112：内容铺满整个窗口（含挖孔和状态栏下方），四边安全区自己留；窗口底色与页面同色，任何没盖到的缝都看不出来
  int bg=root.getBackground() instanceof android.graphics.drawable.ColorDrawable?((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor():0xff10343e;
  a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bg));
  if(Build.VERSION.SDK_INT>=30)a.getWindow().setDecorFitsSystemWindows(false);else{View decor=a.getWindow().getDecorView();decor.setSystemUiVisibility(decor.getSystemUiVisibility()|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
  int top=root.getPaddingTop();root.setTag(BASE_TOP,top);if(body!=null)body.setTag(BASE_BOTTOM,body.getPaddingBottom());root.setPadding(0,top,0,0);
  apply(a,header,status,body,0,0);
  root.setOnApplyWindowInsetsListener((v,insets)->{int topInset=inset(insets,1),bottomInset=inset(insets,3),l=inset(insets,0),r=inset(insets,2);
   if(poster(root)){root.setPadding(0,0,0,0);apply(a,header,status,body,l,r);if(header!=null)header.setPadding(header.getPaddingLeft(),topInset,header.getPaddingRight(),header.getPaddingBottom());int base=body!=null&&body.getTag(BASE_BOTTOM) instanceof Integer?(Integer)body.getTag(BASE_BOTTOM):0;if(body!=null)body.setPadding(body.getPaddingLeft(),body.getPaddingTop(),body.getPaddingRight(),base+bottomInset);tuck(a,root,header);if(header!=null)header.post(()->{if(poster(root))tuck(a,root,header);});}
   else{int baseTop=root.getTag(BASE_TOP) instanceof Integer?(Integer)root.getTag(BASE_TOP):top;root.setPadding(0,baseTop+topInset,0,0);apply(a,header,status,body,l,r);if(header!=null)header.setPadding(header.getPaddingLeft(),0,header.getPaddingRight(),header.getPaddingBottom());int base=body!=null&&body.getTag(BASE_BOTTOM) instanceof Integer?(Integer)body.getTag(BASE_BOTTOM):0;if(body!=null)body.setPadding(body.getPaddingLeft(),body.getPaddingTop(),body.getPaddingRight(),base+bottomInset);}
   return insets;});
 }
 /** Saved content insets. Independent of the "mp-split" string tag, and high enough to pass View.setTag(int). */
 static final int PAD_TAG=0x6d705064;
 static void remember(LinearLayout body,int left,int right){if(body!=null)body.setTag(PAD_TAG,new int[]{left,right});}
 static int[] remembered(LinearLayout body){if(body==null)return null;Object tag=body.getTag(PAD_TAG);if(!(tag instanceof int[])||((int[])tag).length<2)return null;return DetailSplitRules.restore(((int[])tag)[0],((int[])tag)[1]);}
 static void balance(Activity a,ViewGroup row,int contentL,int contentR){if(row==null||row.getChildCount()==0)return;View left=row.getChildAt(0);int gap=dp(a,12);int leftW=DetailSplitRules.leftColumn(a.getResources().getDisplayMetrics().widthPixels,contentL,contentR,gap);ViewGroup.LayoutParams lp=left.getLayoutParams();if(lp!=null&&lp.width!=leftW){lp.width=leftW;left.setLayoutParams(lp);}left.setPadding(0,left.getPaddingTop(),gap,left.getPaddingBottom());}
 static void apply(Activity a,View header,View status,LinearLayout body,int l,int r){int s=dp(a,16);int contentL=l+s,contentR=r+s;
  if(header!=null)header.setPadding(contentL,header.getPaddingTop(),contentR,header.getPaddingBottom());
  if(status!=null)status.setPadding(contentL,status.getPaddingTop(),contentR,status.getPaddingBottom());
  View row=body.getParent()==null?null:(View)body.getParent().getParent();boolean split=row!=null&&"mp-split".equals(row.getTag());
  remember(body,contentL,contentR);
  body.setPadding(split?dp(a,8):contentL,body.getPaddingTop(),contentR,body.getPaddingBottom());
  if(split&&row instanceof ViewGroup)balance(a,(ViewGroup)row,contentL,contentR);
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
  // 左栏图片：只留封面，去掉竖屏用的底部渐隐；0.1.137 封面出血铺到屏幕左缘（含挖孔区），只有右侧留圆角
  for(int i=hero.getChildCount()-1;i>=1;i--)hero.removeViewAt(i);GradientDrawable clip=new GradientDrawable();clip.setColor(0x22000000);clip.setCornerRadii(new float[]{0,0,dp(a,14),dp(a,14),dp(a,14),dp(a,14),0,0});hero.setBackground(clip);hero.setClipToOutline(true);
  body.removeView(hero);int contentL=body.getPaddingLeft(),contentR=body.getPaddingRight(),gap=dp(a,12);
  int leftW=DetailSplitRules.leftColumn(a.getResources().getDisplayMetrics().widthPixels,contentL,contentR,gap);
  int idx=root.indexOfChild(sv);ViewGroup.LayoutParams svLp=sv.getLayoutParams();root.removeView(sv);
  LinearLayout row=new LinearLayout(a);row.setTag("mp-split");row.setOrientation(LinearLayout.HORIZONTAL);
  FrameLayout left=new FrameLayout(a);left.setTag("mp-split-left");left.setPadding(0,dp(a,4),gap,dp(a,16));left.addView(hero,new FrameLayout.LayoutParams(-1,-1));
  row.addView(left,new LinearLayout.LayoutParams(leftW,-1));row.addView(sv,new LinearLayout.LayoutParams(0,-1,1f));root.addView(row,idx,svLp);
  remember(body,contentL,contentR);
  body.setPadding(dp(a,8),body.getPaddingTop(),contentR,body.getPaddingBottom());
  balance(a,row,contentL,contentR);
  // 右栏顶部不留空：片名不再用负边距叠进图里；文字铺满右栏，简介最多 4 行
  if(name!=null){name.setMaxLines(2);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)name.getLayoutParams();if(p!=null){p.topMargin=dp(a,4);p.rightMargin=0;name.setLayoutParams(p);}}
  for(View v:new View[]{facts,overview})if(v!=null&&v.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();p.rightMargin=0;v.setLayoutParams(p);}
  if(overview!=null)overview.setMaxLines(4);
  if(primary!=null&&primary.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)primary.getLayoutParams();p.width=-1;primary.setLayoutParams(p);}
 }
 /** 还原分栏：把 ScrollView 放回根布局原位置，左栏图片丢掉（下一页会重新建）。 */
 static void unsplit(LinearLayout body){if(body==null||!(body.getParent() instanceof ScrollView))return;ScrollView sv=(ScrollView)body.getParent();if(!(sv.getParent() instanceof LinearLayout))return;LinearLayout row=(LinearLayout)sv.getParent();if(!"mp-split".equals(row.getTag())||!(row.getParent() instanceof LinearLayout))return;
  LinearLayout root=(LinearLayout)row.getParent();int idx=root.indexOfChild(row);ViewGroup.LayoutParams lp=row.getLayoutParams();row.removeView(sv);root.removeView(row);root.addView(sv,idx,lp);
  int[] saved=remembered(body);int left=saved==null?body.getPaddingRight():saved[0],right=saved==null?body.getPaddingRight():saved[1];
  body.setPadding(left,body.getPaddingTop(),right,body.getPaddingBottom());}
 static void narrow(View v,int right){ViewGroup.LayoutParams raw=v.getLayoutParams();if(raw instanceof LinearLayout.LayoutParams){((LinearLayout.LayoutParams)raw).rightMargin=right;v.setLayoutParams(raw);}else{LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.rightMargin=right;v.setLayoutParams(p);}}
}
