package com.guocheng.mediapocket;
/** 0.1.130：聚合首页和影院暗金只作为新安装的默认值，升级时不覆盖已经在用的选择。 */
final class InstallDefaults {
 private InstallDefaults(){}
 static final class Choice {
  final boolean writeProvider,writeAppearance;final String appearance;
  Choice(boolean writeProvider,boolean writeAppearance,String appearance){this.writeProvider=writeProvider;this.writeAppearance=writeAppearance;this.appearance=appearance==null||appearance.isEmpty()?"dark":appearance;}
 }
 /** alreadyUsed 为真表示这台机器上已经有观看记录、插件、服务器、来源、外观或以前的引导标记。 */
 static Choice choose(boolean alreadyUsed,String currentAppearance){
  String appearance=currentAppearance==null||currentAppearance.isEmpty()?"dark":currentAppearance;
  if(alreadyUsed)return new Choice(false,false,appearance);
  return new Choice(true,true,"gold");
 }
}
