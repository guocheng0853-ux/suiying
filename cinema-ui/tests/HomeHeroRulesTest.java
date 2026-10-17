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
  int[] mask=HomeHeroRules.artworkMask();
  for(boolean portrait:new boolean[]{false,true}){
   float[] positions=HomeHeroRules.fadePositions(portrait);
   check(mask.length==positions.length,"mask stops align");
   check(positions[0]==0&&positions[positions.length-1]==1,"full height");
   for(int i=1;i<mask.length;i++){
    check(positions[i]>positions[i-1],"ordered positions");
    check((mask[i]>>>24)<=(mask[i-1]>>>24),"artwork fades monotonically");
   }
   check((mask[0]>>>24)==255,"top artwork stays opaque");
   check(mask[mask.length-2]==0&&mask[mask.length-1]==0,"clear tail cannot introduce a colour seam");
   check(positions[positions.length-2]<=.92f,"fade completes before the physical edge");
  }
  check(HomeHeroRules.posterWidth(1000)==320&&HomeHeroRules.posterHeight(320)==480,"portrait card is 2:3, not a full-bleed crop");
  System.out.println("PASS: hero alpha mask, clear tail, portrait card and TVBox captions.");
 }
}
