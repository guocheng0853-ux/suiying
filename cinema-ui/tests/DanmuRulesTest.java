package com.guocheng.mediapocket;
import java.util.*;
public class DanmuRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args){
  DanmuRules.Config cfg=DanmuRules.Config.defaults();
  check(cfg.shown&&cfg.opacity==80&&cfg.speedSec==8f&&cfg.textSp==16&&cfg.topLines==3&&cfg.scrollLines==8&&cfg.bottomLines==3&&cfg.match.equals("auto")&&cfg.remember,"defaults");
  check(DanmuRules.blocked("这是广告弹幕","广告,测试")&&!DanmuRules.blocked("普通弹幕","广告")&&DanmuRules.blocked("","广告"),"block words");
  List<DanDanPlayRules.Cue> cues=new ArrayList<>();cues.add(new DanDanPlayRules.Cue(0,1,0xffffffff,"滚动"));cues.add(new DanDanPlayRules.Cue(0,5,0xffffffff,"顶部"));cues.add(new DanDanPlayRules.Cue(0,4,0xffffffff,"广告词"));
  DanmuRules.Config filtered=new DanmuRules.Config(true,80,8f,16,3,8,3,"广告","auto",true);
  List<DanmuRules.Draw> drawn=DanmuRules.layout(cues,0,200,400,20f,new DanmuRules.Measure(){public float width(String text){return text.length()*20f;}},filtered);
  int scroll=0,top=0;for(DanmuRules.Draw d:drawn){if(d.mode==DanmuRules.SCROLL){scroll++;check(d.x==200f,"scroll starts at the right edge");}if(d.mode==DanmuRules.TOP)top++;check(!d.text.contains("广告"),"blocked text is not drawn");}
  check(scroll==1&&top==1,"scroll and top both draw");
  List<DanDanPlayRules.Cue> crowd=new ArrayList<>();for(int i=0;i<5;i++)crowd.add(new DanDanPlayRules.Cue(0,1,0xffffffff,"同"+i));
  DanmuRules.Config tight=new DanmuRules.Config(true,50,8f,16,0,2,0,"","manual",false);
  check(DanmuRules.layout(crowd,0,300,200,20f,new DanmuRules.Measure(){public float width(String t){return 40f;}},tight).size()==2,"scroll line cap");
  check(DanmuRules.layout(cues,0,200,400,20f,null,new DanmuRules.Config(false,80,8f,16,3,8,3,"","auto",true)).isEmpty(),"hidden draws nothing");
  check(DanmuRules.alpha(80)==204&&new DanmuRules.Config(true,1,1f,8,9,20,9,"","nope",true).opacity==10,"opacity and clamps");
  System.out.println("PASS: danmu visibility, opacity, speed window, line caps, block words.");
 }
}
