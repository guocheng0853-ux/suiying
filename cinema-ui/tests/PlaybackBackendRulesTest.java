package com.guocheng.mediapocket;
public class PlaybackBackendRulesTest {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args){
  check(PlaybackBackendRules.softwareAudio(true,false,false,true,false),"HDR/compatible DV with unavailable AC3/TrueHD needs software audio");
  check(!PlaybackBackendRules.softwareAudio(true,true,false,true,false),"P5 must not silently fall back to plain HEVC/VLC for audio");
  check(!PlaybackBackendRules.softwareAudio(true,false,false,true,true),"supported AAC retains the selected platform player");
  check(!PlaybackBackendRules.softwareAudio(true,false,false,false,false),"missing metadata is not proof of an unsupported track");
  check(!PlaybackBackendRules.softwareAudio(false,false,false,true,false),"server-transcoded audio is not inferred from original-track metadata");
  check(!PlaybackBackendRules.softwareAudio(true,false,true,true,false),"an existing VLC playback never restarts for audio");
  check(PlaybackBackendRules.transcodeOnly(true,"p5")&&PlaybackBackendRules.transcodeOnly(true,"dv"),"failed compatibility transcode cannot auto-open incompatible DV");
  check(!PlaybackBackendRules.transcodeOnly(true,"dv-compatible")&&!PlaybackBackendRules.transcodeOnly(false,"p5"),"ordinary compatible routes retain fallback behavior");
  check(PlaybackBackendRules.preferVlc(true,false),"explicit VLC request overrides a false default before creation");
  check(PlaybackBackendRules.preferVlc(false,true),"default VLC remains respected");
  check(!PlaybackBackendRules.preferVlc(false,false),"ordinary platform playback stays selected");
  java.util.Set<String> none=new java.util.HashSet<String>(),eac3=new java.util.HashSet<String>(),truehd=new java.util.HashSet<String>();eac3.add("audio/eac3");truehd.add("audio/true-hd");
  check(PlaybackBackendRules.preferVlcForSource(false,false,"Rick.and.Morty.2160p.EAC3.mkv",false,none),"P2P EAC3 without a platform decoder opens on VLC");
  check(PlaybackBackendRules.preferVlcForSource(false,false,"Show.DDP5.1.mkv",false,none)&&PlaybackBackendRules.preferVlcForSource(false,false,"Show.DD+.mkv",false,none),"DD+ / DDP names are the same cinema codec");
  check(PlaybackBackendRules.preferVlcForSource(false,false,"Movie.AC3.mkv",false,none)&&PlaybackBackendRules.preferVlcForSource(false,false,"Movie.TrueHD.mkv",false,none)&&PlaybackBackendRules.preferVlcForSource(false,false,"Movie.DTS.mkv",false,none),"AC3 TrueHD and DTS names preselect VLC when undecodable");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"Rick.and.Morty.2160p.EAC3.mkv",false,eac3),"a device that can decode EAC3 stays on the system player");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"Movie Dolby Vision Profile 5 HDR10 EAC3",false,none),"profile 5 stays on the system path even when the name also says EAC3");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"电影 2160p 杜比视界 EAC3",false,none),"Chinese Dolby Vision without an HDR10 base is not sent to VLC for audio");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"Show.EAC3.mkv",true,none),"an Emby dolby.kind of p5/dv blocks the name fallback");
  check(PlaybackBackendRules.preferVlcForSource(false,false,"Movie Dolby Vision Profile 8.1 EAC3",false,none),"profile 8 may use VLC when EAC3 has no platform decoder");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"Show.1080p.AAC.mkv",false,none)&&!PlaybackBackendRules.preferVlcForSource(false,false,"plain title",false,none),"AAC and unlabeled audio stay on the current player");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"Show.EAC3.mkv",false,null),"unknown device codecs are not treated as unsupported");
  check(PlaybackBackendRules.preferVlcForSource(true,false,"Movie Dolby Vision Profile 5",true,none),"an explicit VLC extra is unchanged");
  check(PlaybackBackendRules.preferVlcForSource(false,false,"Movie.TrueHD.Atmos.mkv",false,eac3),"TrueHD Atmos is not treated as playable just because EAC3 exists");
  check(!PlaybackBackendRules.preferVlcForSource(false,false,"Movie.TrueHD.Atmos.mkv",false,truehd),"TrueHD Atmos stays on the system player when TrueHD decodes");
  check(PlaybackBackendRules.preferVlcForSource(false,false,"Movie.DD+.Atmos.mkv",false,truehd),"DD+ Atmos still needs EAC3 when the device only has TrueHD");
  String hinted=PlaybackBackendRules.audioHint("站点","", "https://cdn.example/a/Show.EAC3.mkv?token=1","剧集");
  check(hinted.contains("Show.EAC3.mkv")&&PlaybackBackendRules.preferVlcForSource(false,false,hinted,false,none),"a codec token in the URL file name is visible to the fallback");
  check(PlaybackBackendRules.audioHint("","Movie.DTS.mkv","","").contains("Movie.DTS.mkv"),"magnet display names are part of the audio hint");
  check(PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,0,0,true,1,true,false,true,false,true,false,8,false),"rendered SystemPlayback with an undecodable selected track reopens VLC");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,true,false,true,true,0,0,true,1,true,false,true,false,true,false,8,true),"runtime fallback does not pull profile 5 onto VLC");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,true,true,true,0,0,true,1,true,false,true,false,true,false,8,true),"an in-flight VLC switch does not run again");
  check(!PlaybackBackendRules.runtimeAudioFallback(false,false,false,true,true,0,0,true,1,true,false,true,false,true,false,8,true),"Media3 and VLC are not SystemPlayback");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,0,0,true,1,true,false,true,false,true,false,0,true),"a muted music stream is not treated as a missing decoder");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,9000,0,true,1,true,true,false,true,false,true,8,true),"a selected AAC track stays even if another session looks idle");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,1999,0,true,1,false,false,false,false,false,true,8,true),"idle audio under two seconds is still starting");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,2000,0,true,1,false,false,false,false,false,true,8,false),"idle without a cinema mime or name does not reopen VLC");
  check(PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,2000,0,true,1,false,false,false,false,false,true,8,true),"idle plus a cinema name hint reopens VLC");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,9000,0,true,0,false,false,false,false,false,true,8,true),"a video with no audio track is not a codec failure");
  check(PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,false,0,PlaybackBackendRules.AUDIO_NOT_PLAYING,true,1,false,false,false,false,true,false,8,false),"MEDIA_INFO_AUDIO_NOT_PLAYING reopens when no playable track is present");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,0,PlaybackBackendRules.AUDIO_NOT_PLAYING,true,1,false,false,false,true,false,false,8,true),"a transient 804 does not leave a playable AAC track");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,true,true,0,PlaybackBackendRules.AUDIO_NOT_PLAYING,true,2,true,true,false,true,true,false,8,false),"804 is ignored when the selected track is one the device can play");
  check(!PlaybackBackendRules.runtimeAudioFallback(true,false,false,false,false,0,0,true,1,true,false,true,false,true,false,8,true),"do not switch before the picture is up unless audio-not-playing already fired");
  check(PlaybackBackendRules.cinemaHint("Show.EAC3.mkv")&&PlaybackBackendRules.cinemaHint("Movie.TrueHD.Atmos")&&!PlaybackBackendRules.cinemaHint("Show.1080p.AAC.mkv")&&!PlaybackBackendRules.cinemaHint("plain"),"cinema name hints are the idle-path evidence");
  check(PlaybackBackendRules.countsAsOwnMediaPlayer(true,1000,1000,true,PlaybackBackendRules.MEDIA_PLAYER_TYPE)&&!PlaybackBackendRules.countsAsOwnMediaPlayer(true,2000,1000,true,PlaybackBackendRules.MEDIA_PLAYER_TYPE)&&!PlaybackBackendRules.countsAsOwnMediaPlayer(false,1000,1000,true,PlaybackBackendRules.MEDIA_PLAYER_TYPE)&&!PlaybackBackendRules.countsAsOwnMediaPlayer(true,1000,1000,true,1),"only this app's MediaPlayer configs count");
  check(PlaybackBackendRules.idleFromOwnPlayers(1,0)&&!PlaybackBackendRules.idleFromOwnPlayers(1,1)&&!PlaybackBackendRules.idleFromOwnPlayers(0,0),"idle means we saw our own MediaPlayer and none of them started");
  check(!PlaybackBackendRules.audioSwitchSettled(false,0)&&!PlaybackBackendRules.audioSwitchSettled(false,1)&&PlaybackBackendRules.audioSwitchSettled(true,1)&&PlaybackBackendRules.audioSwitchSettled(false,PlaybackBackendRules.AUDIO_SWITCH_ATTEMPTS),"a failed reopen can retry until VLC is up or the cap is hit");
  check(PlaybackBackendRules.cinemaMime("audio/eac3")&&PlaybackBackendRules.cinemaMime("audio/true-hd")&&PlaybackBackendRules.cinemaMime("audio/vnd.dts")&&!PlaybackBackendRules.cinemaMime("audio/mp4a-latm"),"cinema mime list matches the tablet gap");
  System.out.println("PASS: unsupported audio fallback, explicit VLC and strict P5 guards");
 }
}
