package com.guocheng.mediapocket;
import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.nio.file.Paths;
/** 0.1.171：字幕关闭记忆在冷启动下失效的竞态修复——轨道列表没出来时 current=-1 只是「尚未选择」，轮询既不能跳过下发也不能就此收拍。 */
public class Accept0171Test {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static String src(String name)throws Exception{return new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/"+name)),StandardCharsets.UTF_8);}
 public static void main(String[] args)throws Exception{
  // 冷启动首拍：player 已就绪、getSpuTracks 还是 null，currentSpu 读到 -1。修复前 alreadySpu(-1,-1) 把它当成「已经关闭」，
  // 跳过下发且 userOff 立即收拍，VLC 随后自己挂上片源默认轨——设备实测普通重开 spu=2（英文默认轨），重建路径因暖启动侥幸正确。
  String track=src("TrackSelection.java"),auto=src("SubtitleAuto.java"),memory=src("SubtitleMemory.java");
  check(track.contains("!(step.spu>=0&&raw!=null&&SubtitleAuto.alreadySpu(currentSpu(player),step.spu))"),"the repeated-spu skip only applies to concrete track targets once the list exists");
  check(track.contains("if((userOff&&playPolls>8)||playPolls>24)subtitleDone=true;"),"user-off keeps re-asserting through VLC's whole init window before finishing");
  check(track.contains("if(initial[0]!=null&&video!=null&&video!=initial[0])initial[0]=video;"),"a swapped player view is followed instead of silently killing the poll");
  check(track.contains("0.1.171"),"the race is documented at the fix site");
  // 语义收口：轨道列表在（raw 非 null）且 current 已等于目标时才允许跳过；列表不在时永远补发。vlcPoll 的 userOff 分支保持立即返回 -1。
  SubtitleAuto.Step off=SubtitleAuto.vlcPoll(offPoll());
  check(off.spu==-1&&off.cancelSidecar&&off.finished,"user-off still disables subtitles immediately");
  check(SubtitleAuto.alreadySpu(-1,-1)&&!SubtitleAuto.alreadySpu(2,-1),"alreadySpu keeps its selected-track semantics");
  // 手动选轨记忆（本轮设备实证的三条路径）依赖同一轮询的记忆分支：记住的内嵌轨在列表就绪后命中并收拍。
  SubtitleAuto.Track[] tracks={new SubtitleAuto.Track(-1,"Disable",false,false,false,false),new SubtitleAuto.Track(2,"English Full - [English]",false,false,false,false),new SubtitleAuto.Track(3,"Chinese Full - [Chinese]",false,false,false,false)};
  SubtitleAuto.Step memStep=SubtitleAuto.vlcPoll(poll(tracks,2,"English Full - [English]"));
  check(memStep.spu==2&&memStep.finished,"a remembered manual pick wins over the auto choice once tracks are listed");
  SubtitleAuto.Step pending=SubtitleAuto.vlcPoll(pollEarly(null,2,"English Full - [English]"));
  check(pending.spu==SubtitleAuto.PENDING&&!pending.finished,"a remembered pick keeps polling while the track list is missing");
  // 关闭记忆的持久化形态（设备 prefs 实证）：off 写 disabled=true 并移除 spu/name，embedded 反向写入。
  check(memory.contains("putBoolean(\"subtitleDisabled:\"+id,true).remove(\"subtitleSpu:\"+id).remove(\"subtitleName:\"+id)")&&memory.contains("putInt(\"subtitleSpu:\"+id,spu)"),"off and embedded write complementary per-title keys");
  System.out.println("PASS: subtitle-off survives cold start (poll waits for the spu track list before skipping or finishing).");
 }
 static SubtitleAuto.Poll offPoll(){
  SubtitleAuto.Poll p=new SubtitleAuto.Poll();p.tracks=null;p.pref="zho";p.userOff=true;p.waitedOut=true;p.playing=true;return p;
 }
 static SubtitleAuto.Poll poll(SubtitleAuto.Track[] tracks,int rememberedId,String name){
  SubtitleAuto.Poll p=new SubtitleAuto.Poll();p.tracks=tracks;p.pref="zho";p.rememberedId=rememberedId;p.rememberedName=name==null?"":name;
  p.playing=true;p.waitedOut=true;return p;
 }
 static SubtitleAuto.Poll pollEarly(SubtitleAuto.Track[] tracks,int rememberedId,String name){
  SubtitleAuto.Poll p=poll(tracks,rememberedId,name);p.playing=true;p.waitedOut=false;return p;
 }
}
