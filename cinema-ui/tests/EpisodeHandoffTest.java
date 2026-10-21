package com.guocheng.mediapocket;
import java.util.*;
public class EpisodeHandoffTest {
 public static void main(String[] args){
  List<String> events=new ArrayList<>();
  EpisodeHandoff.dispatch("emby:server:episode2",id->{events.add("request:"+id);return true;},()->events.add("finish/home"));
  check(events.equals(Arrays.asList("request:emby:server:episode2")),"prepare next episode without closing player or waiting for home");
  events.clear();EpisodeHandoff.dispatch("emby:server:episode1",id->{events.add("request failed/cancelled");return true;},()->events.add("finish/home"));
  check(events.equals(Arrays.asList("request failed/cancelled")),"previous episode error/cancellation preserves old player");
  events.clear();EpisodeHandoff.dispatch("tt123:1:2",id->{events.add("not direct");return false;},()->events.add("home request"));
  check(events.equals(Arrays.asList("not direct","home request")),"Stremio still follows source lookup route");
  events.clear();EpisodeHandoff.dispatch(null,id->{events.add("request");return true;},()->events.add("finish/home"));EpisodeHandoff.dispatch("",id->{events.add("request");return true;},()->events.add("finish/home"));check(events.isEmpty(),"missing target cannot close playback");
  System.out.println("PASS: direct previous/next requests preserve old playback during preparation/error/cancel; Stremio fallback and empty-target guard.");
 }
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
