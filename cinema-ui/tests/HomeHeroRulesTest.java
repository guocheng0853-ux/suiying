package com.guocheng.mediapocket;
import org.json.*;
public class HomeHeroRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  JSONObject wide=new JSONObject().put("poster","p").put("background","wide").put("imdbRating","7.8").put("releaseInfo","2026").put("genres",new JSONArray().put("Action"));
  JSONObject tv=new JSONObject().put("fusion.site","xiaoya").put("fusion.source","🌟小雅丨高清").put("poster","same").put("background","same").put("releaseInfo","2026").put("vod_class","剧情").put("name","审判萨达姆 高价值目标");
  JSONObject tvWide=new JSONObject(tv.toString()).put("background","landscape-art");
  JSONObject posterOnly=new JSONObject().put("poster","only");
  check(!HomeHeroRules.portraitHero(wide)&&!HomeHeroRules.tvboxHero(wide),"stremio wide backdrop stays a photo");
  check(HomeHeroRules.portraitHero(tv)&&HomeHeroRules.tvboxHero(tv),"tvbox same art is a portrait hero");
  check(!HomeHeroRules.portraitHero(tvWide)&&HomeHeroRules.tvboxHero(tvWide),"a real tvbox backdrop is not forced into the card");
  check(HomeHeroRules.portraitHero(posterOnly)&&HomeHeroRules.posterOf(posterOnly).equals("only"),"poster only");
  check(!HomeHeroRules.portraitHero(null)&&HomeHeroRules.backdropOf(null).isEmpty(),"null");
  check(HomeRules.heroFacts(wide).contains("★ 7.8")&&HomeRules.heroFacts(wide).contains("2026"),"stremio facts stay");
  check(HomeRules.heroFacts(tv).isEmpty(),"tvbox drops site, quality, year and type");
  check(!HomeRules.heroFacts(tv).contains("影视仓")&&!HomeRules.heroFacts(tv).contains("小雅")&&!HomeRules.heroFacts(tv).contains("2026"),"no meta line");
  check(HomeRules.heroDescription(tv).isEmpty(),"tvbox has no tap hint");
  check(HomeRules.heroDescription(new JSONObject()).contains("点开"),"stremio empty hint stays");
  String synopsis=HomeRules.heroDescription(new JSONObject().put("fusion.site","s").put("vod_content","<p>一段简介</p>"));
  check(synopsis.equals("一段简介"),"tvbox synopsis kept, tags stripped");
  int page=0xffedf5f5;
  int[] plain=HomeHeroRules.blendStops(page,false),strong=HomeHeroRules.blendStops(0x00edf5f5,true);
  check(plain.length==HomeHeroRules.STOPS&&strong.length==HomeHeroRules.STOPS,"nine stops");
  check(HomeHeroRules.alpha(plain[0])==0&&HomeHeroRules.alpha(plain[4])==0,"upper half of a wide hero stays clear");
  check(HomeHeroRules.alpha(plain[5])>0&&HomeHeroRules.alpha(plain[6])>HomeHeroRules.alpha(plain[5])&&HomeHeroRules.alpha(plain[7])>HomeHeroRules.alpha(plain[6]),"tall ramp");
  check(plain[plain.length-1]==page&&strong[strong.length-1]==page,"bottom is the opaque page color");
  check(HomeHeroRules.alpha(strong[3])>HomeHeroRules.alpha(plain[3])&&HomeHeroRules.alpha(strong[5])>HomeHeroRules.alpha(plain[5]),"portrait scrim covers the title sooner");
  check(HomeHeroRules.alpha(strong[0])==0&&HomeHeroRules.alpha(strong[2])==0,"portrait card is above the scrim");
  int tint=0xff2a3140;
  check(HomeHeroRules.blendStops(tint,false)[8]==tint,"atmosphere tint is the bottom stop");
  check(HomeHeroRules.underlap(0)==0&&HomeHeroRules.underlap(1000)==40&&HomeHeroRules.underlap(1000)<1000/8,"underlap tucks the seam under the next section");
  check(HomeHeroRules.posterWidth(1000)==320&&HomeHeroRules.posterHeight(320)==480,"portrait card is 2:3, not a full-bleed crop");
  System.out.println("PASS: hero blend reaches the page color, portrait card, tvbox title without meta or hint.");
 }
}
