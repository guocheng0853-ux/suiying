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
  boolean listBox=false,ccOutline=false,ccText=false,playFill=false,pauseFill=false;
  for(PlayerIconPaths.Shape s:PlayerIconPaths.of(PlayerIcons.LIST))if(s.kind==PlayerIconPaths.ROUND||s.kind==PlayerIconPaths.FILL_ROUND)listBox=true;
  for(PlayerIconPaths.Shape s:PlayerIconPaths.of(PlayerIcons.CC)){if(s.kind==PlayerIconPaths.ROUND)ccOutline=true;if(s.kind==PlayerIconPaths.TEXT&&"CC".equals(s.text))ccText=true;}
  for(PlayerIconPaths.Shape s:PlayerIconPaths.of(PlayerIcons.PLAY))if(s.kind==PlayerIconPaths.FILL)playFill=true;
  for(PlayerIconPaths.Shape s:PlayerIconPaths.of(PlayerIcons.PAUSE))if(s.kind==PlayerIconPaths.FILL||s.kind==PlayerIconPaths.FILL_ROUND)pauseFill=true;
  check(!listBox&&ccOutline&&ccText&&!playFill&&!pauseFill,"list is plain lines, CC is an outlined badge, play and pause are strokes");
  float density=3f;int cut=dp(density,20);
  String title="仙逆",decoder="HW+",episode="第161集 · 名称",speed=PlayerOverlayRules.speedPill(1f);
  String status=PlayerStatus.line(true,true,true,PlayerStatus.clock(21,4),PlayerStatus.net(120L*1024L),PlayerStatus.battery(80,false));
  String time=PlayerOverlayRules.clock(12*60000+34*1000,45*60000);
  int timePx=textWidth(PlayerLayoutMetrics.TIME_SP*density,Font.PLAIN,time);
  int statusPx=textWidth(PlayerLayoutMetrics.STATUS_SP*density,Font.PLAIN,status);
  int decoderPx=textWidth(PlayerLayoutMetrics.DECODER_SP*density,Font.BOLD,decoder)+dp(density,PlayerLayoutMetrics.DECODER_PAD_DP)*2;
  int topBottom=PlayerLayoutMetrics.topBarBottom(density,0),bottomTop=PlayerLayoutMetrics.bottomBarTop(1080,density,0);
  PlayerLayoutMetrics.Overlay laid=PlayerLayoutMetrics.measure(1920,1080,density,cut,0,cut,0,true,true,true,false,topBottom,bottomTop,timePx,statusPx,decoderPx);
  check(laid.camera.cx()==laid.lock.cx()&&laid.camera.w==laid.lock.w&&laid.camera.w==laid.touch,"camera and lock share a center and a size");
  int gapMid=(laid.camera.y+laid.lock.bottom())/2;check(Math.abs(gapMid-(topBottom+bottomTop)/2)<=1,"side buttons are centered as a group");
  check(laid.speed==null&&!PlayerLayoutMetrics.SHOW_SPEED_PILL,"on-screen speed pill is not laid out");
  check(PlayerLayoutMetrics.SPEED_BTN_SP==18&&PlayerLayoutMetrics.SPEED_SP==14&&PlayerLayoutMetrics.EPISODE_SP==14,"speed text is 18 and 14 sp, episode line is 14 sp");
  check(laid.camera.x==laid.margin,"left column uses the safe margin");
  check(laid.prev.cy()==laid.play.cy()&&laid.play.cy()==laid.next.cy()&&laid.play.x-(laid.prev.right())==laid.next.x-(laid.play.right()),"transport sits on one baseline with even gaps");
  PlayerLayoutMetrics.Overlay hidden=PlayerLayoutMetrics.measure(1920,1080,density,0,0,0,0,false,false,false,true,0,0);
  check(hidden.prev==null&&hidden.next==null&&hidden.play.x==hidden.margin,"missing neighbors are not given a slot");
  check(laid.back.h==laid.aspect.h&&laid.aspect.h==laid.pip.h&&laid.pip.h==laid.more.h&&laid.aspect.y==laid.more.y&&laid.title.y==laid.back.y&&laid.topStatus.y==laid.back.y&&laid.decoder.y==laid.back.y,"top slots share one height");
  check(laid.pip.x-(laid.aspect.right())==laid.more.x-(laid.pip.right()),"top icons are evenly spaced");
  check(laid.title.x-laid.back.right()==dp(density,PlayerLayoutMetrics.TITLE_GAP_DP)&&laid.title.w>dp(density,80),"title sits beside the back arrow");
  check(laid.topStatus.w==statusPx&&laid.topStatus.right()<=laid.decoder.x&&laid.decoder.w==decoderPx,"status line and HW+ sit in the top bar");
  check(laid.time.w==timePx&&laid.seek.x==laid.time.right()+dp(density,8)&&laid.seek.right()==1920-laid.margin&&laid.seek.w>1920/2,"time text leads a seek bar that reaches the right safe margin");
  check(hidden.seek.right()==1920-hidden.margin,"seek still ends on the safe margin when neighbors are hidden");
  check(laid.seekTrack==dp(density,3)&&hidden.seekTrack==dp(density,4)&&laid.thumb==dp(density,12),"seek track is 3dp, 4dp while dragging, thumb 12dp");
  check(laid.status.cx()==960&&laid.status.y>laid.back.bottom()&&laid.topStatus.bottom()<=topBottom,"loading status is centered, the clock line stays in the title row");
  check(time.indexOf(" / ")>0&&status.contains("KB/s"),"picture uses the runtime time and status strings");
  render(laid,density,title,decoder,status,episode,time,speed);
  System.out.println("PASS: speed steps, double-tap accumulation, seek mapping, thumbnail search, image rounding, progress dedupe, intro storage, overlay alignment.");
 }
 static int dp(float density,int v){return PlayerLayoutMetrics.dp(density,v);}
 static Font uiFont(int style,float px)throws Exception{Font base;File ttc=new File("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");try{base=ttc.isFile()?Font.createFont(Font.TRUETYPE_FONT,ttc):new Font("WenQuanYi Micro Hei",Font.PLAIN,12);}catch(Exception e){base=new Font("WenQuanYi Micro Hei",Font.PLAIN,12);}return base.deriveFont(style,px);}
 static void hints(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);}
 static int textWidth(float px,int style,String text)throws Exception{BufferedImage image=new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();hints(g);g.setFont(uiFont(style,px));int w=g.getFontMetrics().stringWidth(text);g.dispose();return Math.max(1,w);}
 static void render(PlayerLayoutMetrics.Overlay o,float density,String title,String decoder,String status,String episode,String time,String speed)throws Exception{
  int w=1280,h=720;BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();hints(g);
  GradientPaint backdrop=new GradientPaint(0,0,new Color(0x1A2744),0,h,new Color(0x12303A));g.setPaint(backdrop);g.fillRect(0,0,w,h);
  float sx=w/1920f,sy=h/1080f;g.scale(sx,sy);
  g.setPaint(new GradientPaint(0,0,new Color(0x99,0,0,0),0,o.back.bottom()+dp(density,6),new Color(0,0,0,0)));g.fillRect(0,0,1920,o.back.bottom()+dp(density,6));
  g.setPaint(new GradientPaint(0,o.episode.y-dp(density,12),new Color(0,0,0,0),0,1080,new Color(0xC0,0,0,0)));g.fillRect(0,o.episode.y-dp(density,12),1920,1080);
  icon(g,o.back,PlayerIcons.BACK);textIn(g,title,uiFont(Font.BOLD,PlayerLayoutMetrics.TITLE_SP*density),o.title,false);
  textIn(g,status,uiFont(Font.PLAIN,PlayerLayoutMetrics.STATUS_SP*density),o.topStatus,false);
  textIn(g,decoder,uiFont(Font.BOLD,PlayerLayoutMetrics.DECODER_SP*density),o.decoder,true);
  icon(g,o.aspect,PlayerIcons.ASPECT);icon(g,o.pip,PlayerIcons.PIP);icon(g,o.more,PlayerIcons.MORE);
  icon(g,o.camera,PlayerIcons.CAMERA);icon(g,o.lock,PlayerIcons.LOCK);
  g.setColor(Color.WHITE);
  textIn(g,episode,uiFont(Font.BOLD,PlayerLayoutMetrics.EPISODE_SP*density),o.episode,false);
  textIn(g,time,uiFont(Font.PLAIN,PlayerLayoutMetrics.TIME_SP*density),o.time,false);
  int track=Math.max(2,o.seekTrack),ty=o.seek.y+(o.seek.h-track)/2,played=Math.round(o.seek.w*0.38f),buffered=Math.round(o.seek.w*0.72f);
  g.setColor(new Color(255,255,255,0x33));g.fillRoundRect(o.seek.x,ty,o.seek.w,track,track,track);
  g.setColor(new Color(255,255,255,0x66));g.fillRoundRect(o.seek.x,ty,buffered,track,track,track);
  g.setColor(Color.WHITE);g.fillRoundRect(o.seek.x,ty,played,track,track,track);
  g.fillOval(o.seek.x+played-o.thumb/2,ty+track/2-o.thumb/2,o.thumb,o.thumb);
  icon(g,o.prev,PlayerIcons.PREV);icon(g,o.play,PlayerIcons.PAUSE);icon(g,o.next,PlayerIcons.NEXT);icon(g,o.audio,PlayerIcons.NOTE);icon(g,o.cc,PlayerIcons.CC);icon(g,o.episodes,PlayerIcons.LIST);
  g.dispose();
  File out=new File("build/overlay-layout.png");out.getParentFile().mkdirs();ImageIO.write(image,"png",out);
  File art=new File("/opt/cursor/artifacts/overlay-layout.png");if(art.getParentFile()!=null&&(art.getParentFile().isDirectory()||art.getParentFile().mkdirs()))ImageIO.write(image,"png",art);
  check(out.isFile()&&out.length()>1000,"layout harness wrote a picture");
  check(bright(image,o.title,sx,sy)&&bright(image,o.decoder,sx,sy)&&bright(image,o.topStatus,sx,sy)&&bright(image,o.time,sx,sy)&&bright(image,o.episode,sx,sy),"title, HW+, status, time and episode text are actually drawn");
  int trackX=Math.round((o.seek.right()-8)*sx),trackY=Math.round((ty+track/2f)*sy);
  int gapX=Math.round((o.time.right()+o.seek.x)/2f*sx);
  check(lum(image.getRGB(trackX,trackY))>lum(image.getRGB(gapX,trackY))+12,"seek track continues to the right safe margin");
 }
 static void icon(Graphics2D g,PlayerLayoutMetrics.Box b,int type){if(b==null)return;int box=Math.min(b.w,b.h);int size=Math.round(box*(float)PlayerLayoutMetrics.ICON_DP/PlayerLayoutMetrics.TOUCH_DP);int x=b.x+(b.w-size)/2,y=b.y+(b.h-size)/2;drawIcon(g,type,x,y,size);}
 static void textIn(Graphics2D g,String text,Font font,PlayerLayoutMetrics.Box b,boolean center){if(b==null||text==null)return;g.setFont(font);FontMetrics fm=g.getFontMetrics();int tx=center?b.x+(b.w-fm.stringWidth(text))/2:b.x;int ty=b.y+(b.h-fm.getHeight())/2+fm.getAscent();Shape old=g.getClip();g.setClip(b.x,b.y,Math.max(1,b.w),Math.max(1,b.h));g.setColor(Color.WHITE);g.drawString(text,tx,ty);g.setClip(old);}
 static boolean bright(BufferedImage image,PlayerLayoutMetrics.Box b,float sx,float sy){int x0=Math.max(0,Math.round(b.x*sx)),y0=Math.max(0,Math.round(b.y*sy)),x1=Math.min(image.getWidth()-1,Math.round(b.right()*sx)),y1=Math.min(image.getHeight()-1,Math.round(b.bottom()*sy));for(int y=y0;y<=y1;y+=2)for(int x=x0;x<=x1;x+=2)if(lum(image.getRGB(x,y))>170)return true;return false;}
 static int lum(int rgb){int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;return (r*3+g*6+b)/10;}
 static void drawIcon(Graphics2D g,int type,int x,int y,int size){
  float u=size/PlayerIconPaths.BOX;g.setStroke(new BasicStroke(Math.max(1f,PlayerIconPaths.STROKE*u),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(Color.WHITE);
  for(PlayerIconPaths.Shape s:PlayerIconPaths.of(type)){
   float[] p=s.p;if(p==null)continue;
   if(s.kind==PlayerIconPaths.TEXT){Font font;try{font=uiFont(Font.PLAIN,Math.max(8f,p[2]*u));}catch(Exception e){font=new Font(Font.SANS_SERIF,Font.PLAIN,Math.max(8,Math.round(p[2]*u)));}g.setFont(font);FontMetrics fm=g.getFontMetrics();g.setColor(Color.WHITE);g.drawString(s.text,x+p[0]*u-fm.stringWidth(s.text)/2f,y+p[1]*u);continue;}
   if(s.kind==PlayerIconPaths.OVAL){g.drawOval(Math.round(x+(p[0]-p[2])*u),Math.round(y+(p[1]-p[2])*u),Math.round(p[2]*2*u),Math.round(p[2]*2*u));continue;}
   if(s.kind==PlayerIconPaths.FILL_OVAL){g.fillOval(Math.round(x+(p[0]-p[2])*u),Math.round(y+(p[1]-p[2])*u),Math.max(1,Math.round(p[2]*2*u)),Math.max(1,Math.round(p[2]*2*u)));continue;}
   if(s.kind==PlayerIconPaths.ROUND||s.kind==PlayerIconPaths.FILL_ROUND){if(s.kind==PlayerIconPaths.FILL_ROUND)g.fillRoundRect(Math.round(x+p[0]*u),Math.round(y+p[1]*u),Math.round(p[2]*u),Math.round(p[3]*u),Math.round(p[4]*u),Math.round(p[4]*u));else g.drawRoundRect(Math.round(x+p[0]*u),Math.round(y+p[1]*u),Math.round(p[2]*u),Math.round(p[3]*u),Math.round(p[4]*u),Math.round(p[4]*u));continue;}
   int n=p.length/2;int[] xs=new int[n],ys=new int[n];for(int i=0;i<n;i++){xs[i]=Math.round(x+p[i*2]*u);ys[i]=Math.round(y+p[i*2+1]*u);}if(s.kind==PlayerIconPaths.FILL)g.fillPolygon(xs,ys,n);else g.drawPolyline(xs,ys,n);
  }
 }
}
