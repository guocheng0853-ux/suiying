package com.guocheng.mediapocket;
import android.media.audiofx.Equalizer;import java.util.*;
/** Uses Android's active audio session. VLC Equalizer objects cannot control ExoPlayer. */
final class Media3Sound {
 static final Map<SystemPlayerActivity,State> ALL=new WeakHashMap<>();
 static final class State {Equalizer eq;int session;String applied;}
 static void release(SystemPlayerActivity a){State s=ALL.remove(a);if(s!=null&&s.eq!=null)try{s.eq.release();}catch(RuntimeException ignored){}}
 static State state(SystemPlayerActivity a){int id=a.video.getAudioSessionId();if(id<=0)return null;State s=ALL.get(a);if(s!=null&&s.session==id)return s;release(a);s=new State();s.session=id;ALL.put(a,s);try{s.eq=new Equalizer(0,id);}catch(RuntimeException e){DebugLog.event("杜比音效","设备未提供可用均衡器");}return s;}
 static void tick(SystemPlayerActivity a){String effect=AudioEffects.saved(a);int preset=a.getSharedPreferences("data",0).getInt("audio.media3Preset",-1);if(AudioEffectRules.OFF.equals(effect)&&preset<0&&!ALL.containsKey(a))return;State s=state(a);if(s==null||s.eq==null)return;String key=effect+":"+preset;if(key.equals(s.applied))return;s.applied=key;try{
  if(preset>=0&&preset<s.eq.getNumberOfPresets()){s.eq.usePreset((short)preset);s.eq.setEnabled(true);a.video.player.setVolume(1f);return;}
  if(AudioEffectRules.OFF.equals(effect)||effect.startsWith(AudioEffectRules.PRESET)){s.eq.setEnabled(false);a.video.player.setVolume(1f);return;}
  float[] curve=AudioEffectRules.DIALOG.equals(effect)?AudioEffectRules.DIALOG_BANDS:AudioEffectRules.NIGHT_BANDS;double[] frequencies={31,63,125,250,500,1000,2000,4000,8000,16000};short[] range=s.eq.getBandLevelRange();
  for(short band=0;band<s.eq.getNumberOfBands();band++){double hz=s.eq.getCenterFreq(band)/1000.0;int nearest=0;for(int n=1;n<frequencies.length;n++)if(Math.abs(Math.log(hz/frequencies[n]))<Math.abs(Math.log(hz/frequencies[nearest])))nearest=n;short level=(short)Math.max(range[0],Math.min(range[1],Math.round(curve[nearest]*100)));s.eq.setBandLevel(band,level);}s.eq.setEnabled(true);float pre=AudioEffectRules.DIALOG.equals(effect)?AudioEffectRules.DIALOG_PREAMP:AudioEffectRules.NIGHT_PREAMP;a.video.player.setVolume((float)Math.pow(10,pre/20));
 }catch(RuntimeException e){a.chrome.hint("设备暂时无法应用这项音效");}}
 static void menu(SystemPlayerActivity a){State s=state(a);if(s==null||s.eq==null){a.chrome.hint("音频尚未就绪，或设备未开放音效控制");return;}List<CinemaMenu.Item> rows=new ArrayList<>();for(String value:new String[]{AudioEffectRules.OFF,AudioEffectRules.DIALOG,AudioEffectRules.NIGHT})rows.add(CinemaMenu.item(AudioEffectRules.label(value,""),()->{a.getSharedPreferences("data",0).edit().putString(AudioEffects.KEY,value).putInt("audio.media3Preset",-1).apply();s.applied=null;tick(a);}));for(short i=0;i<s.eq.getNumberOfPresets();i++){final short preset=i;rows.add(CinemaMenu.item("均衡器 · "+s.eq.getPresetName(i),()->{a.getSharedPreferences("data",0).edit().putInt("audio.media3Preset",preset).apply();s.applied=null;tick(a);}));}CinemaMenu.show(a,"音效（设备均衡器）",rows);}
}
