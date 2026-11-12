package com.guocheng.mediapocket;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 首页导航。手机（以及平板竖屏、分屏窄窗）是底部展开胶囊；平板横屏是左缘竖向侧轨。
 * 未选中的胶囊项只留图标，名称放在 contentDescription。展开动效跟系统「移除动画」。
 */
final class HomeNavBar extends FrameLayout {
 interface TabClick { void onTab(String tab); }
 private static final PathInterpolator EASE=new PathInterpolator(BottomNavRules.EASE_X1,BottomNavRules.EASE_Y1,BottomNavRules.EASE_X2,BottomNavRules.EASE_Y2);
 private final String[] tabs;
 private final TabClick click;
 private final Item[] items;
 private Fade fade;
 private Bar bar;
 private boolean rail,day;
 private int selected=-1;
 private int safeLeft,safeTop,safeRight,safeBottom;

 HomeNavBar(Context context,String[] tabs,TabClick click){
  super(context);
  this.tabs=tabs;this.click=click;
  setClipChildren(false);setClipToPadding(false);setClickable(false);setFocusable(false);
  items=new Item[tabs.length];
  for(int i=0;i<tabs.length;i++)items[i]=new Item(i,tabs[i]);
 }
 /** 旋转和分屏只换容器，选中项保持。详情页整条收起。 */
 void layoutChrome(boolean rail,boolean day,int safeLeft,int safeTop,int safeRight,int safeBottom,boolean visible){
  this.safeLeft=safeLeft;this.safeTop=safeTop;this.safeRight=safeRight;this.safeBottom=safeBottom;
  setVisibility(visible?VISIBLE:GONE);
  if(!visible)return;
  boolean modeChanged=bar==null||this.rail!=rail||this.day!=day;
  this.rail=rail;this.day=day;
  if(fade==null){fade=new Fade(getContext());addView(fade);}
  fade.setPage(day);fade.setVisibility(rail?GONE:VISIBLE);
  if(modeChanged){buildBar();applyItems(false);}
  place();
 }
 void select(int index,boolean animate){
  int next=BottomNavRules.keptIndex(index,items.length);
  int prev=selected;selected=next;
  if(bar==null)return;
  boolean move=animate&&prev>=0&&prev!=next&&motion();
  applyItems(move);
 }
 boolean motion(){
  if(!ValueAnimator.areAnimatorsEnabled())return false;
  android.content.ContentResolver r=getContext().getContentResolver();
  float a=Settings.Global.getFloat(r,Settings.Global.ANIMATOR_DURATION_SCALE,1f);
  float t=Settings.Global.getFloat(r,Settings.Global.TRANSITION_ANIMATION_SCALE,1f);
  float w=Settings.Global.getFloat(r,Settings.Global.WINDOW_ANIMATION_SCALE,1f);
  return BottomNavRules.motionEnabled(a,t,w);
 }
 void buildBar(){
  if(bar!=null)removeView(bar);
  bar=new Bar(getContext());
  bar.setOrientation(rail?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
  bar.setGravity(android.view.Gravity.CENTER);
  bar.setClipChildren(false);bar.setClipToPadding(false);
  int pad=dp(rail?BottomNavRules.RAIL_PAD_DP:BottomNavRules.PILL_PAD_DP);
  bar.setPadding(pad,pad,pad,pad);
  GradientDrawable plate=new GradientDrawable();
  plate.setColor(BottomNavRules.surface(day));
  plate.setCornerRadius(dp(rail?BottomNavRules.RAIL_RADIUS_DP:BottomNavRules.PILL_RADIUS_DP));
  plate.setStroke(Math.max(1,dp(1)),BottomNavRules.edge(day));
  bar.setBackground(plate);
  bar.setClickable(true);bar.setFocusable(false);bar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);bar.setElevation(dp(rail?8:6));
  if(android.os.Build.VERSION.SDK_INT>=28){int shadow=BottomNavRules.shadow(day);bar.setOutlineAmbientShadowColor(shadow);bar.setOutlineSpotShadowColor(shadow);}
  GradientDrawable gap=new GradientDrawable();gap.setSize(dp(BottomNavRules.RAIL_GAP_DP),dp(BottomNavRules.RAIL_GAP_DP));
  bar.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);bar.setDividerDrawable(gap);
  for(Item item:items){if(item.getParent() instanceof ViewGroup)((ViewGroup)item.getParent()).removeView(item);bar.addView(item);}
  addView(bar);
 }
 void place(){
  if(fade!=null&&!rail){
   FrameLayout.LayoutParams fadeLp=new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(BottomNavRules.fadeHeightDp(0))+safeBottom,Gravity.BOTTOM);
   fade.setLayoutParams(fadeLp);
  }
  if(bar==null)return;
  if(rail){
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(BottomNavRules.RAIL_WIDTH_DP),LayoutParams.WRAP_CONTENT,Gravity.START|Gravity.TOP);
   bar.setLayoutParams(lp);
  }else{
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dp(BottomNavRules.PILL_HEIGHT_DP),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
   lp.bottomMargin=safeBottom+dp(BottomNavRules.NAV_PAD_BOTTOM_DP);
   lp.leftMargin=Math.max(dp(BottomNavRules.NAV_PAD_H_DP),safeLeft);
   lp.rightMargin=Math.max(dp(BottomNavRules.NAV_PAD_H_DP),safeRight);
   bar.setLayoutParams(lp);
  }
 }
 void applyItems(boolean animate){for(Item item:items)item.setOn(item.index==selected,animate);}
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
  super.onLayout(changed,left,top,right,bottom);
  if(!rail||bar==null||getVisibility()!=VISIBLE)return;
  int x=safeLeft+dp(BottomNavRules.RAIL_LEFT_DP);
  int y=BottomNavRules.railTop(getHeight(),bar.getMeasuredHeight(),safeTop,safeBottom);
  bar.layout(x,y,x+bar.getMeasuredWidth(),y+bar.getMeasuredHeight());
 }
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();for(Item item:items)if(item.anim!=null)item.anim.cancel();}
 int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 static int blend(int from,int to,float t){
  float u=t<0?0:t>1?1:t;
  int af=(from>>>24)&255,at=(to>>>24)&255,rf=(from>>>16)&255,rt=(to>>>16)&255,gf=(from>>>8)&255,gt=(to>>>8)&255,bf=from&255,bt=to&255;
  return (Math.round(af+(at-af)*u)<<24)|(Math.round(rf+(rt-rf)*u)<<16)|(Math.round(gf+(gt-gf)*u)<<8)|Math.round(bf+(bt-bf)*u);
 }

 final class Fade extends View {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private int tone=BottomNavRules.NIGHT_PAGE;
  private int lastW=-1,lastH=-1;
  Fade(Context context){super(context);setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  void setPage(boolean day){int next=BottomNavRules.page(day);if(next!=tone){tone=next;lastH=-1;invalidate();}}
  @Override protected void onDraw(Canvas canvas){
   int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
   if(w!=lastW||h!=lastH||paint.getShader()==null){
    lastW=w;lastH=h;
    paint.setShader(new LinearGradient(0,0,0,h,new int[]{BottomNavRules.fadeColor(tone,0f),BottomNavRules.fadeColor(tone,BottomNavRules.FADE_MID_ALPHA),BottomNavRules.fadeColor(tone,1f)},new float[]{0f,BottomNavRules.FADE_MID,1f},Shader.TileMode.CLAMP));
   }
   canvas.drawRect(0,0,w,h,paint);
  }
 }

 final class Bar extends LinearLayout {
  private final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect=new RectF();
  Bar(Context context){super(context);ring.setStyle(Paint.Style.STROKE);}
  @Override protected void dispatchDraw(Canvas canvas){
   super.dispatchDraw(canvas);
   float o=Math.max(1,dp(2));
   ring.setStrokeWidth(o);ring.setColor(BottomNavRules.focus(day));
   float rad=dp(rail?BottomNavRules.RAIL_ITEM_RADIUS_DP:BottomNavRules.PILL_ITEM_RADIUS_DP)+o;
   for(int i=0;i<getChildCount();i++){
    View child=getChildAt(i);if(!child.isFocused())continue;
    rect.set(child.getLeft()-o,child.getTop()-o,child.getRight()+o,child.getBottom()+o);
    canvas.drawRoundRect(rect,rad,rad,ring);
   }
  }
 }

 final class Item extends LinearLayout {
  final int index;final String label;final Glyph glyph;final TextView text;final GradientDrawable chip=new GradientDrawable();
  float shown;boolean on;ValueAnimator anim;
  Item(int index,String label){
   super(HomeNavBar.this.getContext());
   this.index=index;this.label=label;
   setOrientation(HORIZONTAL);setGravity(Gravity.CENTER);
   setClickable(true);setFocusable(true);setDefaultFocusHighlightEnabled(false);
   setContentDescription(label);
   setOnClickListener(v->{if(click!=null)click.onTab(label);});
   glyph=new Glyph(index);
   addView(glyph,new LayoutParams(dp(BottomNavRules.ICON_DP),dp(BottomNavRules.ICON_DP)));
   text=new TextView(getContext());
   text.setText(label);text.setSingleLine(true);text.setIncludeFontPadding(false);text.setGravity(Gravity.CENTER);
   text.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
   addView(text,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
   chip.setColor(0);setBackground(chip);
  }
  void setOn(boolean selected,boolean animate){
   on=selected;float target=selected?1f:0f;
   if(anim!=null){anim.cancel();anim=null;}
   if(!animate){shown=target;apply();return;}
   anim=ValueAnimator.ofFloat(shown,target);
   anim.setDuration(rail?BottomNavRules.COLOR_MS:BottomNavRules.EXPAND_MS);
   anim.setInterpolator(EASE);
   anim.addUpdateListener(a->{shown=(Float)a.getAnimatedValue();apply();});
   anim.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator animation){if(anim==animation)anim=null;}});
   anim.start();
  }
  void apply(){
   setOrientation(rail?VERTICAL:HORIZONTAL);
   setGravity(Gravity.CENTER);
   setSelected(on);
   LayoutParams self=getLayoutParams() instanceof LayoutParams?(LayoutParams)getLayoutParams():new LayoutParams(LayoutParams.WRAP_CONTENT,dp(BottomNavRules.PILL_ITEM_MIN_DP));
   LayoutParams glyphLp=(LayoutParams)glyph.getLayoutParams();
   glyphLp.width=dp(BottomNavRules.ICON_DP);glyphLp.height=dp(BottomNavRules.ICON_DP);glyphLp.topMargin=0;glyphLp.leftMargin=0;
   LayoutParams textLp=(LayoutParams)text.getLayoutParams();
   text.setTextSize(TypedValue.COMPLEX_UNIT_SP,BottomNavRules.labelSp(rail));
   text.setTypeface(weight(on));
   if(rail){
    self.width=LayoutParams.MATCH_PARENT;self.height=dp(BottomNavRules.RAIL_ITEM_HEIGHT_DP);self.weight=0;
    setMinimumWidth(0);setMinimumHeight(dp(BottomNavRules.railItemHeightDp()));
    setPadding(0,0,0,0);
    textLp.width=LayoutParams.WRAP_CONTENT;textLp.height=LayoutParams.WRAP_CONTENT;textLp.leftMargin=0;textLp.topMargin=dp(BottomNavRules.RAIL_LABEL_GAP_DP);
    text.setAlpha(1f);text.setTextColor(blend(BottomNavRules.muted(day),BottomNavRules.onAccent(day),shown));
    chip.setCornerRadius(dp(BottomNavRules.RAIL_ITEM_RADIUS_DP));
   }else{
    self.width=LayoutParams.WRAP_CONTENT;self.height=dp(BottomNavRules.PILL_ITEM_MIN_DP);self.weight=0;
    setMinimumWidth(dp(BottomNavRules.pillItemMinDp()));setMinimumHeight(dp(BottomNavRules.pillItemMinDp()));
    int start=lerp(dp(BottomNavRules.PILL_PAD_H_DP),dp(BottomNavRules.PILL_SEL_PAD_START_DP),shown);
    int end=lerp(dp(BottomNavRules.PILL_PAD_H_DP),dp(BottomNavRules.PILL_SEL_PAD_END_DP),shown);
    setPadding(start,0,end,0);
    text.setTextColor(BottomNavRules.onAccent(day));text.setTypeface(weight(true));
    float width=text.getPaint().measureText(label)*shown;
    textLp.width=Math.max(0,(int)Math.ceil(width));textLp.height=LayoutParams.WRAP_CONTENT;
    textLp.leftMargin=Math.round(dp(BottomNavRules.LABEL_GAP_DP)*shown);textLp.topMargin=0;
    text.setAlpha(shown);
    chip.setCornerRadius(dp(BottomNavRules.PILL_ITEM_RADIUS_DP));
   }
   setLayoutParams(self);text.setLayoutParams(textLp);glyph.setLayoutParams(glyphLp);
   chip.setColor(blend(0,BottomNavRules.accent(day),shown));
   glyph.icon.tint(blend(BottomNavRules.muted(day),BottomNavRules.onAccent(day),shown));
   glyph.icon.fill(shown);glyph.invalidate();
  }
  Typeface weight(boolean selected){
   if(android.os.Build.VERSION.SDK_INT>=28)return Typeface.create(Typeface.create("sans-serif",Typeface.NORMAL),selected?600:400,false);
   return Typeface.create(selected?"sans-serif-medium":"sans-serif",Typeface.NORMAL);
  }
  int lerp(int a,int b,float t){return Math.round(a+(b-a)*(t<0?0:t>1?1:t));}
  @Override protected void onFocusChanged(boolean gain,int direction,Rect previously){
   super.onFocusChanged(gain,direction,previously);
   if(getParent() instanceof View)((View)getParent()).invalidate();
  }
 }

 final class Glyph extends View {
  final HomeNavIcon icon;
  Glyph(int kind){super(HomeNavBar.this.getContext());icon=new HomeNavIcon(kind,dp(BottomNavRules.ICON_DP),BottomNavRules.muted(false));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas canvas){icon.setBounds(0,0,getWidth(),getHeight());icon.draw(canvas);}
 }
}
