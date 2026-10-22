package com.guocheng.mediapocket;
import org.json.*;
public class RetiredSourceRulesTest {
 public static void main(String[] args)throws Exception {
 if(!RetiredSourceRules.profile("https://raw.githubusercontent.com/qist/tvbox/master/fty.json"))throw new AssertionError("FTY preset must be removed");
 JSONObject c=new JSONObject("{\"spider\":\"keep.jar\",\"sites\":[{\"key\":\"mp_public_ruyi\"},{\"key\":\"mp_public_sony\"},{\"key\":\"mp_public_360\"},{\"key\":\"mp_public_ffzy\"},{\"key\":\"my_site\"}]}");
 RetiredSourceRules.clean(c);RetiredSourceRules.clean(c);if(c.getJSONArray("sites").length()!=1||!c.getString("spider").equals("keep.jar"))throw new AssertionError("preserve user site and component");
 JSONArray p=RetiredSourceRules.profiles(new JSONArray("[\"http://home.jundie.top:81/top98.json\",\"https://tvbox-public.gaoops.top/\",\"https://my.example/config.json\"]"));if(p.length()!=1||!p.getString(0).contains("my.example"))throw new AssertionError("preserve custom profile");
 if(!RetiredSourceRules.cacheKey("fusion.home:mp_public_ruyi")||!RetiredSourceRules.cacheKey("fusion.catalog.http://home.jundie.top:81/top98.json")||!RetiredSourceRules.cacheKey("cinema.catalog.https://tvbox-public.gaoops.top/")||!RetiredSourceRules.cacheKey("fusion.home:https://raw.githubusercontent.com/qist/tvbox/master/fty.json"))throw new AssertionError("retired public caches");
 if(RetiredSourceRules.cacheKey("fusion.home:my.example")||RetiredSourceRules.cacheKey("history")||RetiredSourceRules.cacheKey("tvbox.config:mp_public_ruyi"))throw new AssertionError("keep user caches and non-catalog keys");
 System.out.println("PASS: removes four retired CMS sources and retired preset profiles including FTY; retains custom site/config and is idempotent");
 }
}
