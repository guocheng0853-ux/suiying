package com.guocheng.mediapocket;
import java.nio.file.*;import java.util.*;
public final class PlayerGlassTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a)throws Exception{
  TimeZone z=TimeZone.getTimeZone("Asia/Shanghai");Calendar c=Calendar.getInstance(z);c.clear();c.set(2026,9,24,22,13,0);long now=c.getTimeInMillis();
  String r=PlayerGlassRules.remaining(23*60_000L+14_000,52*60_000L+8_000,1f,now,z);
  check(r.equals("剩 29 分，22:41 看完"),"remaining line: "+r);
  check(!r.contains("分钟"),"minute copy dropped the long 分钟");
  check(PlayerGlassRules.remaining(0,90*60_000L,1f,now,z).equals("剩 1 小时 30 分，23:43 看完"),"hours: "+PlayerGlassRules.remaining(0,90*60_000L,1f,now,z));
  check(PlayerGlassRules.remaining(0,120*60_000L,1f,now,z).equals("剩 2 小时，00:13 看完"),"exact hours skip a zero minute");
  check(PlayerGlassRules.remaining(0,60*60_000L,2f,now,z).startsWith("剩 30 分"),"speed shortens remaining");
  check(PlayerGlassRules.remaining(59_000,60_000,1f,now,z).equals("马上看完"),"under a minute");
  check(PlayerGlassRules.remaining(0,0,1f,now,z).isEmpty()&&PlayerGlassRules.remaining(5,1,1f,now,z).isEmpty(),"unknown duration");
  check(r.contains("剩")&&r.endsWith("看完"),"keeps the 剩…看完 sentence");

  check(PlayerGlassRules.contrast(PlayerGlassRules.WHITE,PlayerGlassRules.composite(PlayerGlassRules.AIRY_ALPHA,255))<PlayerGlassRules.MIN_CONTRAST,"airy glass fails on a white frame");
  int chip=PlayerGlassRules.glassAlpha(255,PlayerGlassRules.WHITE);
  check(chip>=PlayerGlassRules.STRONG_ALPHA&&PlayerGlassRules.contrast(PlayerGlassRules.WHITE,PlayerGlassRules.composite(chip,255))>=4.5,"bright chips at least GLASS_STRONG and 4.5:1");
  check(PlayerGlassRules.glassAlpha(0,PlayerGlassRules.WHITE)==PlayerGlassRules.AIRY_ALPHA,"dark frames keep the airy glass");
  check(PlayerGlassRules.contrast(PlayerGlassRules.WHITE,PlayerGlassRules.composite(PlayerGlassRules.AIRY_ALPHA,0))>=4.5,"white on airy glass over black still passes");
  for(boolean gold:new boolean[]{true,false}){int panel=PlayerGlassRules.panelAlpha(255,gold);int accent=PlayerGlassRules.playbackAccent(gold);
   check(PlayerGlassRules.contrast(PlayerGlassRules.WHITE,PlayerGlassRules.composite(panel,255))>=4.5,"panel white "+gold);
   check(PlayerGlassRules.contrast(PlayerGlassRules.REMAIN_TEXT,PlayerGlassRules.composite(panel,255))>=4.5,"panel remain "+gold);
   check(PlayerGlassRules.contrast(accent,PlayerGlassRules.composite(panel,255))>=4.5,"panel accent "+gold);}
  check(PlayerGlassRules.panelAlpha(0,true)==PlayerGlassRules.AIRY_ALPHA,"dark gold panel stays airy");

  check(PlayerGlassRules.TOUCH_DP>=48&&PlayerLayoutMetrics.TOUCH_DP>=48&&PlayerLayoutMetrics.TOP_H_DP>=48&&PlayerLayoutMetrics.TIME_ROW_DP>=48,"playback rows are at least 48dp");
  check(PlayerGlass.CIRCLE_DP>=48&&PlayerGlass.CENTER_DP>=48&&PlayerGlass.PLAY_DP>=48&&PlayerMotionRules.MENU_ROW_DP>=48,"circles, center keys and menu rows are at least 48dp");

  check(!PlayerGlassRules.seekKeysOn(1_600,1_600)&&PlayerGlassRules.seekKeysOn(1_599,1_600)&&PlayerGlassRules.SEEK_FLASH_MS==1_600L,"seek keys show only inside the flash window");
  check(PlayerGlassRules.showsOnBar("prev")&&PlayerGlassRules.showsOnBar("next")&&PlayerGlassRules.showsOnBar("speed")&&!PlayerGlassRules.showsOnBar("audio"),"bar keeps transport and speed");
  check(PlayerGlassRules.tuckedInMore("audio")&&PlayerGlassRules.tuckedInMore("subtitle")&&PlayerGlassRules.tuckedInMore("episodes")&&!PlayerGlassRules.tuckedInMore("prev"),"tracks, subtitles and episodes move into more");

  check(PlayerGlassRules.playbackAccent(true)==0xffe0b25c,"gold appearance maps playback accent to #e0b25c");
  check(PlayerGlassRules.playbackAccent(false)==PlayerTheme.ACCENT&&PlayerTheme.ACCENT==0xffe6a58d,"clay playback accent stays warm clay");
  String cinema=new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/CinemaTheme.java")),java.nio.charset.StandardCharsets.UTF_8);
  String themeSrc=new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/PlayerTheme.java")),java.nio.charset.StandardCharsets.UTF_8);
  check(cinema.contains("GOLD=0xffe0b25c")&&themeSrc.contains("return PlayerGlassRules.playbackAccent(gold)")&&themeSrc.contains("0x33e0b25c"),"player accent goes through the gold token");

  String media=new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/Media3Controls.java")),java.nio.charset.StandardCharsets.UTF_8);
  String overlay=new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/PlayerOverlay.java")),java.nio.charset.StandardCharsets.UTF_8);
  String glass=new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/PlayerGlass.java")),java.nio.charset.StandardCharsets.UTF_8);
  check(!media.contains("textKey(\"音轨\"")&&!media.contains("textKey(\"字幕\"")&&!media.contains("textKey(\"选集\""),"media3 bar is not a text wall");
  check(media.contains("\"音轨\"")&&media.contains("\"字幕\"")&&media.contains("\"选集\"")&&media.contains("上一集"),"media3 still reaches those actions");
  check(!overlay.contains("mp-slot-")&&overlay.contains("\"音轨\"")&&overlay.contains("\"字幕\"")&&overlay.contains("上一集"),"vlc uses the same overflow, not a second icon row");
  check(!glass.contains("MONOSPACE")&&glass.contains("SANS_SERIF")&&glass.contains("tnum"),"seek labels use tabular sans");
  System.out.println("PASS: player glass remaining copy, bright-scene opacity, 48dp targets, seek-key flash, shared bar language, gold accent.");
 }
}
