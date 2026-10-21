package com.guocheng.mediapocket;
import java.awt.*;import java.awt.geom.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Host-side preview of the playback volume / brightness indicator. Not packaged into the APK. */
public class RenderLevelHud {
 static final String OUT="/opt/cursor/artifacts";
 public static void main(String[] args)throws Exception{
  new File(OUT).mkdirs();
  Font cjk=Font.createFont(Font.TRUETYPE_FONT,new File("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc")).deriveFont(Font.PLAIN,22f);
  Font emoji=Font.createFont(Font.TRUETYPE_FONT,new File("/tmp/fonts/NotoEmoji-Regular.ttf")).deriveFont(Font.PLAIN,28f);
  GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();ge.registerFont(cjk);ge.registerFont(emoji);
  sheet(cjk,emoji);
  states(cjk);
  placement(cjk);
  icons();
  System.out.println("wrote previews to "+OUT);
 }
 static void sheet(Font cjk,Font emoji)throws Exception{
  int w=1480,h=980;BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=start(im);
  g.setColor(new Color(0xf4f1ea));g.fillRect(0,0,w,h);
  title(g,cjk,"播放时的音量 / 亮度指示",36,48);
  caption(g,cjk,"左边是原来的文字图标，居中浮在画面上。右边是同一组数值的新指示。",36,86);
  label(g,cjk,"调整前",70,140);label(g,cjk,"调整后",790,140);
  frame(g,36,170,680,360,false);oldHud(g,emoji,36+680/2,170+360/2,2f,false,70,100);
  frame(g,760,170,680,360,false);newHud(g,760+LevelHudMetrics.x(680,Math.round(LevelHudMetrics.WIDTH*2f),2f,0,0),170+LevelHudMetrics.top(2f,28,false),2f,false,70,100);
  frame(g,36,560,680,360,true);oldHud(g,emoji,36+680/2,560+360/2,2f,true,65,100);
  frame(g,760,560,680,360,true);newHud(g,760+LevelHudMetrics.x(680,Math.round(LevelHudMetrics.WIDTH*2f),2f,0,0),560+LevelHudMetrics.top(2f,28,false),2f,true,65,100);
  g.dispose();ImageIO.write(im,"png",new File(OUT+"/level-hud-before-after.png"));
 }
 static void states(Font cjk)throws Exception{
  int w=1560,h=620;BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=start(im);
  g.setColor(new Color(0xf4f1ea));g.fillRect(0,0,w,h);
  title(g,cjk,"音量档位、静音、超过 100% 和亮度",36,48);
  String[] names={"静音","音量 40%","音量 70%","音量 160%","亮度 65%"};
  int[] values={0,40,70,160,65};boolean[] bright={false,false,false,false,true};int[] scale={100,100,100,200,100};
  float d=1.2f;int pill=Math.round(LevelHudMetrics.WIDTH*d);
  g.setColor(new Color(0x5c584f));g.setFont(cjk.deriveFont(18f));g.drawString("暗画面",36,96);
  for(int i=0;i<names.length;i++){int x=36+i*304;frame(g,x,114,288,180,false);newHud(g,x+(288-pill)/2,114+58,d,bright[i],values[i],scale[i]);g.setColor(new Color(0x5c584f));g.setFont(cjk.deriveFont(16f));g.drawString(names[i],x,330);}
  g.setColor(new Color(0x5c584f));g.setFont(cjk.deriveFont(18f));g.drawString("亮画面",36,390);
  for(int i=0;i<names.length;i++){int x=36+i*304;frame(g,x,408,288,180,true);newHud(g,x+(288-pill)/2,408+58,d,bright[i],values[i],scale[i]);}
  g.dispose();ImageIO.write(im,"png",new File(OUT+"/level-hud-states.png"));
 }
 static void placement(Font cjk)throws Exception{
  int w=1500,h=860;BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=start(im);
  g.setColor(new Color(0xf4f1ea));g.fillRect(0,0,w,h);
  title(g,cjk,"竖屏和横屏都避开挖孔",36,48);
  int pw=460,ph=820,d=2;frame(g,70,90,pw,ph,false);cutout(g,70+pw/2-54,90,108,34);
  int pill=Math.round(LevelHudMetrics.WIDTH*d);int x=70+LevelHudMetrics.x(pw,pill,d,0,0);int y=90+LevelHudMetrics.top(d,48,true);newHud(g,x,y,d,false,40,100);
  caption(g,cjk,"竖屏 · 音量 40%",70,ph+130);
  int lw=900,lh=420;frame(g,560,180,lw,lh,false);cutout(g,560,180+lh/2-40,36,80);
  int lp=Math.round(LevelHudMetrics.WIDTH*d);int lx=560+LevelHudMetrics.x(lw,lp,d,56,0);int ly=180+LevelHudMetrics.top(d,24,false);newHud(g,lx,ly,d,true,65,100);
  caption(g,cjk,"横屏 · 亮度 65%，左侧挖孔",560,lh+230);
  g.dispose();ImageIO.write(im,"png",new File(OUT+"/level-hud-placement.png"));
 }
 static void icons()throws Exception{
  int w=980,h=360;BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=start(im);
  g.setColor(new Color(0x111318));g.fillRect(0,0,w,h/2);g.setColor(new Color(0xf7f4ee));g.fillRect(0,h/2,w,h/2);
  int[] types={LevelIconGeometry.MUTE,LevelIconGeometry.LOW,LevelIconGeometry.MID,LevelIconGeometry.HIGH,LevelIconGeometry.SUN};
  for(int row=0;row<2;row++)for(int t=0;t<types.length;t++){int base=30+t*190,y=row==0?36:h/2+36;Color ink=row==0?Color.WHITE:new Color(0x1a1c22);icon(g,types[t],base,y,18,ink);icon(g,types[t],base+28,y-6,32,ink);icon(g,types[t],base+70,y-12,48,ink);icon(g,types[t],base+126,y-20,64,ink);}
  g.dispose();ImageIO.write(im,"png",new File(OUT+"/level-hud-icons.png"));
 }
 static void frame(Graphics2D g,int x,int y,int w,int h,boolean bright){
  g.setPaint(bright?new GradientPaint(x,y,new Color(0xfff6ea),x,y+h,new Color(0x8fd0ea)):new GradientPaint(x,y,new Color(0x10131b),x+w,y+h,new Color(0x3c3150)));
  g.fillRoundRect(x,y,w,h,32,32);
  g.setColor(bright?new Color(255,255,255,90):new Color(255,255,255,18));g.fillOval(x+w/5,y+h/4,w/2,h/2);
 }
 static void cutout(Graphics2D g,int x,int y,int w,int h){g.setColor(new Color(0x05060a));g.fillRoundRect(x,y,w,h,h,h);}
 static void oldHud(Graphics2D g,Font emoji,int cx,int cy,float d,boolean brightness,int value,int scale){
  int w=Math.round(112*d),h=Math.round(32*d),r=Math.round(8*d);int x=cx-w/2,y=cy-h/2;
  g.setColor(new Color(0x1c,0x1b,0x22,0xcc));g.fillRoundRect(x,y,w,h,r,r);
  g.setFont(emoji.deriveFont(14f*d));g.setColor(Color.WHITE);
  String glyph=brightness?"☀":value<=0?"🔇":"🔊";
  FontMetrics fm=g.getFontMetrics();int gy=y+(h-fm.getHeight())/2+fm.getAscent();g.drawString(glyph,x+Math.round(8*d),gy);
  int trackX=x+Math.round(34*d),trackW=w-Math.round(42*d),trackH=Math.round(12*d),trackY=y+(h-trackH)/2;
  g.setColor(new Color(0xff,0xff,0xff,0x55));g.fillRoundRect(trackX,trackY,trackW,trackH,r,r);
  int primary=scale<=0?0:Math.round(trackW*Math.min(value,100)/100f);
  int extra=value>100&&scale>100?Math.round(trackW*Math.min(value-100,100)/100f):0;
  g.setColor(new Color(0xd7e7f5));g.fillRect(trackX,trackY,Math.max(0,primary),trackH);
  if(extra>0){g.setColor(new Color(0xffb020));g.fillRect(trackX,trackY,extra,trackH);}
 }
 static void newHud(Graphics2D g,int x,int y,float d,boolean brightness,int value,int scale){
  int w=Math.round(LevelHudMetrics.WIDTH*d),h=Math.round(LevelHudMetrics.HEIGHT*d),r=Math.round(LevelHudMetrics.RADIUS*d);
  g.setColor(argb(LevelHudMetrics.PILL));g.fillRoundRect(x,y,w,h,r,r);
  g.setStroke(new BasicStroke(Math.max(1f,d)));g.setColor(argb(LevelHudMetrics.STROKE));g.drawRoundRect(x,y,w-1,h-1,r,r);
  int pad=Math.round(LevelHudMetrics.PAD_H*d),icon=Math.round(LevelHudMetrics.ICON*d),gap=Math.round(LevelHudMetrics.GAP*d),textW=Math.round(LevelHudMetrics.TEXT_W*d);
  int iy=y+(h-icon)/2;icon(g,brightness?LevelIconGeometry.SUN:LevelIconGeometry.volume(value),x+pad,iy,icon,Color.WHITE);
  int bx=x+pad+icon+gap,bw=w-pad-icon-gap-gap-textW-pad,bh=Math.round(LevelHudMetrics.BAR_H*d),by=y+(h-bh)/2;
  Shape old=g.getClip();g.setClip(new RoundRectangle2D.Float(bx,by,bw,bh,bh,bh));
  g.setColor(argb(LevelHudMetrics.TRACK));g.fillRect(bx,by,bw,bh);
  int sc=scale<=0?100:scale;float fill=bw*Math.max(0,Math.min(sc,value))/(float)sc;float mark=sc>100?bw*100f/sc:bw;
  g.setColor(argb(LevelHudMetrics.FILL));g.fillRect(bx,by,Math.round(Math.min(fill,mark)),bh);
  if(fill>mark){g.setColor(argb(LevelHudMetrics.BOOST));g.fillRect(bx+Math.round(mark),by,Math.round(fill-mark),bh);}
  g.setClip(old);
  g.setColor(argb(LevelHudMetrics.FG));g.setFont(new Font("SansSerif",Font.PLAIN,Math.round(14*d)));
  String pct=value+"%";FontMetrics fm=g.getFontMetrics();g.drawString(pct,x+w-pad-textW+(textW-fm.stringWidth(pct))/2f,y+(h-fm.getHeight())/2f+fm.getAscent());
 }
 static void icon(Graphics2D g,int type,int x,int y,int size,Color ink){
  float u=size/24f;Stroke stroke=g.getStroke();g.setStroke(new BasicStroke(Math.max(1f,2f*u),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(ink);
  LevelIconGeometry.draw(type,new LevelIconGeometry.Sink(){
   public void fill(float[] xy){g.fill(path(xy,x,y,u,true));}
   public void stroke(float[] xy){g.draw(path(xy,x,y,u,false));}
   public void circle(float cx,float cy,float r){g.draw(new Ellipse2D.Float(x+(cx-r)*u,y+(cy-r)*u,r*2*u,r*2*u));}
   public void line(float x1,float y1,float x2,float y2){g.draw(new Line2D.Float(x+x1*u,y+y1*u,x+x2*u,y+y2*u));}
  });
  g.setStroke(stroke);
 }
 static Path2D path(float[] xy,float x,float y,float u,boolean close){Path2D p=new Path2D.Float();p.moveTo(x+xy[0]*u,y+xy[1]*u);for(int i=2;i+1<xy.length;i+=2)p.lineTo(x+xy[i]*u,y+xy[i+1]*u);if(close)p.closePath();return p;}
 static void title(Graphics2D g,Font cjk,String s,int x,int y){g.setColor(new Color(0x1a1c22));g.setFont(cjk.deriveFont(Font.BOLD,28f));g.drawString(s,x,y);}
 static void label(Graphics2D g,Font cjk,String s,int x,int y){g.setColor(new Color(0x1a1c22));g.setFont(cjk.deriveFont(Font.BOLD,22f));g.drawString(s,x,y);}
 static void caption(Graphics2D g,Font cjk,String s,int x,int y){g.setColor(new Color(0x5c584f));g.setFont(cjk.deriveFont(18f));g.drawString(s,x,y);}
 static Color argb(int v){return new Color((v>>16)&255,(v>>8)&255,v&255,(v>>>24)&255);}
 static Graphics2D start(BufferedImage im){Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);return g;}
}
