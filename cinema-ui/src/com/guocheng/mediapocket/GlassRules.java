package com.guocheng.mediapocket;
/** 玻璃面板的数值（纯逻辑，可单测）。 */
final class GlassRules {
 private GlassRules(){}
 static final int BLUR_DP=28,RADIUS_DP=20,EDGE_DP=8;
 /** 居中菜单后面再轻压一点暗，让菜单更聚焦。 */
 static final float MENU_DIM=0.25f;
 /** 只在播放页（深色）用玻璃；浅色纸面主题的页面保持原样。 */
 static boolean player(boolean inPlayer,boolean paper){return inPlayer&&!paper;}
 static int tint(boolean blur){return blur?0x9E17171A:0xE617171A;}
 /** 白字在玻璃上的最差对比：把背后当成纯白，按色板合成后算。模糊时背后被压成灰，实际更好。 */
 static double worstContrast(boolean blur){int t=tint(blur);double alpha=((t>>>24)&0xff)/255.0;int c=t&0xff;double mixed=(c*alpha+255*(1-alpha))/255.0;double l=mixed<=0.03928?mixed/12.92:Math.pow((mixed+0.055)/1.055,2.4);return 1.05/(l+0.05);}
 /** 侧边面板窗口：宽 w、高 = 屏高 − 上下边距；整屏 0 时返回 0。 */
 static int[] sideWindow(int screenH,int panelW,int edgePx){if(screenH<=0||panelW<=0)return new int[]{0,0};return new int[]{panelW,Math.max(0,screenH-2*edgePx)};}
}
