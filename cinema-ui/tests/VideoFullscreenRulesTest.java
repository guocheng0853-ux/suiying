package com.guocheng.mediapocket;
public class VideoFullscreenRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static boolean eq(int[] a,int[] b){if(a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;return true;}
 public static void main(String[] args)throws Exception{
  check(!VideoFullscreenRules.tablet(599)&&VideoFullscreenRules.tablet(600)&&VideoFullscreenRules.tablet(807),"shortest side 600dp is the tablet cutoff");
  check(!VideoFullscreenRules.tablet(411),"PJZ110-class phone width stays a phone");
  check(VideoFullscreenRules.immersive(807,false)&&VideoFullscreenRules.immersive(411,true)&&!VideoFullscreenRules.immersive(411,false),"tablet watch and phone landscape are immersive; phone portrait is not");
  check(VideoFullscreenRules.reserveStatusBar(411,true)&&!VideoFullscreenRules.reserveStatusBar(807,true)&&!VideoFullscreenRules.reserveStatusBar(411,false),"only phone portrait reserves the status bar");
  check(eq(VideoFullscreenRules.stage(2120,3000,96,48,807,false,1920,1080),new int[]{0,0,2120,3000}),"OPD2401 portrait still fills the pad window");
  check(eq(VideoFullscreenRules.stage(3000,2120,48,24,807,true,1920,1080),new int[]{0,0,3000,2120}),"OPD2401 landscape fills the window");
  check(eq(VideoFullscreenRules.stage(1080,2400,96,48,411,false,1920,1080),new int[]{0,920,1080,608}),"phone portrait letterboxes under the status bar");
  check(eq(VideoFullscreenRules.stage(2400,1080,96,0,411,true,1920,1080),new int[]{0,0,2400,1080}),"phone landscape stays full-bleed");
  check(VideoFullscreenRules.chromeEdge(120,40,411,true)==120&&VideoFullscreenRules.chromeEdge(120,40,411,false)==40,"phone portrait keeps the status bar; phone landscape uses the cutout");
  check(VideoFullscreenRules.chromeEdge(120,40,807,true)==40&&VideoFullscreenRules.chromeEdge(120,40,807,false)==40,"tablet chrome clears the notch and does not reserve the status bar");
  check(eq(VideoFullscreenRules.dialogWindow(3000,2120,900,1600,false,807),new int[]{3000,2120}),"tablet fullscreen dialog fills the pad, not the centered hole");
  check(eq(VideoFullscreenRules.dialogWindow(3000,2120,1600,2120,true,807),new int[]{1600,2120}),"tablet split dialog fills that window");
  check(eq(VideoFullscreenRules.dialogWindow(1080,2400,1080,2400,false,411),new int[]{-1,-1}),"phone dialog window stays on the existing match-parent path");
  check(VideoFullscreenRules.orientation(false,807,6)==-1&&VideoFullscreenRules.orientation(true,411,6)==-1,"tablet and split windows do not force a rotation");
  check(VideoFullscreenRules.orientation(false,411,6)==6&&VideoFullscreenRules.orientation(false,411,7)==7,"phone still requests the video orientation");
  check(VideoFullscreenRules.clearOf(56,96)==96&&VideoFullscreenRules.clearOf(56,0)==56,"controls stay at least as far in as the cutout");
  String orientation=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlaybackOrientation.java"));
  String overlay=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlayerOverlay.java"));
  String chrome=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlayerChrome.java"));
  String system=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/SystemPlayerActivity.java"));
  String menu=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/CinemaMenu.java"));
  check(orientation.contains("smallestScreenWidthDp")&&orientation.contains("VideoFullscreenRules"),"playback consults the tablet rule before locking landscape");
  check(overlay.contains("chromeInsets")&&chrome.contains("VideoFullscreen.apply")&&system.contains("VideoFullscreen.apply")&&menu.contains("VideoFullscreen.dialog"),"player, system player, and fullscreen dialog use the tablet path");
  System.out.println("PASS: tablet video fills the window immersively; phone portrait letterbox and status-bar chrome stay put.");
 }
}
