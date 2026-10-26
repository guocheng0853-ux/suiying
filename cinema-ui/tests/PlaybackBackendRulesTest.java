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
  System.out.println("PASS: unsupported audio fallback, explicit VLC and strict P5 guards");
 }
}
