package com.guocheng.mediapocket;
import org.json.*;
public class HomeCardRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  check(HomeRules.cardTitle(new JSONObject().put("name","蜘蛛侠：崭新之日 · Spider-Man.2026.1080p.mkv").put("fusion.site","s")).equals("蜘蛛侠：崭新之日"),"tvbox name loses file suffix");
  check(HomeRules.cardTitle(new JSONObject().put("name","x · y").put("fusion.title","真名")).equals("真名"),"fusion.title wins");
  check(HomeRules.cardTitle(new JSONObject().put("name","Stremio · Title")).equals("Stremio · Title"),"non-tvbox untouched");
  check(HomeRules.resumeLine("续播",30*60000,120*60000).equals("剩 1 小时 30 分"),"movie left hours");
  check(HomeRules.resumeLine("续播",100*60000,124*60000).equals("剩 24 分钟"),"movie left minutes");
  check(HomeRules.resumeLine("续播",0,0).equals("继续播放"),"unknown duration");
  check(HomeRules.resumeLine("S1E3 · 第3集",10*60000,40*60000).equals("S1E3 · 第3集 · 剩 30 分钟"),"episode + left");
  check(HomeRules.resumeLine("S1E3 · 第3集",0,0).equals("S1E3 · 第3集"),"episode only");
  check(HomeRules.sourceHint("provider:fusion","provider").contains("合并")&&HomeRules.sourceHint("action:emby","action").contains("Emby")&&HomeRules.sourceHint("source:abc","tvbox").equals("影视仓接口"),"source hints");
  System.out.println("PASS: continue-watching card title strips TVBox file suffix, subtitle shows episode + time left, source picker hints.");
 }
}
