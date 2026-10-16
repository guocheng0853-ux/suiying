package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** 播放页补充线条图标（画中画、锁定、解锁），与原 NavIcon 同为白色描边风格。 */
final class PlayerIcons extends Drawable {
 static final int PIP=1,LOCK=2,UNLOCK=3;
 final int type,size;final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
 PlayerIcons(int type,int size){this.type=type;this.size=size;stroke.setStyle(Paint.Style.STROKE);stroke.setColor(0xffffffff);stroke.setStrokeWidth(Math.max(2f,size/12f));stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeJoin(Paint.Join.ROUND);fill.setColor(0xffffffff);}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas c){
  Rect b=getBounds();float s=Math.min(b.width(),b.height()),x=b.left+(b.width()-s)/2f,y=b.top+(b.height()-s)/2f,u=s/24f;
  if(type==PIP){RectF outer=new RectF(x+2*u,y+5*u,x+22*u,y+19*u);c.drawRoundRect(outer,2*u,2*u,stroke);RectF inner=new RectF(x+12*u,y+11*u,x+19.5f*u,y+16.5f*u);c.drawRoundRect(inner,1*u,1*u,fill);return;}
  RectF body=new RectF(x+5*u,y+11*u,x+19*u,y+21*u);c.drawRoundRect(body,2*u,2*u,stroke);
  Path shackle=new Path();shackle.moveTo(x+8*u,y+11*u);shackle.lineTo(x+8*u,y+7.5f*u);shackle.arcTo(new RectF(x+8*u,y+3*u,x+16*u,y+11*u),180,180,false);
  if(type==LOCK)shackle.lineTo(x+16*u,y+11*u);else shackle.lineTo(x+16*u,y+6.5f*u);
  c.drawPath(shackle,stroke);c.drawCircle(x+12*u,y+16*u,1.4f*u,fill);
 }
 @Override public void setAlpha(int a){stroke.setAlpha(a);fill.setAlpha(a);}
 @Override public void setColorFilter(ColorFilter f){stroke.setColorFilter(f);fill.setColorFilter(f);}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
