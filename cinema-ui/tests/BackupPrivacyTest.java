package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** 0.1.120：不含账号的备份要丢掉字幕密钥，并洗掉网址里的账号、查询参数和路径令牌。 */
public class BackupPrivacyTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Map<String,Object> prefs=new HashMap<>();
  prefs.put("subtitle.opensubtitlesKey","OS-SECRET");
  prefs.put("site","{\"key\":\"Alist\",\"name\":\"小雅\",\"token\":\"hide-me\"}");
  String tv="https://user:pass@xiaoya.example/Ab12cd34Ef56/tvbox/my_ext.json?token=abc&api_key=k&x=1";
  prefs.put("tvbox.profiles",tv);
  prefs.put("addon.url","https://addon.example/Ab12cd34Ef56/manifest.json");
  prefs.put("debrid","https://debrid.example/d/Ab12cd34Ef56/file.mkv");
  prefs.put("lone","https://xiaoya.example/Ab12cd34Ef56");
  prefs.put("poster","https://cdn.example/posters/df6450a1cc03f3139b659338e9f15278.jpg");
  prefs.put("emby.item","https://emby.example/Items/5f2a8c9d1e4b7a6c0d3e/Images/Primary");
  String configKey="tvbox.config:https://user:pass@xiaoya.example/Ab12cd34Ef56/tvbox/my_ext.json";
  prefs.put(configKey,"{\"sites\":[]}");
  String plainKey="bookmark:https://cdn.example/watch/movie";
  prefs.put(plainKey,"keep-me");
  JSONObject full=BackupRules.encode(prefs,true),lite=BackupRules.encode(prefs,false);
  check(full.has("subtitle.opensubtitlesKey")&&full.optJSONObject("tvbox.profiles").optString("v").contains("token="),"full backup keeps secrets");
  check(!lite.has("subtitle.opensubtitlesKey"),"lite drops opensubtitles key");
  String site=lite.optJSONObject("site").optString("v");
  check(site.contains("\"key\":\"Alist\"")&&site.contains("小雅")&&!site.contains("hide-me"),"json site id kept, nested token dropped: "+site);
  String profiles=lite.optJSONObject("tvbox.profiles").optString("v");
  check(!profiles.contains("user:pass")&&!profiles.contains("token=")&&!profiles.contains("api_key=")&&profiles.contains("x=1")&&profiles.contains("/REDACTED/tvbox/"),"tvbox url scrubbed: "+profiles);
  check(lite.optJSONObject("addon.url").optString("v").contains("/REDACTED/manifest.json"),"addon path token");
  check(lite.optJSONObject("debrid").optString("v").contains("/d/REDACTED/"),"debrid path token");
  check(lite.optJSONObject("lone").optString("v").endsWith("/REDACTED"),"lone path token");
  check(lite.optJSONObject("poster").optString("v").contains("df6450a1cc03f3139b659338e9f15278.jpg"),"poster hash kept");
  check(lite.optJSONObject("emby.item").optString("v").contains("5f2a8c9d1e4b7a6c0d3e"),"emby id kept");
  check(full.has(configKey)&&!lite.has(configKey)&&!lite.toString().contains("tvbox.config:"),"lite drops a preference whose key embeds a credential URL");
  check("keep-me".equals(lite.optJSONObject(plainKey).optString("v")),"plain URL key that scrubbing does not change is kept");
  Map<String,JSONObject> sec=new HashMap<>();sec.put("data",full);JSONObject root=BackupRules.wrap("0.1.120",1L,true,sec);
  String sealed=BackupRules.seal(root.toString(),"test-pass");
  check(BackupRules.sealed(sealed)&&!sealed.contains("OS-SECRET"),"sealed wrapper hides the key");
  JSONObject opened=BackupRules.open(sealed,"test-pass");
  check(opened.optJSONObject("prefs").optJSONObject("data").has("subtitle.opensubtitlesKey"),"password opens the backup");
  boolean ask=false;try{BackupRules.open(sealed,null);}catch(IllegalArgumentException e){ask=e.getMessage().contains("请输入密码");}check(ask,"sealed file asks for a password");
  boolean wrong=false;try{BackupRules.open(sealed,"nope");}catch(IllegalArgumentException e){wrong=e.getMessage().contains("密码不对");}check(wrong,"wrong password rejected");
  check(BackupRules.KDF_ITER>=210000&&new JSONObject(sealed).optInt("iter")==BackupRules.KDF_ITER,"pbkdf2 iterations stored and at least 210000");
  String spaced="  test-pass  ";
  String sealedSpaces=BackupRules.seal(root.toString(),spaced);
  check(BackupRules.open(sealedSpaces,spaced).optJSONObject("prefs").optJSONObject("data").has("subtitle.opensubtitlesKey"),"untrimmed password round-trips");
  boolean trimmed=false;try{BackupRules.open(sealedSpaces,spaced.trim());}catch(IllegalArgumentException e){trimmed=e.getMessage().contains("密码不对");}check(trimmed,"trimming the password does not open the backup");
  boolean shortSpaces=false;try{BackupRules.seal("{}", "   ");}catch(IllegalArgumentException e){shortSpaces=e.getMessage().contains("至少 4");}check(shortSpaces,"spaces count toward the length");
  System.out.println("PASS: lite backup drops key prefs, credential URL keys, and URL credentials; site id and media paths stay; sealed backup round-trips with the untrimmed password.");
 }
}
