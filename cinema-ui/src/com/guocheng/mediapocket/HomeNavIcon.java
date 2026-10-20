package com.guocheng.mediapocket;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Compact filled navigation glyphs, drawn at a shared 24-unit optical size. */
final class HomeNavIcon extends Drawable {
 private final int kind,size;
 private int color,alpha=255;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path shape=new Path();

 HomeNavIcon(int kind,int size,int color){this.kind=kind;this.size=size;this.color=color;}
 void tint(int value){if(color!=value){color=value;invalidateSelf();}}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas canvas){
  Rect bounds=getBounds();int save=canvas.save();
  canvas.translate(bounds.left,bounds.top);canvas.scale(bounds.width()/24f,bounds.height()/24f);
  paint.setColor(color);paint.setAlpha(alpha);paint.setStyle(Paint.Style.FILL);
  shape.reset();shape.setFillType(Path.FillType.EVEN_ODD);
  if(kind==0){
   shape.moveTo(2.5f,10);shape.lineTo(11,2.8f);shape.quadTo(12,2,13,2.8f);
   shape.lineTo(21.5f,10);shape.quadTo(22.4f,11,21,11.5f);shape.lineTo(20,11.5f);
   shape.lineTo(20,20);shape.quadTo(20,21,19,21);shape.lineTo(5,21);
   shape.quadTo(4,21,4,20);shape.lineTo(4,11.5f);shape.lineTo(3,11.5f);
   shape.quadTo(1.6f,11,2.5f,10);shape.close();
   shape.addRoundRect(9,13,15,21,1,1,Path.Direction.CW);
  }else if(kind==1){
   shape.moveTo(12,21);shape.cubicTo(8.5f,18.5f,2,13.7f,2,8.2f);
   shape.cubicTo(2,2.5f,8.8f,1.5f,12,5.8f);
   shape.cubicTo(15.2f,1.5f,22,2.5f,22,8.2f);
   shape.cubicTo(22,13.7f,15.5f,18.5f,12,21);shape.close();
  }else if(kind==2){
   shape.addCircle(10,10,7.8f,Path.Direction.CW);shape.addCircle(10,10,4.7f,Path.Direction.CW);
   canvas.drawPath(shape,paint);paint.setStyle(Paint.Style.STROKE);
   paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(3.2f);
   canvas.drawLine(15.7f,15.7f,21,21,paint);canvas.restoreToCount(save);return;
  }else{
   shape.addCircle(12,7,4.2f,Path.Direction.CW);
   shape.moveTo(3,21);shape.lineTo(3,19.5f);shape.cubicTo(3,11.2f,21,11.2f,21,19.5f);
   shape.lineTo(21,21);shape.close();
  }
  canvas.drawPath(shape,paint);canvas.restoreToCount(save);
 }
 @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
