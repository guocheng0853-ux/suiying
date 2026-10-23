package com.guocheng.mediapocket;
import android.app.Application;
/** Starts the title font on a background thread before the first activity draws. */
public final class CinemaApp extends Application {
 @Override public void onCreate(){super.onCreate();CinemaTheme.warm(this);}
}
