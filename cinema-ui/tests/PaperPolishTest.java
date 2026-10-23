package com.guocheng.mediapocket;
/** Tablet cutoff, caption spacing, landscape split padding, and paper contrast. */
public class PaperPolishTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static double luminance(int c){double sum=0;double[] weights={.2126,.7152,.0722};for(int i=0;i<3;i++){double n=((c>>(16-i*8))&255)/255.0;sum+=weights[i]*(n<=.04045?n/12.92:Math.pow((n+.055)/1.055,2.4));}return sum;}
 static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 public static void main(String[] args)throws Exception{
  check(!PaperLayoutRules.tablet(599)&&PaperLayoutRules.tablet(600)&&PaperLayoutRules.tablet(800),"sw600 is the tablet cutoff");
  check(PaperLayoutRules.fadeSpanDp(false,true)==90&&PaperLayoutRules.fadeSpanDp(false,false)==180,"phone fades stay put");
  check(PaperLayoutRules.fadeSpanDp(true,true)==64&&PaperLayoutRules.fadeSpanDp(true,false)==64,"tablet fade is shorter");
  check(PaperLayoutRules.stackCaptionBelow(false,false)&&!PaperLayoutRules.stackCaptionBelow(true,false)&&PaperLayoutRules.stackCaptionBelow(true,true),"only tablet landscape joins the portrait stack");
  check(PaperLayoutRules.sectionSp(false)==22&&PaperLayoutRules.sectionSp(true)>PaperLayoutRules.sectionSp(false),"section titles grow on a tablet");
  check(PaperLayoutRules.posterSp(true)>PaperLayoutRules.posterSp(false)&&PaperLayoutRules.synopsisSp(true)>PaperLayoutRules.synopsisSp(false),"poster names and synopsis grow on a tablet");
  check(PaperLayoutRules.actionsTopDp(true)<PaperLayoutRules.actionsTopDp(false)&&PaperLayoutRules.dotsTopDp(true)<PaperLayoutRules.dotsTopDp(false),"tablet caption gaps are tighter");
  check(PaperLayoutRules.buttonHeightDp(false)>=44&&PaperLayoutRules.captionReserveDp()<214,"buttons stay tappable and the caption reserve shrank");
  int[] same=DetailSplitRules.restore(40,40),diff=DetailSplitRules.restore(24,80);
  check(same[0]==40&&same[1]==40,"equal insets come back equal");
  check(diff[0]==24&&diff[1]==80&&diff[0]!=diff[1],"left inset is not replaced by the right inset");
  int left=DetailSplitRules.leftColumn(1000,300,20,24);
  check(left>300+24,"a large left cutout does not eat the poster");
  check(1000-left-20>=300,"the text column keeps a usable width");
  int balanced=DetailSplitRules.leftColumn(1200,0,0,24);
  check(balanced>24&&balanced<1200*0.5,"without insets the poster stays the smaller column");
  String theme=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/guocheng/mediapocket/CinemaTheme.java")),java.nio.charset.StandardCharsets.UTF_8);
  check(theme.contains("STONE=0xff45423c")&&theme.contains("PROSE=0xff322f2a")&&theme.contains("LINE=0xffa58b74")&&theme.contains("CLAY=0xffd97757")&&theme.contains("CLAY_DEEP=0xffb5573a"),"paper tokens stay on the darker clay palette");
  int paper=0xfffaf9f5,stone=0xff45423c,prose=0xff322f2a,line=0xffa58b74,clay=0xffd97757,deep=0xffb5573a;
  check(contrast(stone,paper)>=7&&contrast(prose,paper)>contrast(stone,paper),"secondary text and synopsis stay readable on paper");
  check(contrast(line,paper)>=3&&contrast(deep,paper)>=4.5&&contrast(0xffffffff,clay)>=3,"borders, action text, and clay buttons clear their backgrounds");
  check(contrast(0xffe8e6dc,paper)<2,"the washed-out caption gray is not a paper color");
  System.out.println("PASS: tablet spacing, short fade, split padding, and paper contrast.");
 }
}
