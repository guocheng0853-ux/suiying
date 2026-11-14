package com.guocheng.mediapocket;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/**
 * 首页导航图标：24 视口，四个都是同一种细线条。
 * 0.1.184：选中不再填实，只把线条加粗一点（{@link BottomNavRules#iconStroke}）。选中已经有滑动底块和文字变色，
 * 再填实是第三重提示；而且搜索本来就没法填，四个图标两种做法。房子去掉门洞，心形改成两段对称曲线。
 */
final class HomeNavIcon extends Drawable {
 private final int kind,size;
 private int color,alpha=255;
 private float lit;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path shape=new Path();

 HomeNavIcon(int kind,int size,int color){this.kind=kind;this.size=size;this.color=color;}
 void tint(int value){if(color!=value){color=value;invalidateSelf();}}
 /** 0 未选中，1 选中。只影响线条粗细。 */
 void fill(float value){float next=value<0?0:value>1?1:value;if(next!=lit){lit=next;invalidateSelf();}}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas canvas){
  Rect bounds=getBounds();if(bounds.width()<=0||bounds.height()<=0)return;
  int save=canvas.save();
  canvas.translate(bounds.left,bounds.top);
  canvas.scale(bounds.width()/24f,bounds.height()/24f);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
  paint.setStrokeWidth(BottomNavRules.iconStroke(lit));
  paint.setColor((color&0xffffff)|(alpha<<24));
  shape.reset();
  if(kind==2)search();else if(kind==1)heart();else if(kind==3)person();else house();
  canvas.drawPath(shape,paint);
  canvas.restoreToCount(save);
 }
 /** 屋顶加方盒，下面两个圆角。 */
 void house(){
  shape.moveTo(4f,10.6f);shape.lineTo(12f,4f);shape.lineTo(20f,10.6f);shape.lineTo(20f,18.6f);
  shape.quadTo(20f,20f,18.6f,20f);shape.lineTo(5.4f,20f);shape.quadTo(4f,20f,4f,18.6f);
  shape.close();
 }
 void heart(){
  shape.moveTo(12f,20f);
  shape.cubicTo(7f,16.4f,3.6f,13.4f,3.6f,9.4f);shape.cubicTo(3.6f,6.8f,5.6f,4.8f,8.1f,4.8f);shape.cubicTo(9.7f,4.8f,11.1f,5.6f,12f,6.9f);
  shape.cubicTo(12.9f,5.6f,14.3f,4.8f,15.9f,4.8f);shape.cubicTo(18.4f,4.8f,20.4f,6.8f,20.4f,9.4f);shape.cubicTo(20.4f,13.4f,17f,16.4f,12f,20f);
  shape.close();
 }
 void search(){
  shape.addCircle(10.6f,10.6f,6.2f,Path.Direction.CW);
  shape.moveTo(15.2f,15.2f);shape.lineTo(20f,20f);
 }
 /** 头是一个圆，肩是一段不封口的弧。 */
 void person(){
  shape.addCircle(12f,8.2f,3.6f,Path.Direction.CW);
  shape.moveTo(5f,20f);shape.cubicTo(5f,16.6f,8.1f,14.6f,12f,14.6f);shape.cubicTo(15.9f,14.6f,19f,16.6f,19f,20f);
 }
 @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
