package com.guocheng.mediapocket;
import org.json.*;
public final class TrickplayRulesTest {
 public static void main(String[] a)throws Exception{
  JSONObject item=new JSONObject("{\"Trickplay\":{\"ms1\":{\"320\":{\"Width\":320,\"Height\":180,\"TileWidth\":10,\"TileHeight\":10,\"ThumbnailCount\":250,\"Interval\":10000},\"160\":{\"Width\":160,\"Height\":90,\"TileWidth\":10,\"TileHeight\":10,\"ThumbnailCount\":250,\"Interval\":10000}}}}");
  TrickplayRules.Info info=TrickplayRules.pick(item,"ms1",320);
  if(info==null||info.width!=320)throw new AssertionError("pick 320");
  int[] l=TrickplayRules.locate(info,125_000);
  if(l[0]!=0||l[1]!=2*320||l[2]!=180)throw new AssertionError("locate frame 12: "+l[0]+","+l[1]+","+l[2]);
  int[] l2=TrickplayRules.locate(info,1_050_000);
  if(l2[0]!=1)throw new AssertionError("tile 1: "+l2[0]);
  String p=TrickplayRules.path("it","ms1",info,1);
  if(!p.equals("/Videos/it/Trickplay/320/1.jpg?MediaSourceId=ms1"))throw new AssertionError(p);
  if(TrickplayRules.pick(new JSONObject("{}"),"ms1",320)!=null)throw new AssertionError("empty");
  System.out.println("TrickplayRulesTest ok");
 }
}
