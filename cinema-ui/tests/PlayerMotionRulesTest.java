package com.guocheng.mediapocket;
import org.json.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** 倍速步进、双击累计、拖动映射、预览二分、图片取整、进度去重、片头存储，以及控件对齐。 */
public class PlayerMotionRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(PlayerOverlayRules.stepSpeed(0.5f,1)==0.6f&&PlayerOverlayRules.stepSpeed(2f,1)==2.25f,"speed steps");
  check(PlayerGestureRules.burstDelta(PlayerGestureRules.RIGHT,PlayerGestureRules.RIGHT,20000,1000)==30000,"taps within 1.3s accumulate");
  check(PlayerGestureRules.burstDelta(PlayerGestureRules.RIGHT,PlayerGestureRules.RIGHT,20000,1301)==10000,"late tap starts over");
  check(PlayerGestureRules.burstDelta(PlayerGestureRules.LEFT,PlayerGestureRules.RIGHT,10000,100)==-10000,"changing side starts over");
  check(PlayerGestureRules.tapHint(30000).equals("+30s")&&PlayerGestureRules.tapHint(-20000).equals("-20s"),"accumulated hint has no space");
  int dur=45*60000;check(PlayerGestureRules.seekSpan(dur)==dur/4&&PlayerGestureRules.seekTarget(600000,dur,500,1000)==937500,"full width maps to a quarter");
  long[] ticks={0L,10_000_000L,20_000_000L};
  check(EmbyApi.thumbnailIndex(ticks,15_000_000L)==1&&EmbyApi.thumbnailIndex(ticks,0L)==0&&EmbyApi.thumbnailIndex(ticks,20_000_000L)==2&&EmbyApi.thumbnailIndex(ticks,-1L)==-1&&EmbyApi.thumbnailIndex(new long[0],1)==-1,"thumbnail binary search");
  JSONArray set=new JSONArray().put(new JSONObject().put("PositionTicks",0).put("ImageTag","a")).put(new JSONObject().put("ImageTag","skip")).put(new JSONObject().put("PositionTicks",10).put("ImageTag","b"));
  check(EmbyApi.thumbnailTicks(set).length==2&&"b".equals(EmbyApi.thumbnailTags(set)[1]),"thumbnail tags follow the same ticks");
  check(EmbyApi.roundUp100(320)==400&&EmbyApi.roundUp100(100)==100&&EmbyApi.roundUp100(0)==100&&EmbyApi.roundUp100(101)==200,"image sizes round up to 100");
  String image=EmbyApi.imageUrl("http://h:8096","item","Primary","tok",320,250,"t");
  check(image.contains("maxWidth=400")&&image.contains("maxHeight=300")&&image.contains("quality=90"),"image url rounds both sides");
  String key=EmbyApi.progressKey("sess",false,1000);check(EmbyApi.sameProgress(key,EmbyApi.progressKey("sess",false,1000))&&!EmbyApi.sameProgress(key,EmbyApi.progressKey("sess",true,1000))&&!EmbyApi.sameProgress(key,EmbyApi.progressKey("sess",false,2000)),"unchanged progress is skipped");
  String store=EmbyCue.writeIntro("{}", "series", 1000L, null);store=EmbyCue.writeIntro(store,"series",null,9000L);
  long[] read=EmbyCue.readIntro(store,"series");check(read[0]==1000&&read[1]==9000,"manual intro keeps the other end");
  check(EmbyCue.readIntro(EmbyCue.clearIntro(store,"series"),"series")[0]==-1,"clearing intro removes it");
  EmbyCue.Span chapters=new EmbyCue.Span(0,5000,-1),bare=new EmbyCue.Span(-1,-1,-1);
  check(EmbyCue.prefer(chapters,1000,9000).introEnd==5000,"chapter markers win");
  check(EmbyCue.prefer(bare,1000,9000).introEnd==9000,"manual marks fill in when chapters have no intro");
  check("3 秒后跳过片头，点击取消".equals(EmbyCue.introCountdown(3)),"countdown copy");
  check(PlayerIconPaths.direction(PlayerIcons.PREV)==-1&&PlayerIconPaths.direction(PlayerIcons.NEXT)==1&&PlayerIconPaths.direction(PlayerIcons.PLAY)==1,"skip icons point the right way and play points right");
  check(!PlayerIconPaths.externalArrow(PlayerIcons.ASPECT)&&PlayerIconPaths.STROKE==2f,"aspect icon is a frame, stroke is 2");
  for(int type=PlayerIcons.PIP;type<=PlayerIcons.LIST;type++)check(PlayerIconPaths.insideBox(type)&&!PlayerIconPaths.of(type).isEmpty(),"icon "+type+" stays inside the 24 box");
  float density=3f;int cut=dp(density,20);
  PlayerLayoutMetrics.Overlay laid=PlayerLayoutMetrics.measure(1920,1080,density,cut,0,cut,0,true,true,true,false,dp(density,56),1080-dp(density,160));
  check(laid.camera.cx()==laid.lock.cx()&&laid.camera.w==laid.lock.w&&laid.camera.w==laid.touch,"camera and lock share a center and a size");
  int gapMid=(laid.camera.y+laid.lock.bottom())/2,spaceMid=(dp(density,56)+(1080-dp(density,160)))/2;check(Math.abs(gapMid-spaceMid)<=1,"side buttons are centered as a group");
  check(laid.camera.x==laid.margin&&1920-(laid.speed.right())==laid.margin,"speed pill uses the same margin as the left column");
  check(laid.prev.cy()==laid.play.cy()&&laid.play.cy()==laid.next.cy()&&laid.play.x-(laid.prev.right())==laid.next.x-(laid.play.right()),"transport sits on one baseline with even gaps");
  PlayerLayoutMetrics.Overlay hidden=PlayerLayoutMetrics.measure(1920,1080,density,0,0,0,0,false,false,false,true,0,0);
  check(hidden.prev==null&&hidden.next==null&&hidden.play.x==hidden.margin,"missing neighbors are not given a slot");
  check(laid.back.h==laid.aspect.h&&laid.aspect.h==laid.pip.h&&laid.pip.h==laid.more.h&&laid.aspect.y==laid.more.y,"top slots share one height");
  check(laid.pip.x-(laid.aspect.right())==laid.more.x-(laid.pip.right()),"top icons are evenly spaced");
  check(laid.seekTrack==dp(density,3)&&hidden.seekTrack==dp(density,4)&&laid.thumb==dp(density,12),"seek track is 3dp, 4dp while dragging, thumb 12dp");
  check(laid.status.cx()==960&&laid.status.y>laid.back.bottom(),"loading status is centered, not in the title row");
  render(laid);
  System.out.println("PASS: speed steps, double-tap accumulation, seek mapping, thumbnail search, image rounding, progress dedupe, intro storage, overlay alignment.");
 }
 static int dp(float density,int v){return PlayerLayoutMetrics.dp(density,v);}
 static void render(PlayerLayoutMetrics.Overlay o)throws Exception{
  int w=1280,h=720;BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
  g.setColor(Color.BLACK);g.fillRect(0,0,w,h);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  float sx=w/1920f,sy=h/1080f;
  box(g,o.back,sx,sy,PlayerIcons.BACK);box(g,o.decoder,sx,sy,-1);box(g,o.aspect,sx,sy,PlayerIcons.ASPECT);box(g,o.pip,sx,sy,PlayerIcons.PIP);box(g,o.more,sx,sy,PlayerIcons.MORE);
  box(g,o.camera,sx,sy,PlayerIcons.CAMERA);box(g,o.lock,sx,sy,PlayerIcons.LOCK);
  g.setColor(new Color(0xC8,0xC8,0xD0,128));round(g,o.speed,sx,sy);g.setColor(Color.WHITE);center(g,"+",o.speed.x,o.speed.y,o.speed.w,o.speed.h/3,sx,sy);center(g,"1.0x",o.speed.x,o.speed.y+o.speed.h/3,o.speed.w,o.speed.h/3,sx,sy);center(g,"−",o.speed.x,o.speed.y+2*o.speed.h/3,o.speed.w,o.speed.h/3,sx,sy);
  g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));g.drawString("第161集 · 名称",(int)(o.margin*sx),(int)(860*sy));
  int trackY=(int)(900*sy);g.setColor(new Color(0x33,0x33,0x33));g.fillRoundRect((int)(220*sx),trackY,(int)(900*sx),Math.max(2,o.seekTrack),4,4);g.setColor(new Color(255,255,255,120));g.fillRoundRect((int)(220*sx),trackY,(int)(700*sx),Math.max(2,o.seekTrack),4,4);g.setColor(Color.WHITE);g.fillRoundRect((int)(220*sx),trackY,(int)(500*sx),Math.max(2,o.seekTrack),4,4);g.fillOval((int)(710*sx)-o.thumb/2,trackY-o.thumb/2,o.thumb,o.thumb);
  box(g,o.prev,sx,sy,PlayerIcons.PREV);box(g,o.play,sx,sy,PlayerIcons.PAUSE);box(g,o.next,sx,sy,PlayerIcons.NEXT);box(g,o.audio,sx,sy,PlayerIcons.NOTE);box(g,o.cc,sx,sy,PlayerIcons.CC);box(g,o.episodes,sx,sy,PlayerIcons.LIST);
  g.setColor(new Color(0x1C,0x1B,0x22));g.fillRoundRect(o.status.x*w/1920,o.status.y*h/1080,o.status.w*w/1920,o.status.h*h/1080,16,16);g.setColor(Color.WHITE);center(g,"播放源 1/1 · 准备连接……",o.status.x,o.status.y,o.status.w,o.status.h,sx,sy);
  g.dispose();
  File out=new File("build/overlay-layout.png");out.getParentFile().mkdirs();ImageIO.write(image,"png",out);
  File art=new File("/opt/cursor/artifacts/overlay-layout.png");if(art.getParentFile()!=null&&(art.getParentFile().isDirectory()||art.getParentFile().mkdirs()))ImageIO.write(image,"png",art);
  check(out.isFile()&&out.length()>1000,"layout harness wrote a picture");
  int pill=image.getRGB((int)((o.speed.x+8)*sx),(int)((o.speed.y+8)*sy));check((pill&0xffffff)!=0,"speed pill background is visible on black");
 }
 static void box(Graphics2D g,PlayerLayoutMetrics.Box b,float sx,float sy,int icon){if(b==null)return;int x=Math.round(b.x*sx),y=Math.round(b.y*sy),w=Math.max(1,Math.round(b.w*sx)),h=Math.max(1,Math.round(b.h*sy));if(icon>=0){int s=Math.min(w,h);int ix=x+(w-s)/2,iy=y+(h-s)/2;drawIcon(g,icon,ix,iy,s);} }
 static void round(Graphics2D g,PlayerLayoutMetrics.Box b,float sx,float sy){g.fillRoundRect(Math.round(b.x*sx),Math.round(b.y*sy),Math.max(1,Math.round(b.w*sx)),Math.max(1,Math.round(b.h*sy)),12,12);}
 static void center(Graphics2D g,String text,int x,int y,int w,int h,float sx,float sy){FontMetrics fm=g.getFontMetrics();int tx=Math.round(x*sx)+(Math.round(w*sx)-fm.stringWidth(text))/2;int ty=Math.round(y*sy)+(Math.round(h*sy)+fm.getAscent())/2-2;g.drawString(text,tx,ty);}
 static void drawIcon(Graphics2D g,int type,int x,int y,int size){
  float u=size/PlayerIconPaths.BOX;g.setStroke(new BasicStroke(Math.max(1f,PlayerIconPaths.STROKE*u),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(Color.WHITE);
  for(PlayerIconPaths.Shape s:PlayerIconPaths.of(type)){
   float[] p=s.p;if(p==null)continue;
   if(s.kind==PlayerIconPaths.TEXT){g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,Math.max(8,Math.round(p[2]*u))));FontMetrics fm=g.getFontMetrics();g.drawString(s.text,x+p[0]*u-fm.stringWidth(s.text)/2f,y+p[1]*u);continue;}
   if(s.kind==PlayerIconPaths.OVAL){g.drawOval(Math.round(x+(p[0]-p[2])*u),Math.round(y+(p[1]-p[2])*u),Math.round(p[2]*2*u),Math.round(p[2]*2*u));continue;}
   if(s.kind==PlayerIconPaths.FILL_OVAL){g.fillOval(Math.round(x+(p[0]-p[2])*u),Math.round(y+(p[1]-p[2])*u),Math.max(1,Math.round(p[2]*2*u)),Math.max(1,Math.round(p[2]*2*u)));continue;}
   if(s.kind==PlayerIconPaths.ROUND||s.kind==PlayerIconPaths.FILL_ROUND){if(s.kind==PlayerIconPaths.FILL_ROUND)g.fillRoundRect(Math.round(x+p[0]*u),Math.round(y+p[1]*u),Math.round(p[2]*u),Math.round(p[3]*u),Math.round(p[4]*u),Math.round(p[4]*u));else g.drawRoundRect(Math.round(x+p[0]*u),Math.round(y+p[1]*u),Math.round(p[2]*u),Math.round(p[3]*u),Math.round(p[4]*u),Math.round(p[4]*u));continue;}
   int n=p.length/2;int[] xs=new int[n],ys=new int[n];for(int i=0;i<n;i++){xs[i]=Math.round(x+p[i*2]*u);ys[i]=Math.round(y+p[i*2+1]*u);}if(s.kind==PlayerIconPaths.FILL)g.fillPolygon(xs,ys,n);else g.drawPolyline(xs,ys,n);
  }
 }
}
