package com.guocheng.mediapocket;
import org.json.*;
public class TrackSelectionTest {
 static class Track {public int id;Track(int n){id=n;}}
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  check(TrackSelection.contains(new Object[]{new Track(2),null,new Track(5)},5),"available track accepted");
  check(!TrackSelection.contains(new Object[]{new Track(2)},5),"missing track never selected");
  check(!TrackSelection.contains(null,5),"unready decoder retries");
  check(TrackSelection.contains(null,-1),"subtitle disable allowed");
  JSONObject one=new JSONObject().put("url","https://example/a.mkv");JSONObject other=new JSONObject().put("url","https://example/b.mkv");
  check(!TrackSelection.sameSource(one,other),"track selection does not cross sources");
  check(TrackSelection.sameSource(one,new JSONObject(one.toString())),"serialized source keeps selection");
  one.put("mp.audioTrack",2);check(TrackSelection.hasAudio(one)&&!TrackSelection.hasSubtitle(one),"explicit selections distinct");
  System.out.println("PASS: actual track ID validation, unready decoder, subtitle disable, exact-source selection scope.");
 }
}
