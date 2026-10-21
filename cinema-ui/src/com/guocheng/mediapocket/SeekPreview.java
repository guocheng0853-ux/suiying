package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.BitmapFactory;import android.view.*;import android.widget.*;import org.json.*;
/**
 * 0.1.123 拖动进度条时的画面预览（参考 Findroid / Streamyfin 的 Trickplay）。
 * Emby：先用 ThumbnailSet（按时间点取图），没有再下载 index.bif（缓存到本机）；都没有时只显示时间。
 * 内置杜比播放页直接用；VLC 播放页的进度条通过触摸监听接到原有的 EmbyPlayerUi 预览。
 */
final class SeekPreview {
 final Activity a;final FrameLayout host;LinearLayout box;ImageView image;TextView time;String serverId="",item="",media="";long[] ticks=new long[0];String[] tags=new String[0];BifIndex bif;JellyfinTrickplay jelly;int lastMs=-1;
 volatile String flight="";JSONObject bound;
 SeekPreview(Activity a,FrameLayout host){this.a=a;this.host=host;}
 int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 /** 换片时调用。进行中的加载按条目区分，换片不会被上一次的 loading 挡住。 */
 void bind(String server,String itemId,String mediaSource){if(server==null||itemId==null||itemId.isEmpty()||(itemId.equals(item)&&server.equals(serverId)))return;serverId=server;item=itemId;media=mediaSource==null?"":mediaSource;ticks=new long[0];tags=new String[0];bif=null;jelly=null;load();}
 void load(){final String key=serverId+"\n"+item+"\n"+media;if(key.equals(flight))return;flight=key;bound=null;final String sid=serverId,iid=item,mid=media;PreviewFetch.IO.execute(()->{try{if(!key.equals(flight))return;JSONObject server=EmbyStore.get(a,sid);if(server==null)return;a.runOnUiThread(()->{if(iid.equals(item))bound=server;});
   try{JSONObject set=EmbyClient.thumbnailSet(a,server,iid,mid,320);JSONArray items=set==null?null:set.optJSONArray("Items");if(items==null&&set!=null)items=set.optJSONArray("Thumbnails");long[] t=EmbyApi.thumbnailTicks(items);String[] g=EmbyApi.thumbnailTags(items);if(t.length>0){a.runOnUiThread(()->{if(iid.equals(item)){ticks=t;tags=g;}});return;}}catch(Exception ignored){}
   if(!key.equals(flight))return;JellyfinTrickplay jt=JellyfinTrickplay.load(a,server,iid,mid);if(jt!=null){a.runOnUiThread(()->{if(iid.equals(item))jelly=jt;});return;}
   if(!key.equals(flight))return;BifIndex parsed=PreviewFetch.fetchBif(a,server,sid,iid,mid);if(parsed!=null)a.runOnUiThread(()->{if(iid.equals(item))bif=parsed;});
  }catch(Exception ignored){}finally{if(key.equals(flight))flight="";}});}
 void ensure(){if(box!=null)return;box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(0xff000000);g.setCornerRadius(dp(8));g.setStroke(dp(2),0xccffffff);image.setBackground(g);image.setClipToOutline(true);image.setPadding(dp(2),dp(2),dp(2),dp(2));box.addView(image,new LinearLayout.LayoutParams(dp(176),dp(99)));
  time=new TextView(a);time.setTextColor(0xffffffff);time.setTextSize(13);time.setShadowLayer(dp(3),0,0,0xff000000);time.setPadding(0,dp(4),0,0);box.addView(time);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);lp.bottomMargin=dp(150);host.addView(box,lp);box.setVisibility(View.GONE);}
 /** ms 是预览位置；fraction 是在进度条上的横向位置（0–1），预览框跟着手指走。 */
 void show(int ms,float fraction){ensure();time.setText(PlayerGestureRules.time(ms));boolean has=false;
  if(ticks.length>0){int i=EmbyApi.thumbnailIndex(ticks,EmbyApi.msToTicks(ms));JSONObject server=bound!=null?bound:EmbyStore.get(a,serverId);if(i>=0&&server!=null){TvPosterImages.loadPoster(a,image,EmbyApi.thumbnailImageUrl(server.optString("url"),item,server.optString("token"),ticks[i],media,i<tags.length?tags[i]:""),dp(176));has=true;}}
  else if(jelly!=null){lastMs=ms;jelly.frame(a,ms,bm->{if(bm!=null&&lastMs==ms&&box!=null&&box.getVisibility()==View.VISIBLE){image.setImageBitmap(bm);image.setVisibility(View.VISIBLE);}});has=image.getDrawable()!=null;}
  else if(bif!=null){byte[] jpeg=bif.jpeg(bif.nearest(ms));android.graphics.BitmapFactory.Options opt=new android.graphics.BitmapFactory.Options();opt.inPreferredConfig=android.graphics.Bitmap.Config.RGB_565;android.graphics.Bitmap bm=jpeg==null?null:BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,opt);if(bm!=null){image.setImageBitmap(bm);has=true;}}
  image.setVisibility(has?View.VISIBLE:View.GONE);
  int hostW=host.getWidth()>0?host.getWidth():a.getResources().getDisplayMetrics().widthPixels,boxW=dp(176);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)box.getLayoutParams();lp.leftMargin=Math.max(dp(12),Math.min(hostW-boxW-dp(12),Math.round(hostW*Math.max(0,Math.min(1,fraction))-boxW/2f)));box.setLayoutParams(lp);box.setVisibility(View.VISIBLE);box.bringToFront();}
 void hide(){if(box!=null)box.setVisibility(View.GONE);}

 /** VLC 播放页：在原版进度条上加触摸监听（不替换它自己的拖动逻辑），拖动时显示预览。 */
 static void installVlc(Activity a){try{Object s=OnlineSubtitleSearch.field(a,"seek"),r=OnlineSubtitleSearch.field(a,"root");if(!(s instanceof SeekBar)||!(r instanceof FrameLayout))return;SeekBar seek=(SeekBar)s;if("mp-preview".equals(seek.getTag(R_TAG)))return;seek.setTag(R_TAG,"mp-preview");
  SeekPreview p=new SeekPreview(a,(FrameLayout)r);
  seek.setOnTouchListener((v,e)->{int act=e.getActionMasked();if(act==MotionEvent.ACTION_DOWN||act==MotionEvent.ACTION_MOVE){float w=v.getWidth()-v.getPaddingLeft()-v.getPaddingRight();float f=w<=0?0:(e.getX()-v.getPaddingLeft())/w;f=Math.max(0,Math.min(1,f));JSONObject st=TrackSelection.stream(a);if(st.optString("emby.item").isEmpty()){EmbyPlayerUi.preview(a,Math.round(f*seek.getMax()));}else{p.bind(st.optString("emby.server"),st.optString("emby.item"),st.optString("emby.mediaSource"));p.show(Math.round(f*seek.getMax()),f);}}else if(act==MotionEvent.ACTION_UP||act==MotionEvent.ACTION_CANCEL){p.hide();EmbyPlayerUi.preview(a,-1);}return false;});}catch(Exception ignored){}}
 static final int R_TAG=0x7f0a7301;
}
