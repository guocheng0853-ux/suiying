package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** 详情页图标，24 格、圆头线。返回、播放、更多、线路、下载、收藏、勾、片源。 */
final class PosterGlyphs extends Drawable {
 static final int GLOBE=1,DOWN=2,HEART=3,HEART_ON=4,CHECK=5,CHEV=6,FILM=7,BACK=8,PLAY=9,MORE=10;
 final int kind,size;final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
 PosterGlyphs(int kind,int size){this.kind=kind;this.size=Math.max(1,size);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeJoin(Paint.Join.ROUND);fill.setStyle(Paint.Style.FILL);tint(0xfff4f1ea);}
 PosterGlyphs tint(int color){stroke.setColor(color);fill.setColor(color);invalidateSelf();return this;}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas c){Rect b=getBounds();float side=Math.min(b.width(),b.height());if(side<=0)return;c.save();c.translate(b.left+(b.width()-side)/2f,b.top+(b.height()-side)/2f);c.scale(side/24f,side/24f);stroke.setStrokeWidth(kind==CHECK?2.4f:1.7f);drawKind(c);c.restore();}
 void drawKind(Canvas c){if(kind==GLOBE){c.drawCircle(12,12,8,stroke);c.drawLine(4,12,20,12,stroke);c.drawOval(new RectF(8.2f,4,15.8f,20),stroke);}
  else if(kind==DOWN){c.drawLine(12,4,12,15,stroke);Path a=new Path();a.moveTo(7,11);a.lineTo(12,16);a.lineTo(17,11);c.drawPath(a,stroke);c.drawLine(5,20,19,20,stroke);}
  else if(kind==HEART||kind==HEART_ON){Path h=heart();c.drawPath(h,kind==HEART_ON?fill:stroke);}
  else if(kind==CHECK){Path p=new Path();p.moveTo(6,12.5f);p.lineTo(10.2f,16.6f);p.lineTo(18,8);c.drawPath(p,stroke);}
  else if(kind==CHEV){Path p=new Path();p.moveTo(6,9);p.lineTo(12,15);p.lineTo(18,9);c.drawPath(p,stroke);}
  else if(kind==FILM){c.drawRoundRect(new RectF(3,6,21,18),2,2,stroke);c.drawLine(3,10,21,10,stroke);c.drawLine(3,14,21,14,stroke);c.drawLine(8,6,8,18,stroke);c.drawLine(16,6,16,18,stroke);}
  else if(kind==BACK){Path p=new Path();p.moveTo(15,6);p.lineTo(8,12);p.lineTo(15,18);c.drawPath(p,stroke);}
  else if(kind==PLAY){Path p=new Path();p.moveTo(8,5);p.lineTo(8,19);p.lineTo(19,12);p.close();c.drawPath(p,fill);}
  else if(kind==MORE){c.drawCircle(12,6,1.35f,fill);c.drawCircle(12,12,1.35f,fill);c.drawCircle(12,18,1.35f,fill);}}
 static Path heart(){Path h=new Path();h.moveTo(12,19);h.cubicTo(12,19,4.5f,14.2f,4.5f,9.4f);h.cubicTo(4.5f,6.6f,6.7f,5,8.8f,5);h.cubicTo(10.4f,5,11.4f,5.8f,12,7.1f);h.cubicTo(12.6f,5.8f,13.6f,5,15.2f,5);h.cubicTo(17.3f,5,19.5f,6.6f,19.5f,9.4f);h.cubicTo(19.5f,14.2f,12,19,12,19);h.close();return h;}
 @Override public void setAlpha(int a){stroke.setAlpha(a);fill.setAlpha(a);}
 @Override public void setColorFilter(ColorFilter f){stroke.setColorFilter(f);fill.setColorFilter(f);}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
