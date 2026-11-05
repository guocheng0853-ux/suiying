package com.guocheng.mediapocket;
/** 玻璃面板的数值（纯逻辑，可单测）。 */
final class GlassRules {
 private GlassRules(){}
 static final int BLUR_DP=28,RADIUS_DP=20,EDGE_DP=8;
 /** 居中菜单后面再轻压一点暗，让菜单更聚焦。 */
 static final float MENU_DIM=0.25f;
 /** 只在播放页（深色）用玻璃；浅色纸面主题的页面保持原样。 */
 /** 0.1.173 面板行悬停 / 聚焦：圆角、底色、强调竖条高度、内容右移、渐变时长、按下水波纹颜色。 */
 static final int ROW_RADIUS_DP=12,BAR_DP=18,HOVER_SHIFT_DP=4,HOVER_MS=150,HOVER_FILL=0x1Affffff,ACTIVE_FILL=0x29ffffff,PRESS_RIPPLE=0x33ffffff;
 /** 0.1.173「更多」下拉：二级菜单沿用锚点的时长、与按钮的间距、下拉时更淡的压暗。 */
 static final int ANCHOR_MS=1500,DROP_GAP_DP=6;static final float DROP_DIM=0.12f;
 /**
  * 下拉菜单的窗口位置 {x, y, 最大高度}（Gravity.TOP|START，相对窗口左上角）。
  * 右边对齐按钮右边，左右都至少留 edge；顶在按钮下方 gap 处；最大高度是下面剩下的空间（至少 1/3 屏高，否则改成贴边往上放）。
  */
 static int[] dropDown(int screenW,int screenH,int menuW,int anchorRight,int anchorBottom,int edge,int gap){
  int x=anchorRight-menuW;x=Math.max(edge,Math.min(x,screenW-menuW-edge));
  int y=anchorBottom+gap;int room=screenH-y-edge;
  if(room<screenH/3){y=edge;room=screenH-2*edge;}
  return new int[]{x,y,Math.max(0,room)};
 }
 static boolean player(boolean inPlayer,boolean paper){return inPlayer&&!paper;}
 static int tint(boolean blur){return blur?0x9E17171A:0xE617171A;}
 /** 白字在玻璃上的最差对比：把背后当成纯白，按色板合成后算。模糊时背后被压成灰，实际更好。 */
 static double worstContrast(boolean blur){int t=tint(blur);double alpha=((t>>>24)&0xff)/255.0;int c=t&0xff;double mixed=(c*alpha+255*(1-alpha))/255.0;double l=mixed<=0.03928?mixed/12.92:Math.pow((mixed+0.055)/1.055,2.4);return 1.05/(l+0.05);}
 /** 侧边面板窗口：宽 w、高 = 屏高 − 上下边距；整屏 0 时返回 0。 */
 static int[] sideWindow(int screenH,int panelW,int edgePx){if(screenH<=0||panelW<=0)return new int[]{0,0};return new int[]{panelW,Math.max(0,screenH-2*edgePx)};}
}
