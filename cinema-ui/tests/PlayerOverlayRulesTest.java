package com.guocheng.mediapocket;
import org.json.*;
public class PlayerOverlayRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(PlayerOverlayRules.AUDIO.equals(PlayerOverlayRules.bottomAction(0)),"music note is audio");
  check(PlayerOverlayRules.SUBTITLE.equals(PlayerOverlayRules.bottomAction(1)),"CC is subtitles");
  check(PlayerOverlayRules.EPISODES.equals(PlayerOverlayRules.bottomAction(2)),"rightmost icon is episode list");
  check("".equals(PlayerOverlayRules.bottomAction(3)),"no fourth bottom action");
  check("HW+".equals(PlayerOverlayRules.decoderLabel("hardware",false,true)),"hardware pref is HW+");
  check("SW".equals(PlayerOverlayRules.decoderLabel("software",true,false)),"software pref is SW");
  check("HW+".equals(PlayerOverlayRules.decoderLabel("auto",true,false))&&"SW".equals(PlayerOverlayRules.decoderLabel("auto",true,true)),"auto shows the decoder actually in use");
  check("AUTO".equals(PlayerOverlayRules.decoderLabel("auto",false,false)),"auto before playback");
  check("software".equals(PlayerOverlayRules.toggleDecode("hardware",false,false))&&"hardware".equals(PlayerOverlayRules.toggleDecode("software",false,false)),"tap toggles HW and SW");
  check("software".equals(PlayerOverlayRules.toggleDecode("auto",true,false))&&"hardware".equals(PlayerOverlayRules.toggleDecode("auto",true,true)),"auto toggles away from the active decoder");
  check("software".equals(PlayerOverlayRules.toggleDecode("auto",false,false)),"unknown auto tries software");
  check(PlayerOverlayRules.canToggleDecode(true,true)&&!PlayerOverlayRules.canToggleDecode(false,true)&&!PlayerOverlayRules.canToggleDecode(true,false),"toggle needs a connected player");
  check(PlayerOverlayRules.stepSpeed(1f,1)==1.25f&&PlayerOverlayRules.stepSpeed(1f,-1)==0.75f,"pill steps the existing speeds");
  check(PlayerOverlayRules.stepSpeed(2f,1)==2f&&PlayerOverlayRules.stepSpeed(0.5f,-1)==0.5f,"pill stops at 0.5 and 2.0");
  check("1.0x".equals(PlayerOverlayRules.speedPill(1f))&&"0.5x".equals(PlayerOverlayRules.speedPill(0.5f))&&"0.75x".equals(PlayerOverlayRules.speedPill(0.75f))&&"1.25x".equals(PlayerOverlayRules.speedPill(1.25f))&&"1.5x".equals(PlayerOverlayRules.speedPill(1.5f))&&"2.0x".equals(PlayerOverlayRules.speedPill(2f)),"pill reads 1.0x");
  check("00:01 / 23:36".equals(PlayerOverlayRules.clock(1000,23*60*1000+36*1000)),"clock is mm:ss / mm:ss");
  check("1:01:01 / 01:00".equals(PlayerOverlayRules.clock(3661000,60000)),"hour clock stays readable");
  JSONObject series=new JSONObject().put("name","斗破苍穹").put("videos",new JSONArray().put(new JSONObject().put("id","ep54").put("season",1).put("episode",54).put("title","前一集")).put(new JSONObject().put("id","ep55").put("season",1).put("episode",55).put("title","神秘的魂殿")).put(new JSONObject().put("id","ep56").put("season",1).put("episode",56).put("title","后一集")));
  check("55: 神秘的魂殿".equals(PlayerOverlayRules.episodeLine(series,"ep55","斗破苍穹")),"episode line is number and title");
  check("斗破苍穹".equals(PlayerOverlayRules.seriesTitle("斗破苍穹 · S1E55 · 神秘的魂殿",new JSONObject().put("emby.line","S1E55 · 神秘的魂殿"))),"top title drops the episode suffix");
  check("55: 神秘的魂殿".equals(PlayerOverlayRules.embyLine("S1E55 · 神秘的魂殿")),"emby line");
  check("ep54".equals(PlayerOverlayRules.neighbor(series,"ep55",-1))&&"ep56".equals(PlayerOverlayRules.neighbor(series,"ep55",1)),"prev and next from the episode list");
  check(!PlayerOverlayRules.showNeighbor(PlayerOverlayRules.neighbor(series,"ep54",-1))&&PlayerOverlayRules.showNeighbor(PlayerOverlayRules.neighbor(series,"ep54",1)),"hide the missing neighbor");
  check(!PlayerOverlayRules.showEpisodes(0,false,true)&&PlayerOverlayRules.showEpisodes(0,true,true)&&PlayerOverlayRules.showEpisodes(3,true,false)&&!PlayerOverlayRules.showEpisodes(0,false,false),"episode icon only for a list or a tvbox series");
  check("55".equals(PlayerOverlayRules.tidyEpisode("第55集"))&&"55: 神秘的魂殿".equals(PlayerOverlayRules.tidyEpisode("55 神秘的魂殿"))&&"55: 神秘的魂殿".equals(PlayerOverlayRules.tidyEpisode("55: 神秘的魂殿")),"tvbox episode names");
  check("55".equals(PlayerOverlayRules.fromId("tt0944947:1:55"))&&"".equals(PlayerOverlayRules.fromId("emby:server:item")),"stremio id gives a number, emby id does not");
  check("".equals(PlayerOverlayRules.episodeLine(new JSONObject().put("name","电影"),"movie","电影")),"movies have no episode line");
  check(PlayerOverlayRules.showStatus(false,false,"正在连接…")&&PlayerOverlayRules.showStatus(true,true,"播放结束")&&!PlayerOverlayRules.showStatus(true,false,"播放源 1 / 2 · 诊断很长"),"status only while connecting or finished");
  check(PlayerOverlayRules.retiredBottom("倍速","播放速度")&&PlayerOverlayRules.retiredBottom("比例","画面比例")&&PlayerOverlayRules.retiredBottom("字幕","字幕")&&PlayerOverlayRules.retiredBottom("播放器","播放器")&&PlayerOverlayRules.retiredBottom("更多","更多")&&PlayerOverlayRules.retiredBottom("选集","选集")&&PlayerOverlayRules.retiredBottom("影视仓源 · 48","影视仓源 · 48"),"old bottom pills are retired");
  check(!PlayerOverlayRules.retiredBottom("返回","返回")&&!PlayerOverlayRules.retiredBottom("","画中画"),"back and pip stay");
  System.out.println("PASS: decoder chip, speed pill, episode line, and bottom icons map to audio, subtitles, episodes.");
 }
}
