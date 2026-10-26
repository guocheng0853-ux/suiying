package com.guocheng.mediapocket;
/** 「我的」分组规则（不依赖 Android，便于测试）。 */
final class SettingsRules {
 private SettingsRules(){}
 static final String[] GROUPS={"来源与插件","首页与外观","播放","播放页显示","字幕与弹幕","网络与存储","关于与故障排查","其他"};
 /** 标签前缀 → 组号。顺序即组内顺序。 */
 static final String[][] RULES={
  {"Emby","0"},{"插件管理","0"},{"插件市场","0"},
  {"外观","1"},{"追剧更新提醒","1"},{"继续观看合并下一集","1"},{"恢复已隐藏的首页分区","1"},
  {"播放器：","2"},{"VLC 视频解码","2"},{"默认倍速","2"},{"长按加速","2"},{"快进快退","2"},{"自动换源","2"},{"自动播放下一集","2"},{"跳过片头片尾","2"},{"双击中间播放暂停","2"},{"记忆画面比例","2"},{"记忆亮度","2"},{"强制高刷","2"},{"系统媒体控制","2"},{"杜比视界片源","2"},
  {"状态栏时间","3"},{"状态栏网速","3"},{"状态栏电量","3"},{"缓冲速度","3"},
  {"字幕样式","4"},{"弹弹play","4"},{"弹幕","4"},{"射手 Token","4"},{"OpenSubtitles","4"},{"Whisper 服务器","4"},{"Whisper 令牌","4"},
  {"多线程加速","5"},{"P2P 加速","5"},{"离线下载","5"},{"备份与恢复","5"},{"清理封面缓存","5"},
  {"检查更新","6"},{"自动检查更新","6"},{"故障排查","6"},
 };
 /** 返回 {组号, 组内顺序}；-1 表示丢弃（原版的「界面与播放」小标题）。 */
 /** 0.1.147（C1）：概览卡片里的数量（Emby 服务器、收藏、离线下载）。 */
 static String counts(int servers,int favorites,int offline){return servers+"/"+favorites+"/"+offline;}
 /** 没有记录过（页面不是「我的」）时不算变化，避免在别的页面重建。 */
 static boolean sameCounts(String built,String now){return built==null||built.equals(now);}
 static int[] place(String label){
  if(label==null)return new int[]{7,999};String s=label.trim();
  if(s.equals("界面与播放"))return new int[]{-1,0};
  if(s.startsWith("随影新版"))return new int[]{6,100};
  for(int i=0;i<RULES.length;i++)if(s.startsWith(RULES[i][0]))return new int[]{Integer.parseInt(RULES[i][1]),i};
  return new int[]{7,999};
 }
 /** 「名称：开…」→ 开关；返回 null 表示不是开关。[0]=名称 [1]=开/关 [2]=括号里的说明 */
 static String[] toggle(String label){
  if(label==null)return null;int c=label.indexOf('：');if(c<=0)return null;String name=label.substring(0,c),value=label.substring(c+1).trim();
  boolean on=value.startsWith("开"),off=value.startsWith("关");if(!on&&!off)return null;
  String note="";int p=value.indexOf('（');if(p>=0&&value.endsWith("）"))note=value.substring(p+1,value.length()-1);else{int dot=value.indexOf(" · ");if(dot>=0)note=value.substring(dot+3);}
  return new String[]{name,on?"开":"关",note};
 }
 /** 「名称：值」→ {名称, 值}；没有冒号返回 {label, ""}。 */
 static String[] split(String label){int c=label.indexOf('：');if(c<=0)return new String[]{label,""};return new String[]{label.substring(0,c),label.substring(c+1).trim()};}

}
