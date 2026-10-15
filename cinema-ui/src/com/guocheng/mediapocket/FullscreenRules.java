package com.guocheng.mediapocket;
final class FullscreenRules {
 static String mode(String saved,boolean migrated){if(!migrated)return "stretch";for(String mode:new String[]{"stretch","fit","crop","16:9","4:3"})if(mode.equals(saved))return mode;return "stretch";}
 static float zoom(String mode,float saved){return "stretch".equals(mode)?1f:saved;}
}
