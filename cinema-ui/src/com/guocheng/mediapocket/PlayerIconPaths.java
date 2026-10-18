package com.guocheng.mediapocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * 24×24 播放图标。线宽固定 2，画进 24dp 的盒子里就是 2dp。
 * 上一集是竖条加向左的三角，下一集是向右的三角加竖条。画面比例是四角框，不是外链箭头。
 */
final class PlayerIconPaths {
 static final float BOX=24f,STROKE=2f;
 static final int POLY=1,FILL=2,ROUND=3,FILL_ROUND=4,OVAL=5,FILL_OVAL=6,TEXT=7;
 static final class Shape {
  final int kind;final float[] p;final String text;
  Shape(int kind,float[] p){this.kind=kind;this.p=p;this.text=null;}
  Shape(String text,float[] p){this.kind=TEXT;this.p=p;this.text=text;}
 }
 private static final List<Shape>[] CACHE=new List[18];
 private PlayerIconPaths(){}
 static List<Shape> of(int type){if(type>0&&type<CACHE.length&&CACHE[type]!=null)return CACHE[type];List<Shape> built=build(type);if(type>0&&type<CACHE.length)CACHE[type]=built;return built;}
 /** 三角尖朝哪边：-1 向左，1 向右，0 不是这种三角。闭合三角会把起点再写一遍，仍然只看前三个顶点。 */
 static int direction(int type){for(Shape s:of(type)){if(s.kind!=POLY&&s.kind!=FILL)continue;int n=s.p==null?0:s.p.length;boolean closed=n==8&&s.p[0]==s.p[6]&&s.p[1]==s.p[7];if(n!=6&&!closed)continue;float x0=s.p[0],x1=s.p[2],x2=s.p[4];float max=Math.max(x0,Math.max(x1,x2)),min=Math.min(x0,Math.min(x1,x2));if(max-min<=1f)continue;int hi=(x0==max?1:0)+(x1==max?1:0)+(x2==max?1:0),lo=(x0==min?1:0)+(x1==min?1:0)+(x2==min?1:0);if(hi==1)return 1;if(lo==1)return -1;}return 0;}
 /** 右上角甩出屏幕的斜箭头。画面比例图标不能长这样。 */
 static boolean externalArrow(int type){for(Shape s:of(type)){if(s.kind!=POLY||s.p.length<4)continue;for(int i=0;i+3<s.p.length;i+=2){float x1=s.p[i],y1=s.p[i+1],x2=s.p[i+2],y2=s.p[i+3];if((x1>20.5f&&y1<4.5f)||(x2>20.5f&&y2<4.5f))return true;}}return false;}
 static boolean insideBox(int type){for(Shape s:of(type)){if(s.p==null)continue;int step=s.kind==ROUND||s.kind==FILL_ROUND?5:s.kind==OVAL||s.kind==FILL_OVAL?3:2;if(s.kind==TEXT)continue;if(s.kind==ROUND||s.kind==FILL_ROUND){float x=s.p[0],y=s.p[1],w=s.p[2],h=s.p[3];if(x<1||y<1||x+w>23||y+h>23)return false;continue;}if(s.kind==OVAL||s.kind==FILL_OVAL){float r=s.p[2];if(s.p[0]-r<1||s.p[1]-r<1||s.p[0]+r>23||s.p[1]+r>23)return false;continue;}for(int i=0;i+1<s.p.length;i+=step){if(s.p[i]<1||s.p[i]>23||s.p[i+1]<1||s.p[i+1]>23)return false;}}return true;}
 private static List<Shape> build(int type){
  List<Shape> s=new ArrayList<>();
  if(type==PlayerIcons.PIP){s.add(new Shape(ROUND,new float[]{3,6,18,12,2}));s.add(new Shape(FILL_ROUND,new float[]{12.5f,11,6,5,1}));}
  else if(type==PlayerIcons.LOCK||type==PlayerIcons.UNLOCK){s.add(new Shape(ROUND,new float[]{6,11,12,9,2}));s.add(new Shape(POLY,type==PlayerIcons.LOCK?new float[]{8,11,8.2f,8.2f,9.2f,5.8f,10.6f,4.4f,12,4,13.4f,4.4f,14.8f,5.8f,15.8f,8.2f,16,11}:new float[]{8,11,8.2f,8.2f,9.2f,5.8f,10.6f,4.4f,12,4,13.4f,4.4f,14.8f,5.8f,15.8f,6.5f}));s.add(new Shape(FILL_OVAL,new float[]{12,15.5f,1.15f}));}
  else if(type==PlayerIcons.BACK){s.add(new Shape(POLY,new float[]{15,6,8,12,15,18}));}
  else if(type==PlayerIcons.ASPECT){s.add(new Shape(POLY,new float[]{4,9,4,4,9,4}));s.add(new Shape(POLY,new float[]{15,4,20,4,20,9}));s.add(new Shape(POLY,new float[]{4,15,4,20,9,20}));s.add(new Shape(POLY,new float[]{15,20,20,20,20,15}));s.add(new Shape(ROUND,new float[]{8,9,8,6,1.2f}));}
  else if(type==PlayerIcons.MORE){s.add(new Shape(FILL_OVAL,new float[]{12,6,1.05f}));s.add(new Shape(FILL_OVAL,new float[]{12,12,1.05f}));s.add(new Shape(FILL_OVAL,new float[]{12,18,1.05f}));}
  else if(type==PlayerIcons.CAMERA){s.add(new Shape(ROUND,new float[]{3.5f,8,17,11,2}));s.add(new Shape(OVAL,new float[]{12,13.2f,2.8f}));s.add(new Shape(POLY,new float[]{9,8,10.4f,5.8f,13.6f,5.8f,15,8}));}
  else if(type==PlayerIcons.PREV){s.add(new Shape(POLY,new float[]{5.5f,6,5.5f,18}));s.add(new Shape(POLY,new float[]{17.2f,6,9.2f,12,17.2f,18,17.2f,6}));}
  else if(type==PlayerIcons.NEXT){s.add(new Shape(POLY,new float[]{6.8f,6,14.8f,12,6.8f,18,6.8f,6}));s.add(new Shape(POLY,new float[]{18.5f,6,18.5f,18}));}
  else if(type==PlayerIcons.PLAY){s.add(new Shape(POLY,new float[]{9,6,9,18,18,12,9,6}));}
  else if(type==PlayerIcons.PAUSE){s.add(new Shape(POLY,new float[]{8.5f,6,8.5f,18}));s.add(new Shape(POLY,new float[]{15.5f,6,15.5f,18}));}
  else if(type==PlayerIcons.NOTE){s.add(new Shape(OVAL,new float[]{8,16.5f,2.4f}));s.add(new Shape(POLY,new float[]{10.4f,16.5f,10.4f,6}));s.add(new Shape(POLY,new float[]{10.4f,6,17.5f,8,17.5f,11,10.4f,9.5f}));}
  else if(type==PlayerIcons.CC){s.add(new Shape(ROUND,new float[]{3.2f,6.4f,17.6f,11.2f,2.4f}));s.add(new Shape("CC",new float[]{12,14.4f,7.2f}));}
  else if(type==PlayerIcons.LIST){s.add(new Shape(POLY,new float[]{5,8,19,8}));s.add(new Shape(POLY,new float[]{5,12,19,12}));s.add(new Shape(POLY,new float[]{5,16,19,16}));}
  else if(type==PlayerIcons.BATTERY){s.add(new Shape(ROUND,new float[]{3,8,16,9,2}));s.add(new Shape(ROUND,new float[]{19.6f,10.4f,1.6f,4.2f,0.7f}));s.add(new Shape(FILL,new float[]{12.6f,9.2f,10.2f,13,12.2f,13,11.2f,16.2f,13.6f,12.4f,11.9f,12.4f,12.9f,9.2f}));}
  return Collections.unmodifiableList(s);
 }
}
