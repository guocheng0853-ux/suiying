package com.guocheng.mediapocket;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
/** 「我的」里的字幕设置。播放页用同一份内容，但是弹在当前画面上，免得把片子暂停。 */
public final class SubtitleSettingsActivity extends Activity {
 @Override protected void onCreate(Bundle saved){boolean light=CinemaTheme.light(this);getTheme().applyStyle(light?android.R.style.Theme_Material_Light_NoActionBar:android.R.style.Theme_Material_NoActionBar,true);super.onCreate(saved);View page=SubtitleSettings.shell(this,this::finish,false);setContentView(page);getWindow().setStatusBarColor(CinemaTheme.light(this)?CinemaTheme.PAPER:CinemaTheme.BG);CinemaTheme.barIcons(this,light);}
}
