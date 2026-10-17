package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** 播放页白色线条图标。底栏右侧依次是音符（音轨）、CC（字幕）、列表（选集）。 */
final class PlayerIcons extends Drawable {
 static final int PIP=1,LOCK=2,UNLOCK=3,BACK=4,ASPECT=5,MORE=6,CAMERA=7,PREV=8,PLAY=9,PAUSE=10,NEXT=11,NOTE=12,CC=13,LIST=14;
 final int type,size;final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
 PlayerIcons(int type,int size){this.type=type;this.size=size;stroke.setStyle(Paint.Style.STROKE);stroke.setColor(0xffffffff);stroke.setStrokeWidth(Math.max(1.6f,size/14f));stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeJoin(Paint.Join.ROUND);fill.setColor(0xffffffff);fill.setAntiAlias(true);}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas c){
  Rect b=getBounds();float s=Math.min(b.width(),b.height()),x=b.left+(b.width()-s)/2f,y=b.top+(b.height()-s)/2f,u=s/24f;
  if(type==PIP){c.drawRoundRect(new RectF(x+2*u,y+5*u,x+22*u,y+19*u),2*u,2*u,stroke);c.drawRoundRect(new RectF(x+12*u,y+11*u,x+19.5f*u,y+16.5f*u),1*u,1*u,fill);return;}
  if(type==LOCK||type==UNLOCK){c.drawRoundRect(new RectF(x+5*u,y+11*u,x+19*u,y+21*u),2*u,2*u,stroke);Path shackle=new Path();shackle.moveTo(x+8*u,y+11*u);shackle.lineTo(x+8*u,y+7.5f*u);shackle.arcTo(new RectF(x+8*u,y+3*u,x+16*u,y+11*u),180,180,false);shackle.lineTo(x+16*u,type==LOCK?y+11*u:y+6.5f*u);c.drawPath(shackle,stroke);c.drawCircle(x+12*u,y+16*u,1.3f*u,fill);return;}
  if(type==BACK){Path p=new Path();p.moveTo(x+15*u,y+5*u);p.lineTo(x+8*u,y+12*u);p.lineTo(x+15*u,y+19*u);c.drawPath(p,stroke);return;}
  if(type==ASPECT){c.drawRoundRect(new RectF(x+3*u,y+6*u,x+21*u,y+18*u),2*u,2*u,stroke);c.drawLine(x+14*u,y+9*u,x+20*u,y+3.5f*u,stroke);c.drawLine(x+16*u,y+3.5f*u,x+20*u,y+3.5f*u,stroke);c.drawLine(x+20*u,y+3.5f*u,x+20*u,y+7.5f*u,stroke);return;}
  if(type==MORE){c.drawCircle(x+12*u,y+6*u,1.5f*u,fill);c.drawCircle(x+12*u,y+12*u,1.5f*u,fill);c.drawCircle(x+12*u,y+18*u,1.5f*u,fill);return;}
  if(type==CAMERA){c.drawRoundRect(new RectF(x+3*u,y+8*u,x+21*u,y+19*u),2*u,2*u,stroke);c.drawCircle(x+12*u,y+13.5f*u,3.1f*u,stroke);c.drawLine(x+8*u,y+8*u,x+10*u,y+5.5f*u,stroke);c.drawLine(x+10*u,y+5.5f*u,x+14*u,y+5.5f*u,stroke);return;}
  if(type==PREV||type==NEXT){boolean next=type==NEXT;float left=next?x+5*u:x+7*u,right=next?x+17*u:x+19*u,bar=next?x+19*u:x+5*u;Path p=new Path();p.moveTo(next?right:left,y+5*u);p.lineTo(next?left:right,y+12*u);p.lineTo(next?right:left,y+19*u);c.drawPath(p,stroke);c.drawLine(bar,y+5*u,bar,y+19*u,stroke);return;}
  if(type==PLAY){Path p=new Path();p.moveTo(x+8*u,y+5*u);p.lineTo(x+8*u,y+19*u);p.lineTo(x+19*u,y+12*u);p.close();c.drawPath(p,stroke);return;}
  if(type==PAUSE){c.drawLine(x+8*u,y+5*u,x+8*u,y+19*u,stroke);c.drawLine(x+16*u,y+5*u,x+16*u,y+19*u,stroke);return;}
  if(type==NOTE){c.drawCircle(x+8*u,y+16*u,2.6f*u,stroke);c.drawLine(x+10.6f*u,y+16*u,x+10.6f*u,y+6*u,stroke);Path flag=new Path();flag.moveTo(x+10.6f*u,y+6*u);flag.lineTo(x+18*u,y+8*u);flag.lineTo(x+18*u,y+11*u);flag.lineTo(x+10.6f*u,y+9.2f*u);c.drawPath(flag,stroke);return;}
  if(type==CC){fill.setTextAlign(Paint.Align.CENTER);fill.setTextSize(9.2f*u);fill.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));fill.setStyle(Paint.Style.FILL);c.drawText("CC",x+12*u,y+15.4f*u,fill);return;}
  if(type==LIST){c.drawRoundRect(new RectF(x+4*u,y+4*u,x+20*u,y+20*u),2*u,2*u,stroke);c.drawLine(x+7*u,y+9*u,x+17*u,y+9*u,stroke);c.drawLine(x+7*u,y+12*u,x+17*u,y+12*u,stroke);c.drawLine(x+7*u,y+15*u,x+14*u,y+15*u,stroke);return;}
 }
 @Override public void setAlpha(int a){stroke.setAlpha(a);fill.setAlpha(a);}
 @Override public void setColorFilter(ColorFilter f){stroke.setColorFilter(f);fill.setColorFilter(f);}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
