package com.guocheng.mediapocket;
import android.content.Context;import android.net.Uri;import android.view.*;import android.widget.*;
import androidx.media3.common.*;import androidx.media3.datasource.*;import androidx.media3.exoplayer.*;import androidx.media3.exoplayer.source.*;import androidx.media3.exoplayer.mediacodec.*;
import java.util.*;
/** App-owned extractor + platform Dolby decoder, rendered to a SurfaceView for HDR. */
final class Media3VideoView extends FrameLayout implements MediaController.MediaPlayerControl {
 interface Prepared {void call(Media3VideoView view);} interface Info {boolean call(Media3VideoView view,int what,int extra);} interface Error {boolean call(Media3VideoView view,int what,int extra);} interface Complete {void call(Media3VideoView view);} interface Size {void call(Media3VideoView view,int width,int height);}
 Prepared prepared;Info info;Error error;Complete complete;Size size;MediaController controller;
 ExoPlayer player;final SurfaceView surface;boolean notified;String pendingSubtitle="";String failure="";int httpStatus;int width,height;boolean strictP5;String aspect="fit";float zoom=1f;android.widget.TextView subtitleView;
 Media3VideoView(Context c){super(c);surface=new SurfaceView(c);addView(surface,new LayoutParams(-1,-1,Gravity.CENTER));setOnClickListener(v->{if(controller!=null)controller.show();});}
 static volatile Boolean p5Cache;
 static void warmDecoder(){Thread t=new Thread(Media3VideoView::supportsP5,"p5-probe");t.setDaemon(true);t.start();}
 static boolean supportsP5(){Boolean cached=p5Cache;if(cached!=null)return cached;synchronized(Media3VideoView.class){if(p5Cache!=null)return p5Cache;p5Cache=probeP5();return p5Cache;}}
 static boolean probeP5(){try{for(android.media.MediaCodecInfo codec:new android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS).getCodecInfos()){if(codec.isEncoder())continue;for(String type:codec.getSupportedTypes())if("video/dolby-vision".equalsIgnoreCase(type))for(android.media.MediaCodecInfo.CodecProfileLevel profile:codec.getCapabilitiesForType(type).profileLevels)if(profile.profile==android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn)return true;}}catch(RuntimeException ignored){}return false;}
 /** 片源自带 User-Agent 时照用。否则用浏览器式标识：部分站点会拒绝 MediaPocket/0.1.3。VLC 与离线下载仍用 OfflineHttp.UA。 */
 static String userAgent(Map<String,String> headers){if(headers!=null)for(Map.Entry<String,String> entry:headers.entrySet())if("User-Agent".equalsIgnoreCase(entry.getKey())&&entry.getValue()!=null&&!entry.getValue().isEmpty())return entry.getValue();return OfflineHttp.BROWSER_UA;}
 void requireDolbyP5(boolean strict){strictP5=strict;}
 void setMediaController(MediaController c){controller=c;c.setMediaPlayer(this);}
 void setOnPreparedListener(Prepared v){prepared=v;}void setOnInfoListener(Info v){info=v;}void setOnErrorListener(Error v){error=v;}void setOnCompletionListener(Complete v){complete=v;}void setOnVideoSizeChangedListener(Size v){size=v;}
 int getVideoWidth(){return width;}int getVideoHeight(){return height;}
 void setVideoURI(Uri uri,Map<String,String> headers){
  stopPlayback();notified=false;pendingSubtitle="";failure="";httpStatus=0;
  String agent=userAgent(headers);Map<String,String> request=headers==null?new HashMap<String,String>():new HashMap<>(headers);
  DefaultDataSource.Factory data=new DefaultDataSource.Factory(getContext(),()->new ScopedHttpDataSource(agent,request));
  DefaultRenderersFactory renderers=new DefaultRenderersFactory(getContext()).setEnableDecoderFallback(true).setMediaCodecSelector((mime,secure,tunnel)->{
   // Profile 5 has no HDR10 base layer: falling back to a plain HEVC decoder produces wrong colours.
   if(strictP5&&MimeTypes.VIDEO_H265.equals(mime))return Collections.emptyList();
   return MediaCodecUtil.getDecoderInfos(mime,secure,tunnel);
  });
  player=new ExoPlayer.Builder(getContext(),renderers).setMediaSourceFactory(new DefaultMediaSourceFactory(data)).build();
  player.setPlaybackSpeed(PlaybackPrefs.speed(getContext().getSharedPreferences("data",0)));
  String language=PlaybackPrefs.subtitle(getContext().getSharedPreferences("data",0));String audio=PlaybackPrefs.audio(getContext().getSharedPreferences("data",0));
  player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredAudioLanguage("auto".equals(audio)?null:audio).setPreferredTextLanguage("eng".equals(language)?"en":"zh").setTrackTypeDisabled(C.TRACK_TYPE_TEXT,"off".equals(language)||getContext().getSharedPreferences("data",0).getBoolean("subtitleDisabled:"+((android.app.Activity)getContext()).getIntent().getStringExtra("watch"),false)).build());
  player.setVideoSurfaceView(surface);
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
  player.addListener(new Player.Listener(){
   @Override public void onVideoSizeChanged(VideoSize v){width=v.width;height=v.height;fit();if(size!=null)size.call(Media3VideoView.this,width,height);}
   @Override public void onTracksChanged(Tracks tracks){if(!pendingSubtitle.isEmpty()){findSubtitle: for(Tracks.Group group:tracks.getGroups())if(group.getType()==C.TRACK_TYPE_TEXT)for(int n=0;n<group.length;n++)if(pendingSubtitle.equals(group.getTrackFormat(n).label)){pendingSubtitle="";selectTrack(C.TRACK_TYPE_TEXT,new Track(group.getMediaTrackGroup(),n,"",false));break findSubtitle;}}for(Tracks.Group group:tracks.getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int i=0;i<group.length;i++)if(group.isTrackSelected(i)){Format f=group.getTrackFormat(i);DebugLog.event("杜比内置","视频格式="+f.sampleMimeType+" · codecs="+f.codecs+" · "+f.width+"×"+f.height);}}
   @Override public void onCues(androidx.media3.common.text.CueGroup group){if(subtitleView!=null){StringBuilder text=new StringBuilder();for(androidx.media3.common.text.Cue cue:group.cues)if(cue.text!=null){if(text.length()>0)text.append("\n");text.append(cue.text);}subtitleView.setText(text.toString());}}
   @Override public void onRenderedFirstFrame(){DebugLog.event("杜比内置","首帧已显示");if(info!=null)info.call(Media3VideoView.this,3,0);}
   @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_READY)HttpRange.landedSeek(player==null?0:player.getCurrentPosition());if(state==Player.STATE_BUFFERING&&info!=null)info.call(Media3VideoView.this,701,0);if(state==Player.STATE_READY){if(info!=null)info.call(Media3VideoView.this,702,0);if(!notified){notified=true;if(prepared!=null)prepared.call(Media3VideoView.this);}}if(state==Player.STATE_ENDED&&complete!=null)complete.call(Media3VideoView.this);}
   @Override public void onPositionDiscontinuity(Player.PositionInfo oldPosition,Player.PositionInfo newPosition,int reason){boolean ready=player!=null&&player.getPlaybackState()==Player.STATE_READY;HttpRange.discontinuitySeek(reason,newPosition==null?0:newPosition.positionMs,ready);}
   @Override public void onPlayerError(PlaybackException e){HttpRange.disarmSeek();failure=e.getErrorCodeName();httpStatus=0;Throwable cause=e;for(int depth=0;cause!=null&&depth<12;depth++,cause=cause.getCause()){if(cause instanceof HttpDataSource.InvalidResponseCodeException)httpStatus=((HttpDataSource.InvalidResponseCodeException)cause).responseCode;if(HttpRange.SKIP_MESSAGE.equals(cause.getMessage()))failure=HttpRange.SKIP_MESSAGE;}DebugLog.event("杜比内置","播放错误 · "+failure+" · HTTP="+httpStatus);if(error!=null)error.call(Media3VideoView.this,e.errorCode,httpStatus);}
  });
  DebugLog.event("杜比内置","Media3 1.8.0 · P5约束="+strictP5+" · 系统P5解码声明="+supportsP5());
  player.setMediaItem(MediaItem.fromUri(uri));player.prepare();
 }
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);fit();}
 void restoreViewport(){requestLayout();post(()->{surface.setTranslationX(0);surface.setTranslationY(0);surface.requestLayout();fit();});}
 void fit(){int[] dimensions=Media3ControlRules.box(getWidth(),getHeight(),width,height,aspect);if(dimensions[0]<=0||dimensions[1]<=0)return;LayoutParams p=(LayoutParams)surface.getLayoutParams();if(p.width!=dimensions[0]||p.height!=dimensions[1]){p.width=dimensions[0];p.height=dimensions[1];p.gravity=Gravity.CENTER;surface.setLayoutParams(p);}surface.setScaleX(zoom);surface.setScaleY(zoom);}
 void aspect(String mode){aspect=mode;zoom=1f;fit();}void zoom(float amount){zoom=FullscreenRules.clamp(amount);fit();}
 void speed(float rate){if(player!=null)player.setPlaybackSpeed(PlaybackPrefs.clampRate(rate));}
 float speed(){return player==null?1f:player.getPlaybackParameters().speed;}
 static final class Track {final androidx.media3.common.TrackGroup group;final int index;final String label;final boolean selected;Track(androidx.media3.common.TrackGroup g,int i,String l,boolean s){group=g;index=i;label=l;selected=s;}}
 List<Track> tracks(int type){List<Track> result=new ArrayList<>();if(player==null)return result;for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==type)for(int i=0;i<group.length;i++){if(!group.isTrackSupported(i))continue;Format f=group.getTrackFormat(i);if(type==C.TRACK_TYPE_TEXT&&("application/pgs".equals(f.sampleMimeType)||"application/vobsub".equals(f.sampleMimeType)||"application/dvbsubs".equals(f.sampleMimeType)))continue;String label=f.label==null?"":f.label;String lang=f.language==null?"未标语言":f.language;String codec=f.codecs==null?"":f.codecs;result.add(new Track(group.getMediaTrackGroup(),i,(label.isEmpty()?lang:label+" · "+lang)+(codec.isEmpty()?"":" · "+codec),group.isTrackSelected(i)));}return result;}
 void selectTrack(int type,Track track){if(player==null)return;androidx.media3.common.TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,track==null);if(track!=null)b.setOverrideForType(new TrackSelectionOverride(track.group,track.index));player.setTrackSelectionParameters(b.build());if(type==C.TRACK_TYPE_TEXT&&subtitleView!=null)subtitleView.setText("");}
 void addSubtitle(String path,String mime,String label){if(player==null||player.getCurrentMediaItem()==null)return;pendingSubtitle=label;long pos=player.getCurrentPosition();boolean play=player.getPlayWhenReady();MediaItem.SubtitleConfiguration sub=new MediaItem.SubtitleConfiguration.Builder(Uri.parse(path.startsWith("/")?new java.io.File(path).toURI().toString():path)).setMimeType(mime).setLanguage("zh").setLabel(label).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();MediaItem item=player.getCurrentMediaItem().buildUpon().setSubtitleConfigurations(Collections.singletonList(sub)).build();player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT).setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).build());player.setMediaItem(item,pos);player.prepare();player.setPlayWhenReady(play);}
 boolean wantsPlayback(){return player!=null&&player.getPlayWhenReady();}
 public void start(){if(player!=null)player.play();}public void pause(){if(player!=null)player.pause();}
 public int getDuration(){return player==null||player.getDuration()==C.TIME_UNSET?0:(int)Math.min(Integer.MAX_VALUE,player.getDuration());}
 public int getCurrentPosition(){return player==null?0:(int)Math.min(Integer.MAX_VALUE,player.getCurrentPosition());}
 public void seekTo(int pos){seekTo(pos,false);}
 /** fromUser 为真时，这次拖动认领的字节请求才受 64 MB 上限约束，直到落到目标或播放失败。续播走 seekTo(int)。 */
 public void seekTo(int pos,boolean fromUser){int at=Math.max(0,pos);if(fromUser){HttpRange.armSeek(at);postDelayed(HttpRange::expireSeek,HttpRange.SeekCap.TIMEOUT_MS);}if(player!=null)player.seekTo(at);}
 public boolean isPlaying(){return player!=null&&player.isPlaying();}
 public int getBufferPercentage(){return player==null?0:player.getBufferedPercentage();}public boolean canPause(){return true;}public boolean canSeekBackward(){return true;}public boolean canSeekForward(){return true;}public int getAudioSessionId(){return player==null?0:player.getAudioSessionId();}
 void stopPlayback(){HttpRange.disarmSeek();if(player!=null){player.release();player=null;}}
}
