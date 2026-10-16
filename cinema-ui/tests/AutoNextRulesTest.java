package com.guocheng.mediapocket;
import org.json.*;
public class AutoNextRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x)throws Exception{
  JSONArray v=new JSONArray();v.put(new JSONObject().put("id","tt1:1:2").put("season",1).put("episode",2));v.put(new JSONObject().put("id","tt1:1:1").put("season",1).put("episode",1));v.put(new JSONObject().put("id","tt1:2:1").put("season",2).put("episode",1));
  JSONObject meta=new JSONObject().put("videos",v);
  check("tt1:1:2".equals(AutoNextRules.next(meta,"tt1:1:1")),"next in season");check("tt1:2:1".equals(AutoNextRules.next(meta,"tt1:1:2")),"next season");check(AutoNextRules.next(meta,"tt1:2:1")==null,"last episode");check(AutoNextRules.next(meta,null)==null&&AutoNextRules.next(null,"a")==null,"null safe");
  JSONObject m2=new JSONObject().put("videos",new JSONArray().put(new JSONObject().put("id","p-ep-9").put("season",3).put("episode",5)));check("p-ep-9".equals(AutoNextRules.next(m2,"show:3:4")),"pattern fallback");
  int d=2400000;check(!AutoNextRules.offer(true,true,true,false,false,d-30000,d,20000),"too early");check(AutoNextRules.offer(true,true,true,false,false,d-15000,d,20000),"in lead");check(AutoNextRules.offer(true,true,true,false,true,0,0,20000),"ended");
  check(!AutoNextRules.offer(false,true,true,false,true,0,0,20000)&&!AutoNextRules.offer(true,false,true,false,true,0,0,20000)&&!AutoNextRules.offer(true,true,false,false,true,0,0,20000)&&!AutoNextRules.offer(true,true,true,true,true,0,0,20000),"disabled/movie/no next/cancelled");
  check(!AutoNextRules.offer(true,true,true,false,false,d-5000,d,0),"end-only mode waits");check(!AutoNextRules.offer(true,true,true,false,false,20000,30000,20000),"short clip ignored");
  check(AutoNextRules.lead(12345)==20000&&AutoNextRules.lead(0)==0&&AutoNextRules.lead(60000)==60000,"lead");check(AutoNextRules.secondsLeft(0)==5&&AutoNextRules.secondsLeft(4100)==1&&AutoNextRules.secondsLeft(5000)==0,"countdown");
  check(HomeRules.shouldStoreDuration(2400000,0)&&!HomeRules.shouldStoreDuration(2400500,2400000)&&!HomeRules.shouldStoreDuration(5000,0),"duration store");
  check(HomeRules.progress(1200000,2400000,0)==0.5f&&HomeRules.progress(1200000,0,0)!=0.5f,"real duration used over 45-min estimate");
  System.out.println("PASS: next episode ordering/season rollover/fallback, offer threshold/end/toggles, lead, countdown, real duration storage and progress.");}
}
