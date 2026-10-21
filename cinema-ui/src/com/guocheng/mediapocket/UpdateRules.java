package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** 0.1.106 检查更新的纯逻辑：版本号比较、从 GitHub Releases 的返回里挑出安装包。 */
final class UpdateRules {
 private UpdateRules(){}
 static final String REPO="guocheng0853-ux/suiying";
 static final String LATEST="https://api.github.com/repos/"+REPO+"/releases/latest";
 static final String PAGE="https://github.com/"+REPO+"/releases";
 /** "v0.1.106" / "随影 0.1.106-xxx" → [0,1,106]；没有数字返回空数组。 */
 static int[] parse(String v){if(v==null)return new int[0];java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?").matcher(v);if(!m.find())return new int[0];List<Integer> out=new ArrayList<>();for(int i=1;i<=4;i++)if(m.group(i)!=null)out.add(Integer.parseInt(m.group(i)));int[] r=new int[out.size()];for(int i=0;i<r.length;i++)r[i]=out.get(i);return r;}
 static int compare(String a,String b){int[] x=parse(a),y=parse(b);for(int i=0;i<Math.max(x.length,y.length);i++){int p=i<x.length?x[i]:0,q=i<y.length?y[i]:0;if(p!=q)return Integer.compare(p,q);}return 0;}
 static boolean newer(String remote,String local){return parse(remote).length>0&&compare(remote,local)>0;}
 /** Release JSON → {version, apk 下载地址（没有安装包时为发布页）, 说明, 大小}；草稿 / 预发布跳过（latest 接口本来就不返回）。 */
 static String[] release(JSONObject r){
  if(r==null||r.optBoolean("draft")||r.optBoolean("prerelease"))return null;String tag=r.optString("tag_name",r.optString("name"));if(parse(tag).length==0)tag=r.optString("name");if(parse(tag).length==0)return null;
  String url=r.optString("html_url",PAGE),size="";JSONArray assets=r.optJSONArray("assets");
  for(int i=0;assets!=null&&i<assets.length();i++){JSONObject a=assets.optJSONObject(i);if(a==null)continue;String n=a.optString("name").toLowerCase(Locale.ROOT);if(n.endsWith(".apk")){url=a.optString("browser_download_url",url);long s=a.optLong("size");size=s>0?OfflineRules.size(s):"";break;}}
  String notes=r.optString("body","").trim();if(notes.length()>800)notes=notes.substring(0,800)+"…";
  String version=tag.replaceFirst("^[^0-9]*","");return new String[]{version,url,notes,size};
 }
}
