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
  System.out.println("PASS: settings grouping (7 groups + 其他), toggle detection incl. 开启/关闭 and notes, value split.");
 }
}
