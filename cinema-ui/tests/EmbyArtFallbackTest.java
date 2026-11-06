package com.guocheng.mediapocket;
import org.json.*;
public class EmbyArtFallbackTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x)throws Exception{
  JSONObject server=new JSONObject().put("url","http://h").put("token","t").put("id","s");
  // 单集没有剧照、没有自己的背景图：真实 Emby 返回顶层 SeriesPrimaryImageTag 和数组 ParentBackdropImageTags
  JSONObject ep=new JSONObject().put("Id","ep").put("Type","Episode").put("Name","第 46 集").put("SeriesId","ser").put("SeriesPrimaryImageTag","sp").put("ParentBackdropItemId","ser").put("ParentBackdropImageTags",new JSONArray().put("pb")).put("ImageTags",new JSONObject());
  EmbyApi.ImageRef b=EmbyApi.fallbackImage(ep,"Backdrop");check(b.itemId.equals("ser")&&b.kind.equals("Backdrop")&&b.tag.equals("pb"),"parent backdrop from the tags array");
  EmbyApi.ImageRef p=EmbyApi.fallbackImage(ep,"Primary");check(p.itemId.equals("ser")&&p.kind.equals("Primary")&&p.tag.equals("sp"),"series poster from the top-level tag");
  JSONObject m=EmbyApi.meta(server,ep);check(m.optString("background").contains("/Items/ser/Images/Backdrop"),"detail art uses the series backdrop: "+m.optString("background"));
  // 只有父级缩略图
  JSONObject thumb=new JSONObject().put("Id","ep").put("Type","Episode").put("ParentThumbItemId","ser").put("ParentThumbImageTag","pt");
  b=EmbyApi.fallbackImage(thumb,"Backdrop");check(b.itemId.equals("ser")&&b.kind.equals("Thumb"),"parent thumb as last landscape art");
  // 父级 id 有、tag 没有：仍然去取父级背景，而不是退回空的单集海报
  JSONObject bare=new JSONObject().put("Id","ep").put("Type","Episode").put("SeriesId","ser").put("ParentBackdropItemId","ser");
  check(EmbyApi.meta(server,bare).optString("background").contains("/Items/ser/Images/Backdrop"),"untagged parent backdrop still used");
  check(EmbyApi.fallbackImage(bare,"Primary").itemId.equals("ser"),"no own poster: series poster");
  check(PosterDetailRules.BAR==0xff080706,"poster page bars match the poster ground");
  System.out.println("PASS: Emby episode art falls back to series backdrop / poster / parent thumb; poster page bars dark.");
 }
}
