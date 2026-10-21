package com.guocheng.mediapocket;
import java.util.*;
/**
 * 0.1.110 播放页「音效」的纯逻辑（不依赖 Android，便于测试）。
 * 用 VLC 内核自带的 10 段均衡器（31 Hz … 16 kHz）。保存值：off / dialog / night / preset:N。
 * 对白增强：压低 250 Hz 以下的轰鸣，抬高 1–8 kHz 的人声清晰度段；
 * 夜间：大幅压低 125 Hz 以下（爆炸、配乐低音是半夜吵人的主因），轻抬人声段，整体前级留出余量防止破音。
 */
final class AudioEffectRules {
 private AudioEffectRules(){}
 static final String OFF="off",DIALOG="dialog",NIGHT="night",PRESET="preset:";
 static final float[] DIALOG_BANDS={-6f,-5f,-3f,-1f,1f,3f,4.5f,4.5f,3f,0f};static final float DIALOG_PREAMP=-4f;
 static final float[] NIGHT_BANDS={-12f,-10f,-7f,-3f,0f,2f,3f,2f,0f,-2f};static final float NIGHT_PREAMP=-2f;
 /** VLC 预设的英文名 → 中文名；不认识的原样显示。 */
 static final String[][] NAMES={{"Flat","平直"},{"Classical","古典"},{"Club","俱乐部"},{"Dance","舞曲"},{"Full bass","重低音"},{"Full bass and treble","低音 + 高音"},{"Full treble","高音"},{"Headphones","耳机"},{"Large Hall","大厅"},{"Live","现场"},{"Party","派对"},{"Pop","流行"},{"Reggae","雷鬼"},{"Rock","摇滚"},{"Ska","斯卡"},{"Soft","柔和"},{"Soft rock","轻摇滚"},{"Techno","电子"}};
 static String chinese(String english){if(english==null)return "";for(String[] n:NAMES)if(n[0].equalsIgnoreCase(english.trim()))return n[1];return english;}
 static String normalize(String saved){if(saved==null||saved.isEmpty())return OFF;if(saved.equals(DIALOG)||saved.equals(NIGHT)||saved.equals(OFF))return saved;if(saved.startsWith(PRESET)){try{int n=Integer.parseInt(saved.substring(PRESET.length()));if(n>=0&&n<64)return PRESET+n;}catch(Exception ignored){}}return OFF;}
 static int presetIndex(String saved){String s=normalize(saved);return s.startsWith(PRESET)?Integer.parseInt(s.substring(PRESET.length())):-1;}
 /** 自定义曲线：{前级, 第 0 段, …}；不是自定义曲线返回 null。段数不足 10 时按实际段数截取。 */
 static float[] curve(String saved,int bands){String s=normalize(saved);float[] src;float pre;if(s.equals(DIALOG)){src=DIALOG_BANDS;pre=DIALOG_PREAMP;}else if(s.equals(NIGHT)){src=NIGHT_BANDS;pre=NIGHT_PREAMP;}else return null;
  int n=Math.max(0,Math.min(bands,src.length));float[] out=new float[n+1];out[0]=pre;for(int i=0;i<n;i++)out[i+1]=src[i];return out;}
 static String label(String saved,String presetName){String s=normalize(saved);if(s.equals(DIALOG))return "对白增强";if(s.equals(NIGHT))return "夜间模式";if(s.startsWith(PRESET))return "均衡器 · "+chinese(presetName);return "关闭";}
}
