package com.guocheng.mediapocket;
import java.util.*;
import org.json.*;
/** Per-playback fallbacks: audio compatibility must never turn P5 into plain HEVC. */
final class PlaybackBackendRules {
 private PlaybackBackendRules(){}
 /** MediaPlayer info: audio track exists but is not actually playing. */
 static final int AUDIO_NOT_PLAYING=804;
 /** Video is up, but MediaPlayer's audio session is still idle. AAC would have started by now. */
 static final long AUDIO_IDLE_MS=2000;
 /** AudioPlaybackConfiguration player type for android.media.MediaPlayer. */
 static final int MEDIA_PLAYER_TYPE=2;
 /** Give up switching to VLC after this many attempts that never produced a VLC surface. */
 static final int AUDIO_SWITCH_ATTEMPTS=3;
 static boolean softwareAudio(boolean direct,boolean riskyVideo,boolean alreadyVlc,boolean knownCodec,boolean supported){return direct&&!riskyVideo&&!alreadyVlc&&knownCodec&&!supported;}
 static boolean transcodeOnly(boolean transcodeFirst,String dolby){return transcodeFirst&&("p5".equals(dolby)||"dv".equals(dolby));}
 static boolean preferVlc(boolean requested,boolean defaultVlc){return requested||defaultVlc;}
 /**
  * 显式 VLC（本场 extra 或用户默认）优先。否则片源名 / 文件名里写了影院音轨，且这台设备没有对应解码器时，
  * 开播前就走 VLC。杜比视界 P5 / 未知纯 DV 不因音轨改道，留给系统 / Media3。
  */
 static boolean preferVlcForSource(boolean requested,boolean defaultVlc,String hint,boolean riskyKind,Set<String> codecs){return preferVlc(requested,defaultVlc)||namedCinemaAudio(hint,riskyKind,false,codecs);}
 /** 文件名、磁力显示名、地址最后一段和标题拼在一起，给音轨 / 杜比判断用。 */
 static String audioHint(String streamText,String magnetName,String url,String title){return ((streamText==null?"":streamText)+" "+(magnetName==null?"":magnetName)+" "+fileName(url)+" "+(title==null?"":title)).replace('\n',' ').trim();}
 static String fileName(String url){if(url==null||url.isEmpty())return "";String path=url;int q=path.indexOf('?');if(q>=0)path=path.substring(0,q);int slash=path.lastIndexOf('/');if(slash>=0&&slash+1<path.length())path=path.substring(slash+1);try{path=java.net.URLDecoder.decode(path,"UTF-8");}catch(Exception ignored){}return path;}
 /** 名字里能认出的影院音轨，且设备解不了，并且不是会在 VLC 里发紫的杜比视界。codecs 为 null 表示设备能力未知，不强行改道。 */
 static boolean namedCinemaAudio(String hint,boolean riskyKind,boolean alreadyVlc,Set<String> codecs){if(hint==null||hint.isEmpty()||codecs==null||riskyKind||riskyHint(hint))return false;String label=labelOf(hint);return nameAudioFallback(false,alreadyVlc,label,hint,codecs);}
 static boolean riskyHint(String hint){try{return SourceCardRules.vlcRisky(StreamInfo.parse(new JSONObject().put("title",hint==null?"":hint)));}catch(JSONException e){return false;}}
 static String labelOf(String hint){try{return StreamInfo.parse(new JSONObject().put("title",hint==null?"":hint)).audio;}catch(JSONException e){return "";}}
 /** 复用 softwareAudio：已知影院音轨、设备不支持、不是风险画面、当前还不是 VLC。 */
 static boolean nameAudioFallback(boolean riskyVideo,boolean alreadyVlc,String label,String hint,Set<String> codecs){if(codecs==null)return false;boolean known="Atmos".equals(label)||!mimeForLabel(label).isEmpty();return softwareAudio(true,riskyVideo,alreadyVlc,known,deviceSupports(label,hint,codecs));}
 static String mimeForLabel(String label){if(label==null)return "";switch(label){case "DD+":return "audio/eac3";case "AC3":return "audio/ac3";case "TrueHD":return "audio/true-hd";case "DTS":return "audio/vnd.dts";case "DTS-HD":return "audio/vnd.dts.hd";default:return "";}}
 /** Atmos 单独出现时，EAC3 或 TrueHD 任一解码器都先留在系统播放；写明 TrueHD 则只认 TrueHD。 */
 static boolean deviceSupports(String label,String hint,Set<String> types){if(types==null||label==null)return false;if("Atmos".equals(label)){String u=hint==null?"":hint.toUpperCase(Locale.ROOT);boolean truehd=u.contains("TRUEHD"),eac3=u.contains("EAC3")||u.contains("E-AC")||u.contains("DDP")||u.contains("DD+");if(truehd&&!eac3)return types.contains("audio/true-hd");if(eac3&&!truehd)return types.contains("audio/eac3")||types.contains("audio/eac3-joc");return types.contains("audio/true-hd")||types.contains("audio/eac3")||types.contains("audio/eac3-joc");}String mime=mimeForLabel(label);return !mime.isEmpty()&&types.contains(mime);}
 static boolean cinemaMime(String mime){if(mime==null)return false;switch(mime.toLowerCase(Locale.ROOT)){case "audio/ac3":case "audio/eac3":case "audio/eac3-joc":case "audio/true-hd":case "audio/vnd.dts":case "audio/vnd.dts.hd":case "audio/vnd.dts.uhd":return true;default:return false;}}
 /** 文件名 / 标题里能认出影院音轨。空 MIME 的 idle 回退必须有这个，或有 cinema MIME。 */
 static boolean cinemaHint(String hint){String label=labelOf(hint);return "Atmos".equals(label)||!mimeForLabel(label).isEmpty();}
 /** 只认本应用自己的 MediaPlayer。uid 读不到，或是别的应用，都不算。 */
 static boolean countsAsOwnMediaPlayer(boolean uidKnown,int configUid,int appUid,boolean typeKnown,int playerType){return uidKnown&&configUid==appUid&&typeKnown&&playerType==MEDIA_PLAYER_TYPE;}
 /** 看到本应用至少一个 MediaPlayer，且没有一个已经 STARTED。一个都没看到不等于无声。 */
 static boolean idleFromOwnPlayers(int ownSeen,int ownStarted){return ownSeen>0&&ownStarted==0;}
 /** VLC 表面已经换上，或尝试次数用尽，这场就不再重开。失败且还有次数时下一拍再试。 */
 static boolean audioSwitchSettled(boolean vlcConfirmed,int attempts){return vlcConfirmed||attempts>=AUDIO_SWITCH_ATTEMPTS;}
 /**
  * SystemPlayback 有画面但没有声音时，从当前位置重开 VLC。
  * 选中的音轨设备能解，就不改道。P5 / 纯 DV、已经在 VLC、媒体音量为 0，都不改。
  * 没有音轨的片子也不改。音量键静音时 MEDIA_INFO_AUDIO_NOT_PLAYING 也会出现，所以音量为 0 一律不动。
  * idle 只在已经知道是影院音轨（MIME 或文件名）时才用，避免别的应用的空闲 MediaPlayer 把普通片切走。
  */
 static boolean runtimeAudioFallback(boolean systemPlayer,boolean riskyVideo,boolean alreadyVlc,boolean playing,boolean rendered,long sinceRenderMs,int infoWhat,boolean trackInfoKnown,int audioCount,boolean selectionKnown,boolean selectedPlayable,boolean selectedUnsupported,boolean anyPlayable,boolean anyUnsupported,boolean sawIdleMediaPlayer,int musicVolume,boolean cinemaHint){
  if(!systemPlayer||riskyVideo||alreadyVlc||musicVolume<=0)return false;
  if(selectionKnown&&selectedPlayable)return false;
  if(trackInfoKnown&&audioCount==0)return false;
  boolean ready=playing&&rendered;
  if(infoWhat==AUDIO_NOT_PLAYING&&(playing||rendered)&&!anyPlayable)return true;
  if(!ready)return false;
  if(selectionKnown&&selectedUnsupported)return true;
  if(anyUnsupported&&!anyPlayable)return true;
  return sawIdleMediaPlayer&&cinemaHint&&sinceRenderMs>=AUDIO_IDLE_MS&&(!trackInfoKnown||audioCount>0)&&!anyPlayable;
 }
}
