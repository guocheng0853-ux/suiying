package com.guocheng.mediapocket;
import org.json.*;
/** 首页海报：竖图判断、底部渐变到页面底色、影视仓文案。不依赖 View。 */
final class HomeHeroRules {
 /** 九个色标，从上到下均分。最后一档是不透明的页面底色。 */
 static final int STOPS=9;
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
 /** 海报底部伸进第一节的高度，用来把接缝藏到标题上方的空白里。 */
 static int underlap(int heroHeight){if(heroHeight<=0)return 0;return Math.max(1,Math.round(heroHeight*.04f));}
 static int posterWidth(int screenWidth){if(screenWidth<=0)return 0;return Math.round(screenWidth*.32f);}
 static int posterHeight(int posterWidth){return Math.round(posterWidth*1.5f);}
 /**
  * 自上而下：上半段透明，下半段加高过渡到页面底色（含氛围取色）。
  * 竖版海报的过渡更早、更实，盖住海报上自带的字，避免和我们的标题叠在一起。
  */
 static int[] blendStops(int pageColor,boolean strongScrim){
  int rgb=pageColor&0xffffff,solid=0xff000000|rgb;
  if(strongScrim)return new int[]{0,0,0,ink(0x38,rgb),ink(0x80,rgb),ink(0xc0,rgb),ink(0xe8,rgb),ink(0xf6,rgb),solid};
  return new int[]{0,0,0,0,0,ink(0x30,rgb),ink(0x88,rgb),ink(0xe0,rgb),solid};
 }
 static int ink(int alpha,int rgb){return (alpha<<24)|(rgb&0xffffff);}
 static int alpha(int color){return (color>>>24)&255;}
}
