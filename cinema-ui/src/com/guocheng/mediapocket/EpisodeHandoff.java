package com.guocheng.mediapocket;
import java.util.function.Predicate;
/** A handled direct request (also cancelled/failed) must never close playback via the home route. */
final class EpisodeHandoff {
 static void dispatch(String target,Predicate<String> direct,Runnable home){if(target==null||target.isEmpty())return;if(!direct.test(target))home.run();}
}
