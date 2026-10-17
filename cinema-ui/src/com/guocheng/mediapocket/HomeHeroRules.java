package com.guocheng.mediapocket;
import org.json.*;
/** 首页海报：竖图判断、底部渐变到页面底色、影视仓文案。不依赖 View。 */
final class HomeHeroRules {
 /** Alpha-only mask. Clear tail exposes the exact shared backdrop, including its texture. */
 static int[] artworkMask(){return new int[]{0xff000000,0xff000000,0xe0000000,0x80000000,0x20000000,0,0};}
 static float[] fadePositions(boolean portrait){return portrait?new float[]{0,.30f,.43f,.58f,.73f,.90f,1}:new float[]{0,.38f,.50f,.64f,.78f,.92f,1};}
 static boolean tvboxHero(JSONObject m){return m!=null&&m.has("fusion.site");}
 /** 没有独立横版背景时按竖版海报排：模糊底 + 居中竖图。影视仓通常海报和背景是同一张竖图。 */
 static boolean portraitHero(JSONObject m){
  if(m==null)return false;
  String poster=m.optString("poster",""),bg=m.optString("background","");
  if(!bg.isEmpty()&&!poster.isEmpty()&&!bg.equals(poster))return false;
  return tvboxHero(m)||!poster.isEmpty();
 }
 static String backdropOf(JSONObject m){if(m==null)return "";String bg=m.optString("background","");return bg.isEmpty()?m.optString("poster",""):bg;}
 static String posterOf(JSONObject m){if(m==null)return "";String poster=m.optString("poster","");return poster.isEmpty()?m.optString("background",""):poster;}
 static int posterWidth(int screenWidth){if(screenWidth<=0)return 0;return Math.round(screenWidth*.32f);}
 static int posterHeight(int posterWidth){return Math.round(posterWidth*1.5f);}
}
