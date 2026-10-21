package com.guocheng.mediapocket;
import android.content.Context;import android.net.Uri;import android.view.*;import android.widget.*;
import androidx.media3.common.*;import androidx.media3.datasource.*;import androidx.media3.exoplayer.*;import androidx.media3.exoplayer.source.*;import androidx.media3.exoplayer.mediacodec.*;
import java.util.*;
/** App-owned extractor + platform Dolby decoder, rendered to a SurfaceView for HDR. */
final class Media3VideoView extends FrameLayout implements MediaController.MediaPlayerControl {
 interface Prepared {void call(Media3VideoView view);} interface Info {boolean call(Media3VideoView view,int what,int extra);} interface Error {boolean call(Media3VideoView view,int what,int extra);} interface Complete {void call(Media3VideoView view);} interface Size {void call(Media3VideoView view,int width,int height);}
 Prepared prepared;Info info;Error error;Complete complete;Size size;MediaController controller;
 ExoPlayer player;final SurfaceView surface;boolean notified;String failure="";int width,height;boolean strictP5;
 Media3VideoView(Context c){super(c);surface=new SurfaceView(c);addView(surface,new LayoutParams(-1,-1,Gravity.CENTER));setOnClickListener(v->{if(controller!=null)controller.show();});}
 static boolean supportsP5(){try{for(android.media.MediaCodecInfo codec:new android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS).getCodecInfos()){if(codec.isEncoder())continue;for(String type:codec.getSupportedTypes())if("video/dolby-vision".equalsIgnoreCase(type))for(android.media.MediaCodecInfo.CodecProfileLevel profile:codec.getCapabilitiesForType(type).profileLevels)if(profile.profile==android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn)return true;}}catch(RuntimeException ignored){}return false;}
 void requireDolbyP5(boolean strict){strictP5=strict;}
 void setMediaController(MediaController c){controller=c;c.setMediaPlayer(this);}
 void setOnPreparedListener(Prepared v){prepared=v;}void setOnInfoListener(Info v){info=v;}void setOnErrorListener(Error v){error=v;}void setOnCompletionListener(Complete v){complete=v;}void setOnVideoSizeChangedListener(Size v){size=v;}
 int getVideoWidth(){return width;}int getVideoHeight(){return height;}
 void setVideoURI(Uri uri,Map<String,String> headers){
  stopPlayback();notified=false;
  DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setDefaultRequestProperties(headers);
  DefaultDataSource.Factory data=new DefaultDataSource.Factory(getContext(),http);
  DefaultRenderersFactory renderers=new DefaultRenderersFactory(getContext()).setEnableDecoderFallback(true).setMediaCodecSelector((mime,secure,tunnel)->{
   // Profile 5 has no HDR10 base layer: falling back to a plain HEVC decoder produces wrong colours.
   if(strictP5&&MimeTypes.VIDEO_H265.equals(mime))return Collections.emptyList();
   return MediaCodecUtil.getDecoderInfos(mime,secure,tunnel);
  });
  player=new ExoPlayer.Builder(getContext(),renderers).setMediaSourceFactory(new DefaultMediaSourceFactory(data)).build();
  player.setVideoSurfaceView(surface);
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
  player.addListener(new Player.Listener(){
   @Override public void onVideoSizeChanged(VideoSize v){width=v.width;height=v.height;fit();if(size!=null)size.call(Media3VideoView.this,width,height);}
   @Override public void onTracksChanged(Tracks tracks){for(Tracks.Group group:tracks.getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int i=0;i<group.length;i++)if(group.isTrackSelected(i)){Format f=group.getTrackFormat(i);DebugLog.event("杜比内置","视频格式="+f.sampleMimeType+" · codecs="+f.codecs+" · "+f.width+"×"+f.height);}}
   @Override public void onRenderedFirstFrame(){DebugLog.event("杜比内置","首帧已显示");if(info!=null)info.call(Media3VideoView.this,3,0);}
   @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_BUFFERING&&info!=null)info.call(Media3VideoView.this,701,0);if(state==Player.STATE_READY){if(info!=null)info.call(Media3VideoView.this,702,0);if(!notified){notified=true;if(prepared!=null)prepared.call(Media3VideoView.this);}}if(state==Player.STATE_ENDED&&complete!=null)complete.call(Media3VideoView.this);}
   @Override public void onPlayerError(PlaybackException e){failure=e.getErrorCodeName();DebugLog.event("杜比内置","播放错误 · "+failure);if(error!=null)error.call(Media3VideoView.this,e.errorCode,0);}
  });
  DebugLog.event("杜比内置","Media3 1.8.0 · P5约束="+strictP5+" · 系统P5解码声明="+supportsP5());
  player.setMediaItem(MediaItem.fromUri(uri));player.prepare();
 }
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);fit();}
 void fit(){if(width<=0||height<=0||getWidth()<=0||getHeight()<=0)return;float scale=Math.min(getWidth()/(float)width,getHeight()/(float)height);LayoutParams p=(LayoutParams)surface.getLayoutParams();p.width=Math.round(width*scale);p.height=Math.round(height*scale);p.gravity=Gravity.CENTER;surface.setLayoutParams(p);}
 boolean wantsPlayback(){return player!=null&&player.getPlayWhenReady();}
 public void start(){if(player!=null)player.play();}public void pause(){if(player!=null)player.pause();}
 public int getDuration(){return player==null||player.getDuration()==C.TIME_UNSET?0:(int)Math.min(Integer.MAX_VALUE,player.getDuration());}
 public int getCurrentPosition(){return player==null?0:(int)Math.min(Integer.MAX_VALUE,player.getCurrentPosition());}
 public void seekTo(int pos){if(player!=null)player.seekTo(Math.max(0,pos));}public boolean isPlaying(){return player!=null&&player.isPlaying();}
 public int getBufferPercentage(){return player==null?0:player.getBufferedPercentage();}public boolean canPause(){return true;}public boolean canSeekBackward(){return true;}public boolean canSeekForward(){return true;}public int getAudioSessionId(){return player==null?0:player.getAudioSessionId();}
 void stopPlayback(){if(player!=null){player.release();player=null;}}
}
