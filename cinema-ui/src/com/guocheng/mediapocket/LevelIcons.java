package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** 音量（静音 / 低 / 中 / 高）和亮度太阳。按 24dp 盒子缩放，各密度都是矢量。 */
final class LevelIcons extends Drawable {
 final int type,size;final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
 LevelIcons(int type,int size){this.type=type;this.size=Math.max(1,size);stroke.setStyle(Paint.Style.STROKE);stroke.setColor(0xffffffff);stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeJoin(Paint.Join.ROUND);fill.setStyle(Paint.Style.FILL);fill.setColor(0xffffffff);}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas canvas){
  Rect b=getBounds();float side=Math.min(b.width(),b.height());if(side<=0)return;
  float u=side/24f,sw=Math.max(1f,2f*u);stroke.setStrokeWidth(sw);
  float x0=b.left+(b.width()-side)/2f,y0=b.top+(b.height()-side)/2f;
  LevelIconGeometry.draw(type,new LevelIconGeometry.Sink(){
   public void fill(float[] xy){Path p=path(xy,x0,y0,u);p.close();canvas.drawPath(p,fill);}
   public void stroke(float[] xy){canvas.drawPath(path(xy,x0,y0,u),stroke);}
   public void circle(float cx,float cy,float r){canvas.drawCircle(x0+cx*u,y0+cy*u,r*u,stroke);}
   public void line(float x1,float y1,float x2,float y2){canvas.drawLine(x0+x1*u,y0+y1*u,x0+x2*u,y0+y2*u,stroke);}
  });
 }
 static Path path(float[] xy,float x0,float y0,float u){Path p=new Path();if(xy==null||xy.length<4)return p;p.moveTo(x0+xy[0]*u,y0+xy[1]*u);for(int i=2;i+1<xy.length;i+=2)p.lineTo(x0+xy[i]*u,y0+xy[i+1]*u);return p;}
 @Override public void setAlpha(int a){stroke.setAlpha(a);fill.setAlpha(a);}
 @Override public void setColorFilter(ColorFilter f){stroke.setColorFilter(f);fill.setColorFilter(f);}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
