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
 * 首页导航。胶囊里四项等宽，图标在上、文字常在；选中是一块中性圆角底，切换时滑过去。
 * 平板横屏同一套，改成竖向滑动。空处不吃点击，好让底下的海报还能点到。
 */
final class HomeNavBar extends FrameLayout {
 interface TabClick { void onTab(String tab); }
 private static final PathInterpolator EASE=new PathInterpolator(BottomNavRules.EASE_X1,BottomNavRules.EASE_Y1,BottomNavRules.EASE_X2,BottomNavRules.EASE_Y2);
 private final String[] tabs;
 private final TabClick click;
 private final Item[] items;
 private final Runnable slideRunnable=this::startSlide;
 private Bar bar;
 private boolean rail,day;
 private int selected=-1;
 private int safeLeft,safeTop,safeRight,safeBottom;
 private boolean slideQueued;
 private ValueAnimator slide;
 private BottomNavRules.Slide motion=BottomNavRules.Slide.resting(null);
 private BottomNavRules.Box shown;

 HomeNavBar(Context context,String[] tabs,TabClick click){
  super(context);
  this.tabs=tabs;this.click=click;
  setClickable(false);setFocusable(false);setClipChildren(false);setClipToPadding(false);
  items=new Item[tabs.length];
  for(int i=0;i<tabs.length;i++)items[i]=new Item(i,tabs[i]);
 }
 /** 详情页收起时停掉滑动。旋转换了形态就直接落到新位置，不补播。 */
 void layoutChrome(boolean rail,boolean day,int safeLeft,int safeTop,int safeRight,int safeBottom,boolean visible){
  this.safeLeft=safeLeft;this.safeTop=safeTop;this.safeRight=safeRight;this.safeBottom=safeBottom;
  if(!visible){setVisibility(GONE);settle();return;}
  boolean modeChanged=bar==null||this.rail!=rail||this.day!=day;
  this.rail=rail;this.day=day;
  setVisibility(VISIBLE);
  if(modeChanged){settle();buildBar();}
  place();
  if(modeChanged)snapSoon();
 }
 void select(int index,boolean animate){
  int next=BottomNavRules.keptIndex(index,items.length);
  int prev=selected;selected=next;
  boolean move=animate&&prev>=0&&prev!=next&&getVisibility()==VISIBLE&&bar!=null&&motion();
  for(Item item:items)item.setLit(item.index==next?1f:0f,move);
  if(!move){restOn(slot(selected));return;}
  if(slide!=null||motion.running){if(slide!=null){slide.cancel();slide=null;}launch(true);return;}
  slideQueued=true;
  bar.removeCallbacks(slideRunnable);
  bar.post(slideRunnable);
 }
 /** 配置变化或导航被收起：停在当前选中项，不把半截动画留着。 */
 void settle(){
  slideQueued=false;
  if(bar!=null)bar.removeCallbacks(slideRunnable);
  if(slide!=null){slide.cancel();slide=null;}
  for(Item item:items)item.setLit(item.index==selected?1f:0f,false);
  motion=motion.settle();
  placeShown(motion.shown);
 }
 void restOn(BottomNavRules.Box target){
  slideQueued=false;
  if(bar!=null)bar.removeCallbacks(slideRunnable);
  if(slide!=null){slide.cancel();slide=null;}
  motion=BottomNavRules.selectMark(motion,target,false);
  placeShown(motion.shown);
 }
 void snapSoon(){
  if(bar==null)return;
  bar.post(() -> {if(slide!=null||slideQueued)return;BottomNavRules.Box fitted=slot(selected);if(BottomNavRules.markVisible(fitted))motion=BottomNavRules.Slide.resting(fitted);placeShown(BottomNavRules.markVisible(fitted)?motion.shown:fitted);});
 }
 boolean motion(){
  if(!ValueAnimator.areAnimatorsEnabled())return false;
  android.content.ContentResolver r=getContext().getContentResolver();
  float a=Settings.Global.getFloat(r,Settings.Global.ANIMATOR_DURATION_SCALE,1f);
  float t=Settings.Global.getFloat(r,Settings.Global.TRANSITION_ANIMATION_SCALE,1f);
  float w=Settings.Global.getFloat(r,Settings.Global.WINDOW_ANIMATION_SCALE,1f);
  return BottomNavRules.motionEnabled(a,t,w);
 }
 void startSlide(){slideQueued=false;launch(motion.running);}
 void launch(boolean retarget){
  if(bar==null||getVisibility()!=VISIBLE){placeShown(slot(selected));return;}
  BottomNavRules.Box to=slot(selected);
  if(!BottomNavRules.markVisible(to)){placeShown(to);return;}
  boolean allow=motion();
  motion=retarget?motion.retarget(to,allow):motion.play(to,allow);
  if(slide!=null){slide.cancel();slide=null;}
  if(!motion.running){placeShown(motion.shown);return;}
  final BottomNavRules.Slide origin=motion;
  slide=ValueAnimator.ofFloat(0f,1f);
  slide.setDuration(BottomNavRules.slideMs(true));
  slide.setInterpolator(EASE);
  slide.addUpdateListener(a->{motion=origin.advance((Float)a.getAnimatedValue());placeShown(motion.shown);});
  slide.addListener(new AnimatorListenerAdapter(){
   boolean cancelled;
   public void onAnimationCancel(Animator animation){cancelled=true;if(slide==animation)slide=null;}
   public void onAnimationEnd(Animator animation){if(cancelled)return;if(slide==animation)slide=null;motion=origin.advance(1f);placeShown(motion.shown);}
  });
  slide.start();
 }
 BottomNavRules.Box slot(int index){
  if(bar==null)return new BottomNavRules.Box(0,0,0,0);
  float pad=dp(rail?BottomNavRules.RAIL_PAD_DP:BottomNavRules.PILL_PAD_DP);
  float inset=dp(BottomNavRules.MARK_INSET_DP);
  float track=rail?bar.getHeight():bar.getWidth();
  float cross=rail?bar.getWidth():bar.getHeight();
  return BottomNavRules.indicator(rail,index,items.length,track,cross,pad,inset);
 }
 void placeShown(BottomNavRules.Box box){
  shown=box;
  if(bar!=null)bar.placeMark(box);
 }
 void buildBar(){
  if(bar!=null){bar.removeCallbacks(slideRunnable);removeView(bar);}
  bar=new Bar(getContext());
  GradientDrawable plate=new GradientDrawable();
  plate.setColor(rail?BottomNavRules.railSurface(day):BottomNavRules.surface(day));
  plate.setCornerRadius(dp(rail?BottomNavRules.RAIL_RADIUS_DP:BottomNavRules.PILL_RADIUS_DP));
  plate.setStroke(Math.max(1,dp(1)),BottomNavRules.edge(day));
  bar.setBackground(plate);
  bar.setElevation(dp(day?2:3));
  if(android.os.Build.VERSION.SDK_INT>=28){int shadow=BottomNavRules.shadow(day);bar.setOutlineAmbientShadowColor(shadow);bar.setOutlineSpotShadowColor(shadow);}
  for(Item item:items){if(item.getParent() instanceof ViewGroup)((ViewGroup)item.getParent()).removeView(item);bar.row.addView(item,itemParams());}
  addView(bar);
  for(Item item:items)item.setLit(item.index==selected?1f:0f,false);
 }
 LinearLayout.LayoutParams itemParams(){
  if(rail)return new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(64));
  return new LinearLayout.LayoutParams(0,LayoutParams.MATCH_PARENT,1f);
 }
 void place(){
  if(bar==null)return;
  if(rail){
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(BottomNavRules.RAIL_WIDTH_DP),LayoutParams.WRAP_CONTENT,Gravity.START|Gravity.TOP);
   bar.setLayoutParams(lp);
  }else{
   int parent=getWidth();if(parent<=0)parent=getResources().getDisplayMetrics().widthPixels;
   int gutter=Math.max(dp(BottomNavRules.NAV_PAD_H_DP),Math.max(safeLeft,safeRight));
   int width=Math.min(Math.max(dp(160),parent-gutter*2),dp(480));
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,dp(BottomNavRules.PILL_HEIGHT_DP),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
   lp.bottomMargin=safeBottom+dp(BottomNavRules.NAV_PAD_BOTTOM_DP);
   bar.setLayoutParams(lp);
  }
 }
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
  super.onLayout(changed,left,top,right,bottom);
  if(rail&&bar!=null&&getVisibility()==VISIBLE){
   int x=safeLeft+dp(BottomNavRules.RAIL_LEFT_DP);
   int y=BottomNavRules.railTop(getHeight(),bar.getMeasuredHeight(),safeTop,safeBottom);
   bar.layout(x,y,x+bar.getMeasuredWidth(),y+bar.getMeasuredHeight());
  }
  if(slide==null&&!slideQueued){BottomNavRules.Box fitted=slot(selected);if(BottomNavRules.markVisible(fitted))motion=BottomNavRules.Slide.resting(fitted);placeShown(fitted);}
  else if(shown!=null)bar.placeMark(shown);
 }
 @Override public boolean onTouchEvent(MotionEvent event){return false;}
 @Override protected void onDetachedFromWindow(){settle();super.onDetachedFromWindow();}
 int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 static int blend(int from,int to,float t){
  float u=t<0f?0f:t>1f?1f:t;
  int af=(from>>>24)&255,at=(to>>>24)&255,rf=(from>>>16)&255,rt=(to>>>16)&255,gf=(from>>>8)&255,gt=(to>>>8)&255,bf=from&255,bt=to&255;
  return (Math.round(af+(at-af)*u)<<24)|(Math.round(rf+(rt-rf)*u)<<16)|(Math.round(gf+(gt-gf)*u)<<8)|Math.round(bf+(bt-bf)*u);
 }

 final class Bar extends FrameLayout {
  final Row row=new Row(getContext());
  final Highlight mark=new Highlight(getContext());
  Bar(Context context){
   super(context);
   setClickable(false);setFocusable(false);setClipChildren(false);setClipToPadding(false);
   addView(mark,new LayoutParams(0,0));
   row.setOrientation(rail?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
   int pad=dp(rail?BottomNavRules.RAIL_PAD_DP:BottomNavRules.PILL_PAD_DP);
   row.setPadding(pad,pad,pad,pad);
   row.setClickable(false);row.setFocusable(false);
   addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
  }
  void placeMark(BottomNavRules.Box box){
   if(!BottomNavRules.markVisible(box)){mark.setAlpha(0f);return;}
   mark.setAlpha(1f);
   mark.layout(0,0,Math.round(box.w),Math.round(box.h));
   mark.setTranslationX(box.x);
   mark.setTranslationY(box.y);
  }
  @Override public boolean onTouchEvent(MotionEvent event){return false;}
  @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
   super.onLayout(changed,left,top,right,bottom);
   if(shown!=null)placeMark(shown);
  }
 }

 final class Row extends LinearLayout {
  private final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect=new RectF();
  Row(Context context){super(context);ring.setStyle(Paint.Style.STROKE);setDefaultFocusHighlightEnabled(false);}
  @Override protected void dispatchDraw(Canvas canvas){
   super.dispatchDraw(canvas);
   float sw=Math.max(1,dp(2));
   ring.setStrokeWidth(sw);ring.setColor(BottomNavRules.focus(day));
   for(int i=0;i<getChildCount();i++){
    View child=getChildAt(i);
    if(!child.isFocused())continue;
    float inset=sw/2f+dp(1);
    rect.set(child.getLeft()+inset,child.getTop()+inset,child.getRight()-inset,child.getBottom()-inset);
    canvas.drawRoundRect(rect,dp(BottomNavRules.MARK_RADIUS_DP),dp(BottomNavRules.MARK_RADIUS_DP),ring);
   }
  }
 }

 final class Highlight extends View {
  Highlight(Context context){
   super(context);
   setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
   GradientDrawable plate=new GradientDrawable();
   plate.setColor(BottomNavRules.mark(day));
   plate.setCornerRadius(dp(BottomNavRules.MARK_RADIUS_DP));
   plate.setStroke(Math.max(1,dp(1)),BottomNavRules.markEdge(day));
   setBackground(plate);setAlpha(0f);
  }
 }

 final class Item extends LinearLayout {
  final int index;final String label;final Glyph glyph;final TextView text;
  float lit;ValueAnimator litAnim;
  Item(int index,String label){
   super(HomeNavBar.this.getContext());
   this.index=index;this.label=label;
   setOrientation(VERTICAL);setGravity(Gravity.CENTER);
   setClickable(true);setFocusable(true);setFocusableInTouchMode(false);
   setDefaultFocusHighlightEnabled(false);setForeground(null);setBackground(null);
   setMinimumHeight(dp(48));
   setContentDescription(label);
   setOnClickListener(v->{if(click!=null)click.onTab(label);});
   glyph=new Glyph(index);
   addView(glyph,new LayoutParams(dp(BottomNavRules.ICON_DP),dp(BottomNavRules.ICON_DP)));
   text=new TextView(getContext());
   text.setText(label);text.setSingleLine(true);text.setIncludeFontPadding(false);text.setGravity(Gravity.CENTER);
   text.setTextSize(TypedValue.COMPLEX_UNIT_SP,BottomNavRules.LABEL_SP);
   text.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
   text.setFocusable(false);text.setClickable(false);
   LayoutParams lp=new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
   lp.topMargin=dp(2);
   addView(text,lp);
  }
  @Override public boolean onTouchEvent(MotionEvent e){if(rail&&!BottomNavRules.insideHit(true,e.getX(),e.getY(),getWidth(),getHeight(),dp(48)))return false;return super.onTouchEvent(e);}
  void setLit(float target,boolean animate){
   if(litAnim!=null){litAnim.cancel();litAnim=null;}
   if(!animate){lit=target;paint();return;}
   litAnim=ValueAnimator.ofFloat(lit,target);
   litAnim.setDuration(BottomNavRules.slideMs(true));
   litAnim.setInterpolator(EASE);
   litAnim.addUpdateListener(a->{lit=(Float)a.getAnimatedValue();paint();});
   litAnim.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator animation){if(litAnim==animation)litAnim=null;}});
   litAnim.start();
  }
  void paint(){
   setSelected(lit>0.5f);
   text.setTextColor(blend(BottomNavRules.muted(day),BottomNavRules.ink(day),lit));
   text.setTypeface(weight(lit>0.5f));
   glyph.icon.tint(blend(BottomNavRules.muted(day),BottomNavRules.ink(day),lit));
   glyph.icon.fill(lit);
   glyph.invalidate();
  }
  Typeface weight(boolean selected){
   if(android.os.Build.VERSION.SDK_INT>=28)return Typeface.create(Typeface.create("sans-serif",Typeface.NORMAL),selected?600:400,false);
   return Typeface.create(selected?"sans-serif-medium":"sans-serif",Typeface.NORMAL);
  }
 }

 final class Glyph extends View {
  final HomeNavIcon icon;
  Glyph(int kind){
   super(HomeNavBar.this.getContext());
   icon=new HomeNavIcon(kind,dp(BottomNavRules.ICON_DP),BottomNavRules.muted(false));
   setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
   setFocusable(false);setClickable(false);setDefaultFocusHighlightEnabled(false);
  }
  @Override protected void onDraw(Canvas canvas){icon.setBounds(0,0,getWidth(),getHeight());icon.draw(canvas);}
 }
}
