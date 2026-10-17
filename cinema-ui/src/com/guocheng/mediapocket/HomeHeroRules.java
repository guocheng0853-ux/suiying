package com.guocheng.mediapocket;
import org.json.*;
/**
 * 首页海报：竖图判断、底部渐隐、影视仓文案。不依赖 View。
 * 底边必须是完全透明，而不是某种猜出来的页面底色。页面真正露出来的是
 * 氛围层（取色底 + 16% 模糊海报），和取色底本身不是同一个颜色。
 */
final class HomeHeroRules {
 /** 九个色标，从上到下均分。每一档只改透明度，RGB 跟氛围色保持一致，避免插值抹成黑边。 */
 static final int STOPS=9;
 /** 海报底部这段高度渐隐到全透明，并伸进下一节。 */
 static final float FADE=.36f;
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
 /** No negative margin: the next section must start below the caption and dots. */
 static int underlap(int heroHeight){return 0;}
 static int captionFadeEnd(int height,int captionTop){return Math.max(1,Math.min(height,captionTop<0?Math.round(height*.64f):captionTop));}
 static int posterWidth(int screenWidth){if(screenWidth<=0)return 0;return Math.round(screenWidth*.32f);}
 static int posterHeight(int posterWidth){return Math.round(posterWidth*1.5f);}
 /**
  * 压暗渐变，只托文字。底边两档 alpha 都是 0（同一 RGB），
  * 不再用实色盖住海报——实色盖不住后面那层模糊海报，底边就会出现一条硬线。
  * 竖版遮罩更早，盖住海报上自带的字。
  */
 static int[] blendStops(int pageColor,boolean strongScrim){
  int rgb=pageColor&0xffffff,clear=ink(0,rgb);
  if(strongScrim)return new int[]{clear,clear,ink(0x28,rgb),ink(0x68,rgb),ink(0xa0,rgb),ink(0x88,rgb),ink(0x40,rgb),clear,clear};
  return new int[]{clear,clear,clear,clear,clear,ink(0x24,rgb),ink(0x48,rgb),clear,clear};
 }
 /** DST_IN 遮罩：渐隐段内从上到下的 alpha。最后两档是 0，底边像素钳制后仍是全透明。 */
 static int[] fadeMask(){return new int[]{0xff000000,0xd0000000,0x60000000,0x00000000,0x00000000};}
 static float[] fadeMaskPositions(){return new float[]{0f,.40f,.70f,.90f,1f};}
 static int ink(int alpha,int rgb){return (alpha<<24)|(rgb&0xffffff);}
 static int alpha(int color){return (color>>>24)&255;}
 static int channel(int color,int shift){return (color>>shift)&255;}
 /** 在渐变上取 alpha。positions 为空时按色标均分。 */
 static int sampleAlpha(int[] colors,float[] positions,float t){
  if(colors==null||colors.length==0)return 0;
  float[] at=positions;
  if(at==null||at.length!=colors.length){at=new float[colors.length];for(int i=0;i<at.length;i++)at[i]=at.length==1?0:i/(float)(at.length-1);}
  if(t<=at[0])return alpha(colors[0]);
  if(t>=at[at.length-1])return alpha(colors[at.length-1]);
  for(int i=1;i<at.length;i++)if(t<=at[i]){float span=at[i]-at[i-1];float f=span<=0?0:(t-at[i-1])/span;return Math.round(alpha(colors[i-1])*(1-f)+alpha(colors[i])*f);}
  return alpha(colors[colors.length-1]);
 }
 /** SRC_OVER。源完全透明时原样返回目标，用来保证底边等于真实页面而不是猜的底色。 */
 static int over(int src,int dst){
  int sa=alpha(src);if(sa<=0)return dst;if(sa>=255)return 0xff000000|(src&0xffffff);
  int da=alpha(dst),inv=255-sa,outA=sa+da*inv/255;if(outA<=0)return 0;
  int r=(channel(src,16)*sa+channel(dst,16)*da*inv/255)/outA;
  int g=(channel(src,8)*sa+channel(dst,8)*da*inv/255)/outA;
  int b=(channel(src,0)*sa+channel(dst,0)*da*inv/255)/outA;
  return (outA<<24)|(r<<16)|(g<<8)|b;
 }
}
