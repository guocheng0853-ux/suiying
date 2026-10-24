package com.guocheng.mediapocket;
/**
 * 0.1.141 全 App 界面规范（按花叔Design 审查定，见 docs/design/huashu-audit-0141.md）。两套外观共用，只换颜色不换尺度。
 * 字号 5 档：12 说明·标签 / 14 次要文字 / 16 正文·按钮 / 20 栏目标题 / 28 片名；最小 12。
 * 间距 4 / 8 / 16 / 24 / 32；圆角 小 8（标签、缩略图）/ 中 12（按钮、卡片）/ 大 20（面板、弹层），≥100 表示正圆或全胶囊照旧。
 * 可点区域至少 48dp，图标 24dp。中文字距一律 0。不依赖 Android，便于测试。
 */
final class Ui {
 private Ui(){}
 static final int CAPTION=12,SMALL=14,BODY=16,SECTION=20,DISPLAY=28;
 static final int S1=4,S2=8,S3=16,S4=24,S5=32;
 static final int R_S=8,R_M=12,R_L=20;
 static final int TOUCH=48,ICON=24;
 /** 旧字号就近折算到 5 档。 */
 static int sp(int legacy){if(legacy<=12)return CAPTION;if(legacy<=14)return SMALL;if(legacy<=17)return BODY;if(legacy<=22)return SECTION;return DISPLAY;}
 /** 旧圆角折算到 3 档；0 保持直角，≥100 是正圆 / 全胶囊。 */
 static int radius(int legacy){if(legacy<=0)return 0;if(legacy>=100)return legacy;if(legacy<=8)return R_S;if(legacy<=14)return R_M;return R_L;}
 /** 栏目标题、按钮去掉尾巴箭头「›」「→」（花叔Design：按钮尾巴箭头属模板装饰）。返回去掉后的文字。 */
 static String plain(String label){if(label==null)return "";String s=label.replaceAll("\\s*[›→]\\s*$","");return s;}
 static boolean hadArrow(String label){return label!=null&&label.matches(".*[›→]\\s*$");}
}
