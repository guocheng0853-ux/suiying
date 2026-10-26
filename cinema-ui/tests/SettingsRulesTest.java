package com.guocheng.mediapocket;
public class SettingsRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(SettingsRules.place("界面与播放")[0]==-1,"base heading dropped");
  check(SettingsRules.place("VLC 视频解码：auto")[0]==2&&SettingsRules.place("自动换源：开启")[0]==2,"playback group");
  check(SettingsRules.place("弹幕样式与匹配")[0]==4&&SettingsRules.place("弹弹play 弹幕")[0]==4&&SettingsRules.place("Whisper 令牌：未填写")[0]==4,"subtitle/danmu group");
  check(SettingsRules.place("状态栏电量：开")[0]==3,"display group");
  check(SettingsRules.place("多线程加速：开（网盘等大文件 4 连接分段下载）")[0]==5,"network group");
  check(SettingsRules.place("Emby 服务器 · 2 个")[0]==0&&SettingsRules.place("插件管理")[0]==0,"sources group");
  check(SettingsRules.place("某个新按钮")[0]==7,"unknown kept in 其他");
  check(SettingsRules.place("播放器：VLC")[1]<SettingsRules.place("强制高刷：关")[1],"order inside group");
  String[] t=SettingsRules.toggle("多线程加速：开（网盘等大文件 4 连接分段下载）");check(t!=null&&t[0].equals("多线程加速")&&t[1].equals("开")&&t[2].startsWith("网盘"),"toggle with note");
  check("开".equals(SettingsRules.toggle("自动换源：开启")[1])&&"关".equals(SettingsRules.toggle("自动换源：关闭")[1]),"开启/关闭");
  String[] n=SettingsRules.toggle("自动播放下一集：开 · 片尾前 30 秒（长按改提前量）");check(n!=null&&n[1].equals("开"),"auto next toggle");
  check(SettingsRules.toggle("弹幕：未配置")==null&&SettingsRules.toggle("VLC 视频解码：auto")==null&&SettingsRules.toggle("插件管理")==null,"non toggles");
  String[] kv=SettingsRules.split("快进快退：15 秒");check(kv[0].equals("快进快退")&&kv[1].equals("15 秒"),"split");
  // 0.1.147 C1：从服务器管理返回时，概览数量变了要重建；没在「我的」页时不重建
  check(SettingsRules.counts(3,5,0).equals("3/5/0"),"overview counts key");
  check(!SettingsRules.sameCounts(SettingsRules.counts(3,5,0),SettingsRules.counts(4,5,0)),"added server makes the overview stale");
  check(!SettingsRules.sameCounts(SettingsRules.counts(4,5,0),SettingsRules.counts(3,5,0)),"deleted server makes the overview stale");
  check(SettingsRules.sameCounts(SettingsRules.counts(3,5,0),SettingsRules.counts(3,5,0)),"unchanged counts keep the page");
  check(SettingsRules.sameCounts(null,SettingsRules.counts(3,5,0)),"no overview recorded means nothing to refresh");
  try{java.nio.file.Path src=java.nio.file.Paths.get("src/com/guocheng/mediapocket");String main=java.nio.file.Files.readString(src.resolve("ScrapedMainActivity.java")),layout=java.nio.file.Files.readString(src.resolve("SettingsLayout.java"));
   int resume=main.indexOf("@Override protected void onResume()");check(resume>=0&&main.indexOf("refreshSettingsCounts();",resume)>resume&&main.indexOf("refreshSettingsCounts();",resume)<main.indexOf("\n",resume),"onResume re-checks the overview counts");
   check(main.contains("settingsSummary.getParent()!=body")&&main.contains("SettingsLayout.remember(this);preferences();"),"only the visible 我的 page is rebuilt, keeping its scroll position");
   check(layout.contains("a.settingsSummary=card;a.settingsCounts=SettingsRules.counts(servers,fav,off);"),"overview records the counts it showed");
  }catch(java.io.IOException e){throw new AssertionError(e);}
  System.out.println("PASS: settings grouping (7 groups + 其他), toggle detection incl. 开启/关闭 and notes, value split, overview refresh on return.");
 }
}
