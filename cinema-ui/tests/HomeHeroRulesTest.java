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
  int page=0xffedf5f5,dark=0xff1c1b21;
  int[] plain=HomeHeroRules.blendStops(page,false),strong=HomeHeroRules.blendStops(0x00edf5f5,true),night=HomeHeroRules.blendStops(dark,false);
  check(plain.length==HomeHeroRules.STOPS&&strong.length==HomeHeroRules.STOPS,"nine stops");
  check(HomeHeroRules.alpha(plain[0])==0&&HomeHeroRules.alpha(plain[4])==0,"upper half of a wide hero stays clear");
  check(HomeHeroRules.alpha(plain[5])>0&&HomeHeroRules.alpha(plain[6])>HomeHeroRules.alpha(plain[5]),"text wash before the dissolve");
  check(HomeHeroRules.alpha(plain[7])==0&&HomeHeroRules.alpha(plain[8])==0,"wide scrim is clear along the bottom edge");
  check(HomeHeroRules.alpha(strong[8])==0&&HomeHeroRules.alpha(strong[7])==0&&HomeHeroRules.alpha(night[8])==0,"light and dark, portrait and wide, the edge is clear");
  for(int stop:plain)check((stop&0xffffff)==(page&0xffffff),"wide scrim keeps the page rgb so the fade does not fringe black");
  for(int stop:strong)check((stop&0xffffff)==(page&0xffffff),"portrait scrim keeps the page rgb");
  for(int stop:night)check((stop&0xffffff)==(dark&0xffffff),"dark scrim keeps the dark rgb");
  for(int stop:plain)check(HomeHeroRules.alpha(stop)<255,"scrim never paints an opaque slab");
  for(int stop:strong)check(HomeHeroRules.alpha(stop)<255,"portrait scrim never paints an opaque slab");
  check(HomeHeroRules.alpha(strong[4])>HomeHeroRules.alpha(plain[4])&&HomeHeroRules.alpha(strong[5])>HomeHeroRules.alpha(plain[5]),"portrait scrim covers the baked-in title sooner");
  check(HomeHeroRules.alpha(strong[0])==0&&HomeHeroRules.alpha(strong[1])==0&&HomeHeroRules.alpha(strong[2])>0,"portrait card face stays above the scrim");
  int[] mask=HomeHeroRules.fadeMask();float[] at=HomeHeroRules.fadeMaskPositions();
  check(mask.length==at.length&&HomeHeroRules.alpha(mask[0])==255,"image fade starts opaque");
  for(int i=1;i<mask.length;i++)check(HomeHeroRules.alpha(mask[i])<=HomeHeroRules.alpha(mask[i-1])&&at[i]>at[i-1],"image fade only falls, and keeps moving down");
  check(HomeHeroRules.alpha(mask[mask.length-1])==0&&HomeHeroRules.alpha(mask[mask.length-2])==0,"image edge is fully clear, including the last pixel");
  check(HomeHeroRules.sampleAlpha(plain,null,0f)==0&&HomeHeroRules.sampleAlpha(plain,null,1f)==0,"sampled wide scrim is clear at both ends");
  check(HomeHeroRules.sampleAlpha(strong,null,1f)==0&&HomeHeroRules.sampleAlpha(night,null,1f)==0,"sampled portrait and dark edges are clear");
  check(HomeHeroRules.sampleAlpha(mask,at,0f)==255&&HomeHeroRules.sampleAlpha(mask,at,.95f)==0&&HomeHeroRules.sampleAlpha(mask,at,1f)==0,"mask is clear before the clip");
  int tone=0xffe5e5e4,blurredPage=0xffc8d0d0;
  int opaqueGuess=HomeHeroRules.ink(255,tone);
  check(HomeHeroRules.over(opaqueGuess,blurredPage)!=blurredPage,"an opaque guessed color hides the real page and leaves a hard edge");
  int clearScrim=HomeHeroRules.ink(HomeHeroRules.sampleAlpha(plain,null,1f),page);
  int clearImage=HomeHeroRules.ink(HomeHeroRules.sampleAlpha(mask,at,1f),0x336655);
  check(HomeHeroRules.over(clearScrim,HomeHeroRules.over(clearImage,blurredPage))==blurredPage,"bottom pixel is the real page in front of the hero");
  check(HomeHeroRules.over(HomeHeroRules.ink(0,dark),0xff203040)==0xff203040,"a clear dark scrim does not recolor the page");
  check(HomeHeroRules.captionFadeEnd(1000,650)==650,"fade ends at measured caption top");
  check(HomeHeroRules.captionFadeEnd(1000,420)==420,"larger text moves fade earlier");
  check(HomeHeroRules.captionFadeEnd(1000,-1)==640,"pre-layout fallback");
  int tuck=HomeHeroRules.underlap(1000),fade=Math.round(1000*HomeHeroRules.FADE);
  check(HomeHeroRules.underlap(0)==0&&tuck==0&&tuck<fade,"captions and following sections never overlap");
  check(HomeHeroRules.posterWidth(1000)==320&&HomeHeroRules.posterHeight(320)==480,"portrait card is 2:3, not a full-bleed crop");
  System.out.println("PASS: hero dissolves to transparent over the real page, portrait card, tvbox title without meta or hint.");
 }
}
