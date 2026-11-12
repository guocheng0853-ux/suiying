package com.guocheng.mediapocket;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** 方向三底栏图标：24 视口，未选中描边，选中填实。搜索选中只加粗，不填色。 */
final class HomeNavIcon extends Drawable {
 private final int kind,size;
 private int color,alpha=255;
 private float fill;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path shape=new Path();

 HomeNavIcon(int kind,int size,int color){this.kind=kind;this.size=size;this.color=color;}
 void tint(int value){if(color!=value){color=value;invalidateSelf();}}
 /** 0 描边，1 填实。搜索在两端之间加粗描边。 */
 void fill(float value){float next=value<0?0:value>1?1:value;if(next!=fill){fill=next;invalidateSelf();}}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas canvas){
  Rect bounds=getBounds();if(bounds.width()<=0||bounds.height()<=0)return;
  int save=canvas.save();
  canvas.translate(bounds.left,bounds.top);
  canvas.scale(bounds.width()/24f,bounds.height()/24f);
  paint.setAntiAlias(true);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
  if(kind==2)drawSearch(canvas);else if(kind==1)drawHeart(canvas);else if(kind==3)drawPerson(canvas);else drawHouse(canvas);
  canvas.restoreToCount(save);
 }
 void paintColor(float factor){
  int a=Math.round(alpha*(factor<0?0:factor>1?1:factor));
  paint.setColor((color&0xffffff)|(a<<24));
 }
 void drawHouse(Canvas canvas){
  housePath();
  if(fill<0.98f){paintColor(1f-fill);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);canvas.drawPath(shape,paint);}
  if(fill>0.02f){paintColor(fill);paint.setStyle(Paint.Style.FILL);canvas.drawPath(shape,paint);}
 }
 void housePath(){
  shape.reset();
  shape.moveTo(3.6f,10.4f);shape.lineTo(12f,3.4f);shape.lineTo(20.4f,10.4f);shape.lineTo(20.4f,19.5f);
  shape.arcTo(18.2f,18.4f,20.4f,20.6f,0f,90f,false);
  shape.lineTo(15f,20.6f);shape.lineTo(15f,14.6f);shape.lineTo(9f,14.6f);shape.lineTo(9f,20.6f);shape.lineTo(4.7f,20.6f);
  shape.arcTo(3.6f,18.4f,5.8f,20.6f,90f,90f,false);
  shape.close();
 }
 void drawHeart(Canvas canvas){
  shape.reset();
  shape.moveTo(12f,20.4f);
  shape.cubicTo(6.6f,16.6f,3.2f,13.2f,3.2f,9.2f);
  shape.arcTo(3.1894f,4.3919f,12.1894f,13.3919f,176.0745f,167.2413f,false);
  shape.arcTo(11.8106f,4.3919f,20.8106f,13.3919f,-163.3158f,167.2413f,false);
  shape.cubicTo(20.8f,13.2f,17.4f,16.6f,12f,20.4f);
  shape.close();
  if(fill<0.98f){paintColor(1f-fill);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);canvas.drawPath(shape,paint);}
  if(fill>0.02f){paintColor(fill);paint.setStyle(Paint.Style.FILL);canvas.drawPath(shape,paint);}
 }
 void drawSearch(Canvas canvas){
  float r=6.4f+(6.2f-6.4f)*fill,width=1.8f+(3.1f-1.8f)*fill,x=15.2f+(15.4f-15.2f)*fill,y=15.2f+(15.4f-15.2f)*fill;
  paintColor(1f);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);
  canvas.drawCircle(10.4f,10.4f,r,paint);
  canvas.drawLine(x,y,20.6f,20.6f,paint);
 }
 void drawPerson(Canvas canvas){
  if(fill<0.98f){
   paintColor(1f-fill);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);
   canvas.drawCircle(12f,8f,4f,paint);shoulders(false);canvas.drawPath(shape,paint);
  }
  if(fill>0.02f){
   paintColor(fill);paint.setStyle(Paint.Style.FILL);
   canvas.drawCircle(12f,8f,4.1f,paint);shoulders(true);canvas.drawPath(shape,paint);
  }
 }
 void shoulders(boolean close){
  shape.reset();shape.moveTo(4.2f,20.6f);shape.cubicTo(4.2f,16.4f,7.7f,14f,12f,14f);shape.cubicTo(16.3f,14f,19.8f,16.4f,19.8f,20.6f);
  if(close)shape.close();
 }
 @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
