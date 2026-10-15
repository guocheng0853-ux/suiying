package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
final class CinemaGlyph extends Drawable {
 final int size,kind,color;final Paint paint=new Paint(3);
 CinemaGlyph(int kind,int size,int color){this.kind=kind;this.size=size;this.color=color;}
 public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
 public void draw(Canvas c){c.save();Rect b=getBounds();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);
  if(kind==0){c.drawCircle(10.5f,10.5f,6.5f,paint);c.drawLine(15.5f,15.5f,21,21,paint);}else{for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1.3f,paint);}c.restore();}
 public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
