package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** 播放页白色图标。全部画在同一 24dp 盒子里，线宽 2dp，不拉伸。 */
final class PlayerIcons extends Drawable {
 static final int PIP=1,LOCK=2,UNLOCK=3,BACK=4,ASPECT=5,MORE=6,CAMERA=7,PREV=8,PLAY=9,PAUSE=10,NEXT=11,NOTE=12,CC=13,LIST=14,BATTERY=15;
 final int type,size;final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG);
 PlayerIcons(int type,int size){this.type=type;this.size=Math.max(1,size);stroke.setStyle(Paint.Style.STROKE);stroke.setColor(0xffffffff);stroke.setStrokeCap(Paint.Cap.ROUND);stroke.setStrokeJoin(Paint.Join.ROUND);fill.setColor(0xffffffff);fill.setAntiAlias(true);fill.setStyle(Paint.Style.FILL);}
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void draw(Canvas c){
  Rect b=getBounds();float side=Math.min(b.width(),b.height());if(side<=0)return;
  float u=side/PlayerIconPaths.BOX,sw=Math.max(1f,Math.round(PlayerIconPaths.STROKE*u));
  stroke.setStrokeWidth(sw);float align=(Math.round(sw)&1)==1?0.5f:0f;
  float x0=b.left+(b.width()-side)/2f,y0=b.top+(b.height()-side)/2f;
  for(PlayerIconPaths.Shape shape:PlayerIconPaths.of(type))drawShape(c,shape,x0,y0,u,align);
 }
 void drawShape(Canvas c,PlayerIconPaths.Shape shape,float x0,float y0,float u,float align){
  float[] p=shape.p;if(p==null)return;
  if(shape.kind==PlayerIconPaths.TEXT){fill.setTextAlign(Paint.Align.CENTER);fill.setTextSize(p[2]*u);fill.setTypeface(Typeface.DEFAULT);c.drawText(shape.text,snap(x0+p[0]*u,align),snap(y0+p[1]*u,align),fill);return;}
  if(shape.kind==PlayerIconPaths.OVAL||shape.kind==PlayerIconPaths.FILL_OVAL){c.drawCircle(snap(x0+p[0]*u,align),snap(y0+p[1]*u,align),Math.max(1f,p[2]*u),shape.kind==PlayerIconPaths.FILL_OVAL?fill:stroke);return;}
  if(shape.kind==PlayerIconPaths.ROUND||shape.kind==PlayerIconPaths.FILL_ROUND){RectF r=new RectF(snap(x0+p[0]*u,align),snap(y0+p[1]*u,align),snap(x0+(p[0]+p[2])*u,align),snap(y0+(p[1]+p[3])*u,align));float rad=p[4]*u;c.drawRoundRect(r,rad,rad,shape.kind==PlayerIconPaths.FILL_ROUND?fill:stroke);return;}
  Path path=new Path();path.moveTo(snap(x0+p[0]*u,align),snap(y0+p[1]*u,align));
  for(int i=2;i+1<p.length;i+=2)path.lineTo(snap(x0+p[i]*u,align),snap(y0+p[i+1]*u,align));
  if(shape.kind==PlayerIconPaths.FILL){path.close();c.drawPath(path,fill);}else c.drawPath(path,stroke);
 }
 static float snap(float v,float align){return Math.round(v-align)+align;}
 @Override public void setAlpha(int a){stroke.setAlpha(a);fill.setAlpha(a);}
 @Override public void setColorFilter(ColorFilter f){stroke.setColorFilter(f);fill.setColorFilter(f);}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
