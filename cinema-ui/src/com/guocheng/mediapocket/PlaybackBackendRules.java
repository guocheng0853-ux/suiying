package com.guocheng.mediapocket;
/** Per-playback fallbacks: audio compatibility must never turn P5 into plain HEVC. */
final class PlaybackBackendRules {
 private PlaybackBackendRules(){}
 static boolean softwareAudio(boolean direct,boolean riskyVideo,boolean alreadyVlc,boolean knownCodec,boolean supported){return direct&&!riskyVideo&&!alreadyVlc&&knownCodec&&!supported;}
 static boolean transcodeOnly(boolean transcodeFirst,String dolby){return transcodeFirst&&("p5".equals(dolby)||"dv".equals(dolby));}
 static boolean switchOnce(boolean requested,boolean attempted,boolean alreadyVlc,boolean sourceReady,boolean finishing){return requested&&!attempted&&!alreadyVlc&&sourceReady&&!finishing;}
}
