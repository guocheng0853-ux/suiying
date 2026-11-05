package com.guocheng.mediapocket;
import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.nio.file.Paths;
/** 0.1.170 接手 acceptance-0169 的三处修复：Media3 键盘长按加速、VLC 字幕改样式重建播放器、字幕设置深色玻璃与沉浸状态。 */
public class Accept0170Test {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static String src(String name)throws Exception{return new String(Files.readAllBytes(Paths.get("src/com/guocheng/mediapocket/"+name)),StandardCharsets.UTF_8);}
 public static void main(String[] args)throws Exception{
  // T1：Media3 键盘长按加速。VLC 与 Media3 共用 KeyboardRules.seekStepMs，接线必须把 repeatCount 传进去；屏幕键和双击仍走基础步长。
  String keys=src("KeyboardKeys.java"),controls=src("Media3Controls.java");
  check(keys.contains("KeyboardRules.seekStepMs(PlaybackPrefs.seekMs(c.prefs),e.getRepeatCount())"),"media3 keyboard feeds repeat count into the shared seek step");
  check(keys.contains("c.jump(act==KeyboardRules.FORWARD?1:-1,KeyboardRules"),"media3 keyboard jump carries the computed step");
  check(controls.contains("void jump(int direction,int stepMs)"),"Media3Controls keeps a step-aware jump overload");
  check(controls.split("void jump\\(int direction\\)\\{",2).length==2&&controls.split("void jump\\(int direction\\)\\{",2)[1].contains("jump(direction,PlaybackPrefs.seekMs(prefs))"),"single-arg jump delegates with the base step");
  check(KeyboardRules.seekStepMs(15000,0)==15000&&KeyboardRules.seekStepMs(15000,8)==15000&&KeyboardRules.seekStepMs(15000,9)==30000,"hold acceleration starts after 8 repeats, matching the VLC path");
  // T2：freetype 样式只在新建 LibVLC 的全局选项里生效（media 级 ：freetype-* 进不了 vout），改样式必须重建播放器；改解码档位仍先试原位换 Media。
  String settings=src("SubtitleSettings.java"),decoder=src("DecoderSwitch.java");
  check(settings.contains("DecoderSwitch.rebuild(a,PlayerOverlay.currentTier(a))"),"closing the sheet with a changed style rebuilds the VLC player");
  check(decoder.contains("!reason.startsWith(\"当前不是\")")&&decoder.contains("return once(a,tier);"),"decode-tier switch keeps its original in-place-then-retry-and-once path");
  check(decoder.contains("retryCurrentDecoder"),"decode-tier fallback still has the base retry path");
  check(decoder.contains("in.replaceExtras(a.getIntent())")&&decoder.contains("a.finish()"),"style rebuild restarts the player page so onCreate rebuilds LibVLC with the new global style options");
  SubtitlePrefs.Values yellow=new SubtitlePrefs.Values("normal","shadow","0","yellow","zho",false,true,"normal","2",0);
  SubtitlePrefs.Values cyan=new SubtitlePrefs.Values("normal","shadow","0","cyan","zho",false,true,"normal","2",0);
  check(!SubtitleVlcRules.signature(yellow).equals(SubtitleVlcRules.signature(cyan)),"color is part of the VLC style signature");
  check(SubtitleVlcRules.global(yellow).contains("--freetype-color="+(0xffffe14a&0xffffff))&&SubtitleVlcRules.media(yellow).contains(":freetype-color="+(0xffffe14a&0xffffff)),"yellow maps to the freetype color on both option levels");
  check(SubtitleVlcRules.global(yellow).size()==SubtitleVlcRules.media(yellow).size(),"global and media option sets stay in sync");
  // T3：播放页字幕设置弹层换深色玻璃并跟随沉浸状态；弹层上方按当前设置实时渲染预览；「我的」整页保持纸面。
  check(settings.contains("VideoFullscreen.hideBars(d.getWindow())"),"the sheet hides system bars through the same path as the player");
  check(settings.contains("dark=sheet&&glass(a)")&&settings.contains("dark?PlayerTheme.PANEL_SOFT"),"the player sheet switches to the translucent dark glass panel");
  check(settings.contains("a instanceof PlayerActivity||a instanceof SystemPlayerActivity"),"glass covers both the VLC and Media3 players");
  check(settings.contains("static boolean glass(Activity a)"),"glass applies only in the player dialog path");
  check(settings.contains("字幕预览 Sample")&&settings.contains("SubtitleStyle.style(a,preview,true,0,dim,false)"),"the sheet renders a live-styled preview above itself for native VLC subtitles");
  check(settings.contains("static void clearLift(){previewLiftPx=0;preview=null;}"),"dismiss clears the preview together with the lift");
  check(src("SubtitleSettingsActivity.java").contains("shell(this,this::finish,false)"),"the「我的」full page keeps the paper shell");
  System.out.println("PASS: media3 keyboard hold acceleration, VLC style rebuild, dark glass settings sheet.");
 }
}
