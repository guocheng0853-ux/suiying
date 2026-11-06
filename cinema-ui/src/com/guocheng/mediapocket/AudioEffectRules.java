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
 static final String OFF="off",DIALOG="dialog",NIGHT="night",AKG="akg",ANIME="anime",CINEMA="cinema",DOCUMENTARY="documentary",VARIETY="variety",VINTAGE="vintage",PRESET="preset:";
 /** 0.1.169 影院：超低音和低音下潜（爆炸、配乐的冲击感），中频略收让声场显得更开阔，2–4 kHz 托住对白，高频加空气感。前级留足余量防破音。 */
 static final float[] CINEMA_BANDS={5f,4.5f,2.5f,0f,-1.5f,-0.5f,1.5f,2.5f,2.5f,3f};static final float CINEMA_PREAMP=-5f;
 /** 纪录片 / 旁白：旁白男声的胸腔感在 125–250 Hz，咬字在 2–4 kHz；低频轰鸣和高频嘶声都不抬。 */
 static final float[] DOCUMENTARY_BANDS={0f,0.5f,1.5f,1.5f,0.5f,1f,3f,3f,1f,-0.5f};static final float DOCUMENTARY_PREAMP=-3f;
 /** 综艺 / 真人秀：多人说话加现场噪声，压掉 31–125 Hz 的现场轰隆，整段抬亮人声，笑声和字效更清楚。 */
 static final float[] VARIETY_BANDS={-3f,-2f,-0.5f,0f,1f,2f,3f,3f,2.5f,1.5f};static final float VARIETY_PREAMP=-3f;
 /** 老剧修复：早年剧集和低码率片源的嘶声集中在 8 kHz 以上，压掉它；中低频补回厚度，听起来不再干瘪发尖。 */
 static final float[] VINTAGE_BANDS={1f,2f,2.5f,1.5f,0.5f,0f,1f,0f,-2.5f,-6f};static final float VINTAGE_PREAMP=-2.5f;
 /** 菜单顺序：影院放在关闭后面，然后是看剧常用的几种。 */
 static final String[] CURATED={OFF,CINEMA,DIALOG,NIGHT,DOCUMENTARY,VARIETY,VINTAGE,ANIME,AKG};
 /**
  * 0.1.134 AKG 风格：仿 AKG / 哈曼目标曲线的听感——超低音和低音饱满、中频略收、高频通透有空气感。
  * 只是均衡器曲线，不是 AKG 官方算法（三星手机里的 AKG 调音是厂商私有的处理，第三方拿不到）。
  */
 static final float[] AKG_BANDS={4f,4f,2.5f,0f,-1f,0f,1f,2f,1.5f,2.5f};static final float AKG_PREAMP=-3f;
 /** 动漫：压掉 250 Hz 的浑浊，抬高 2–8 kHz 让日语配音和角色声线更清亮，低音轻抬保留打斗冲击感，高频略亮配合配乐。 */
 static final float[] ANIME_BANDS={1f,2f,1f,-2f,-1f,1f,3f,4f,3f,2f};static final float ANIME_PREAMP=-3.5f;
 static final float[] DIALOG_BANDS={-6f,-5f,-3f,-1f,1f,3f,4.5f,4.5f,3f,0f};static final float DIALOG_PREAMP=-4f;
 static final float[] NIGHT_BANDS={-12f,-10f,-7f,-3f,0f,2f,3f,2f,0f,-2f};static final float NIGHT_PREAMP=-2f;
 /** VLC 预设的英文名 → 中文名；不认识的原样显示。 */
 static final String[][] NAMES={{"Flat","平直"},{"Classical","古典"},{"Club","俱乐部"},{"Dance","舞曲"},{"Full bass","重低音"},{"Full bass and treble","低音 + 高音"},{"Full treble","高音"},{"Headphones","耳机"},{"Large Hall","大厅"},{"Live","现场"},{"Party","派对"},{"Pop","流行"},{"Reggae","雷鬼"},{"Rock","摇滚"},{"Ska","斯卡"},{"Soft","柔和"},{"Soft rock","轻摇滚"},{"Techno","电子"}};
 static String chinese(String english){if(english==null)return "";for(String[] n:NAMES)if(n[0].equalsIgnoreCase(english.trim()))return n[1];return english;}
 static String normalize(String saved){if(saved==null||saved.isEmpty())return OFF;for(String c:CURATED)if(c.equals(saved))return saved;if(saved.startsWith(PRESET)){try{int n=Integer.parseInt(saved.substring(PRESET.length()));if(n>=0&&n<64)return PRESET+n;}catch(Exception ignored){}}return OFF;}
 static int presetIndex(String saved){String s=normalize(saved);return s.startsWith(PRESET)?Integer.parseInt(s.substring(PRESET.length())):-1;}
 /** 自定义曲线：{前级, 第 0 段, …}；不是自定义曲线返回 null。段数不足 10 时按实际段数截取。 */
 static float[] curve(String saved,int bands){String s=normalize(saved);float[] src;float pre;if(s.equals(DIALOG)){src=DIALOG_BANDS;pre=DIALOG_PREAMP;}else if(s.equals(NIGHT)){src=NIGHT_BANDS;pre=NIGHT_PREAMP;}else if(s.equals(AKG)){src=AKG_BANDS;pre=AKG_PREAMP;}else if(s.equals(ANIME)){src=ANIME_BANDS;pre=ANIME_PREAMP;}else if(s.equals(CINEMA)){src=CINEMA_BANDS;pre=CINEMA_PREAMP;}else if(s.equals(DOCUMENTARY)){src=DOCUMENTARY_BANDS;pre=DOCUMENTARY_PREAMP;}else if(s.equals(VARIETY)){src=VARIETY_BANDS;pre=VARIETY_PREAMP;}else if(s.equals(VINTAGE)){src=VINTAGE_BANDS;pre=VINTAGE_PREAMP;}else return null;
  int n=Math.max(0,Math.min(bands,src.length));float[] out=new float[n+1];out[0]=pre;for(int i=0;i<n;i++)out[i+1]=src[i];return out;}
 static String label(String saved,String presetName){String s=normalize(saved);if(s.equals(DIALOG))return "对白增强";if(s.equals(NIGHT))return "夜间模式";if(s.equals(AKG))return "AKG 风格";if(s.equals(ANIME))return "动漫";if(s.equals(CINEMA))return "影院";if(s.equals(DOCUMENTARY))return "纪录片旁白";if(s.equals(VARIETY))return "综艺";if(s.equals(VINTAGE))return "老剧修复";if(s.startsWith(PRESET))return "均衡器 · "+chinese(presetName);return "关闭";}
 /** 菜单里每种的一句说明。 */
 static String hint(String value){switch(normalize(value)){case CINEMA:return "低音下潜、声场开阔，适合电影和大制作剧集";case DIALOG:return "压低轰鸣，人声更清楚";case NIGHT:return "大幅压低爆炸和重低音，小音量也听得清";case DOCUMENTARY:return "旁白更厚、更清楚，适合纪录片";case VARIETY:return "压掉现场轰隆，多人说话更清楚";case VINTAGE:return "压掉老片和低码率片源的嘶声，补回厚度";case ANIME:return "配音更清亮，配乐更亮";case AKG:return "低音饱满、高音通透，适合耳机";default:return "";}}
}
