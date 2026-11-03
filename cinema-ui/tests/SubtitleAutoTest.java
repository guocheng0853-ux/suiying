package com.guocheng.mediapocket;
public class SubtitleAutoTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static SubtitleAuto.Step poll(SubtitleAuto.Track[] tracks,boolean userOff,String pref,boolean rememberedFile,boolean hasSidecar,boolean sidecarPreferred,boolean sidecarVisible,boolean waitedOut,int rememberedId,String name){SubtitleAuto.Poll p=new SubtitleAuto.Poll();p.tracks=tracks;p.userOff=userOff;p.pref=pref;p.rememberedFile=rememberedFile;p.hasSidecar=hasSidecar;p.sidecarPreferred=sidecarPreferred;p.sidecarVisible=sidecarVisible;p.waitedOut=waitedOut;p.rememberedId=rememberedId;p.rememberedName=name==null?"":name;return SubtitleAuto.vlcPoll(p);}
 static SubtitleAuto.Track track(int id,String label,boolean forced,boolean meaningless,boolean commentary,boolean def){return new SubtitleAuto.Track(id,label,forced,meaningless,commentary,def);}
 static SubtitleAuto.Track plain(int id,String label){return track(id,label,false,false,false,false);}
 public static void main(String[] args)throws Exception{
  SubtitleAuto.Track[] zh=new SubtitleAuto.Track[]{plain(1,"繁体中文"),plain(2,"简体中文"),plain(3,"English"),track(4,"Forced 简体",true,false,false,false),track(5,"Commentary",false,false,true,false),track(-1,"关闭",false,false,false,false)};
  check(SubtitleAuto.decide(zh,"zho",true,-1,null,false)==SubtitleAuto.DISABLE,"user off stays off for this video");
  check(SubtitleAuto.decide(zh,"off",false,-1,null,true)==SubtitleAuto.DISABLE,"global off beats a saved external file");
  check(SubtitleAuto.decide(zh,"zho",false,1,"繁体",true)==SubtitleAuto.EXTERNAL,"saved external file beats a remembered embedded track");
  check(SubtitleAuto.decide(zh,"zho",false,1,"繁体",false)==1,"remembered embedded id is kept");
  check(SubtitleAuto.decide(zh,"zho",false,-1,"English",false)==3,"remembered name still wins");
  check(SubtitleAuto.decide(zh,"zho",false,99,"没有这条",false)==2,"missing memory falls through to simplified");
  check(SubtitleAuto.decide(zh,"zho",false,-1,null,false)==2,"simplified before traditional and english");
  check(SubtitleAuto.decide(zh,"hant",false,-1,null,false)==1,"traditional preference");
  check(SubtitleAuto.decide(zh,"eng",false,-1,null,false)==3,"english preference");
  check(SubtitleAuto.decide(zh,"zho",false,-1,null,false)!=4,"forced track is not the automatic choice");
  SubtitleAuto.Track[] onlyForced=new SubtitleAuto.Track[]{track(7,"Forced English",true,false,false,true),track(8,"Songs 歌词",false,true,false,false)};
  check(SubtitleAuto.decide(onlyForced,"eng",false,-1,null,false)==SubtitleAuto.KEEP,"forced and lyrics alone stay off");
  SubtitleAuto.Track[] fallback=new SubtitleAuto.Track[]{plain(1,"日本語"),track(2,"日本語",false,false,false,true),plain(3,"Français")};
  check(SubtitleAuto.decide(fallback,"zho",false,-1,null,false)==2,"without a preferred language, the default track wins");
  SubtitleAuto.Track[] first=new SubtitleAuto.Track[]{plain(4,"日本語"),plain(6,"Français")};
  check(SubtitleAuto.decide(first,"hant",false,-1,null,false)==4,"otherwise the first full embedded track");
  check(SubtitleAuto.decide(null,"zho",false,-1,null,false)==SubtitleAuto.PENDING,"empty decoder list waits");
  check(SubtitleAuto.decide(null,"zho",false,-1,null,true)==SubtitleAuto.EXTERNAL,"remembered external closes embedded before the track list exists");
  check(SubtitleAuto.decide(new SubtitleAuto.Track[0],"zho",false,-1,null,false)==SubtitleAuto.KEEP,"no tracks means nothing to force on");
  check(SubtitleAuto.decide(zh,"zho",true,2,"简体",true)==SubtitleAuto.DISABLE,"turning subtitles off is sticky even if a file was saved");
  SubtitleAuto.Track[] named=SubtitleAuto.fromNames(new int[]{-1,3,4},new String[]{"Disable","Track 1","Track 2"},new String[]{"简体中文","English"});
  check(named!=null&&SubtitleAuto.decide(named,"zho",false,-1,null,false)==3,"unnamed tracks follow the title order and skip the disable row");
  SubtitleAuto.Track[] forcedFull=SubtitleAuto.fromNames(new int[]{1,2},new String[]{"Forced 简体","繁体中文"},null);
  check(SubtitleAuto.decide(forcedFull,"zho",false,-1,null,false)==2,"a full traditional track beats a forced simplified track");
  check(SubtitleAuto.secondary(zh,2,"eng")==3,"second track skips the primary and uses the other preference");
  check(SubtitleAuto.secondary(new SubtitleAuto.Track[]{plain(2,"简体")},2,"zho")==SubtitleAuto.KEEP,"no second dialogue track");
  check(SubtitleAuto.remember(zh,-1,"简体中文")==2,"script match finds simplified when the player label differs");
  SubtitleCueClock.Cue[] cues=new SubtitleCueClock.Cue[]{new SubtitleCueClock.Cue(0,1000,"一"),new SubtitleCueClock.Cue(1000,2000,"二"),null,new SubtitleCueClock.Cue(500,1500,"叠")};
  check(SubtitleCueClock.textAt(cues,500,0).equals("一\n叠"),"overlapping cues stack");
  check(SubtitleCueClock.textAt(cues,1000,0).equals("二\n叠"),"end is exclusive and the next cue starts");
  check(SubtitleCueClock.textAt(cues,200,500).equals(""),"positive offset shows the line later");
  check(SubtitleCueClock.textAt(cues,0,-500).equals("一\n叠"),"negative offset shows the line earlier");
  check(SubtitleCueClock.textAt(null,0,0).isEmpty()&&SubtitleCueClock.textAt(cues,-1,0).isEmpty(),"missing clock or position stays blank");
  SubtitleAuto.Step waiting=poll(null,false,"zho",false,true,false,false,false,-1,null);
  check(!waiting.loadSidecar&&waiting.spu==SubtitleAuto.PENDING&&!waiting.finished,"null track list waits before choosing a sidecar");
  SubtitleAuto.Step missing=poll(null,false,"zho",false,true,false,false,true,-1,null);
  check(missing.loadSidecar&&missing.spu==-1&&missing.finished,"null track list still loads source sidecars and does not open embedded");
  SubtitleAuto.Step remembered=poll(zh,false,"zho",true,true,true,false,false,-1,null);
  check(!remembered.loadSidecar&&remembered.spu==-1,"remembered external closes the embedded track");
  SubtitleAuto.Step rememberedEarly=poll(null,false,"zho",true,true,false,false,false,-1,null);
  check(rememberedEarly.spu==-1&&!rememberedEarly.finished,"remembered external closes embedded even when getSpuTracks is null, and checks again");
  SubtitleAuto.Step picked=poll(zh,false,"zho",false,true,true,false,false,1,"繁体中文");
  check(!picked.loadSidecar&&picked.cancelSidecar&&picked.spu==1,"remembered embedded is not replaced by a source sidecar");
  SubtitleAuto.Step simplified=poll(zh,false,"zho",false,true,false,false,false,-1,null);
  check(!simplified.loadSidecar&&simplified.cancelSidecar&&simplified.spu==2,"preferred embedded beats a non-preferred sidecar");
  SubtitleAuto.Step bothPreferred=poll(zh,false,"zho",false,true,true,false,false,-1,null);
  check(bothPreferred.loadSidecar&&bothPreferred.spu==-1,"a preferred sidecar is used instead of also opening embedded");
  SubtitleAuto.Step englishOnly=poll(new SubtitleAuto.Track[]{plain(3,"English")},false,"zho",false,true,true,false,false,-1,null);
  check(englishOnly.loadSidecar&&englishOnly.spu==-1&&!englishOnly.cancelSidecar,"preferred sidecar is used when embedded is another language, without leaving embedded on");
  SubtitleAuto.Step shown=poll(zh,false,"zho",false,true,false,true,false,-1,null);
  check(!shown.loadSidecar&&shown.spu==-1&&shown.finished,"once a sidecar is showing, embedded stays off");
  check(SubtitleAuto.secondaryUrl("https://cdn.example/a.srt")&&SubtitleAuto.secondaryUrl("http://cdn.example/a.srt")&&!SubtitleAuto.secondaryUrl("file:///sdcard/evil.srt")&&!SubtitleAuto.secondaryUrl("/sdcard/evil.srt")&&SubtitleAuto.secondaryLocal("content://media/1"),"secondary urls are http(s), local only when the user picked one");
  String menu=SubtitleAuto.media3Label("简体","zh","subrip");
  check(menu.equals("简体 · zh · subrip")&&SubtitleAuto.remember(new SubtitleAuto.Track[]{new SubtitleAuto.Track(0,menu,false,false,false,false)},-1,menu)==0,"saved media3 label matches the menu label");
  org.json.JSONObject entry=new org.json.JSONObject().put("url","https://cdn.example/a.srt").put("header",new org.json.JSONObject().put("Referer","https://emby.example/").put("Cookie","sid=1"));
  java.util.Map<String,String> headers=SubtitleAuto.sidecarHeaders(entry);
  check("https://emby.example/".equals(headers.get("Referer"))&&"sid=1".equals(headers.get("Cookie")),"sidecar keeps the entry Referer and Cookie");
  java.util.Map<String,String> kept=SubtitleAuto.safeSecondaryHeaders(headers);
  check("https://emby.example/".equals(kept.get("Referer"))&&!kept.containsKey("Cookie")&&kept.size()==1,"reopen stores Referer, not Cookie");
  SubtitleCueClock.Delayed[] late=new SubtitleCueClock.Delayed[]{SubtitleCueClock.schedule(0,2000,"你好"),SubtitleCueClock.schedule(800,2000,"")};
  check(SubtitleCueClock.showing(late,1000).isEmpty()&&SubtitleCueClock.showing(late,2000).equals("你好")&&SubtitleCueClock.showing(late,2500).equals("你好")&&SubtitleCueClock.showing(late,2800).isEmpty(),"delay longer than one cue still shows that cue");
  System.out.println("PASS: vlcPoll covers null sidecars, remembered embedded, preferred embedded over foreign sidecar, one primary line, cue delay, headers.");
 }
}
