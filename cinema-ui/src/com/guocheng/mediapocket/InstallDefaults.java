package com.guocheng.mediapocket;
/**
 * 0.1.132：外观只留两套——白天「暖白陶土」(clay)、夜间「影院暗金」(gold)，外加「跟随系统」(system) 按系统深色模式在两者间切换。
 * 新安装默认跟随系统；升级时旧选项折算：深青色 / 旧的深色 → 影院暗金，日间 → 暖白陶土，其余 → 跟随系统。聚合首页仍只在新安装时写入。
 */
final class InstallDefaults {
 private InstallDefaults(){}
 static final String SYSTEM="system",CLAY="clay",GOLD="gold";
 static final class Choice {
  final boolean writeProvider,writeAppearance;final String appearance;
  Choice(boolean writeProvider,boolean writeAppearance,String appearance){this.writeProvider=writeProvider;this.writeAppearance=writeAppearance;this.appearance=normalize(appearance);}
 }
 /** 存储值折算成三选一。 */
 static String normalize(String stored){if(stored==null)return SYSTEM;switch(stored){case CLAY:case "light":return CLAY;case GOLD:case "dark":return GOLD;default:return SYSTEM;}}
 /** 实际使用的外观：clay 或 gold。 */
 static String effective(String stored,boolean systemNight){String n=normalize(stored);return SYSTEM.equals(n)?(systemNight?GOLD:CLAY):n;}
 /** alreadyUsed 为真表示这台机器上已经有观看记录、插件、服务器、来源、外观或以前的引导标记。 */
 static Choice choose(boolean alreadyUsed,String currentAppearance){
  String n=normalize(currentAppearance);
  // 旧值（dark / light / 未设置）要折算后写回，所以只要和原值不同就写
  if(alreadyUsed)return new Choice(false,currentAppearance==null||!n.equals(currentAppearance),n);
  return new Choice(true,true,SYSTEM);
 }
 static String label(String stored,boolean systemNight){String n=normalize(stored),e=effective(stored,systemNight);String name=CLAY.equals(e)?"暖白陶土":"影院暗金";return SYSTEM.equals(n)?"跟随系统 · 当前"+name:name;}
}
