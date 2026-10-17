package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.zip.*;
/** 射手与 OpenSubtitles 的查询、结果解析和落盘文件名。密钥只由调用方传入。 */
final class OnlineSubtitleRules {
 static final String ASSRT="https://api.assrt.net/v1/sub/search";
 static final String ASSRT_DETAIL="https://api.assrt.net/v1/sub/detail";
 static final String OPEN="https://api.opensubtitles.com/api/v1/subtitles";
 static final String OPEN_DOWNLOAD="https://api.opensubtitles.com/api/v1/download";
 static final String UA="suiying/0.1.80";
 private OnlineSubtitleRules(){}
 static boolean assrtReady(String token){return token!=null&&!token.trim().isEmpty();}
 static boolean openReady(String key){return key!=null&&!key.trim().isEmpty();}
 static String missing(String assrt,String open){if(assrtReady(assrt)||openReady(open))return null;return "在线字幕还没有密钥。请到「我的」里填写射手 Token 或 OpenSubtitles API Key。";}
 static String assrtSearch(String token,String query){return ASSRT+"?token="+enc(token.trim())+"&q="+enc(query==null?"":query)+"&cnt=20";}
 static String assrtDetail(String token,long id){return ASSRT_DETAIL+"?token="+enc(token.trim())+"&id="+id;}
 static String openSearch(String query){return OPEN+"?query="+enc(query==null?"":query)+"&languages=zh-cn,zh,en";}
 static String enc(String raw){try{return java.net.URLEncoder.encode(raw,"UTF-8");}catch(Exception e){return "";}}
 static final class Hit {final String source,name,url;final long id,fileId;Hit(String source,String name,long id,long fileId,String url){this.source=source;this.name=name==null?source:name;this.id=id;this.fileId=fileId;this.url=url==null?"":url;}
  String label(){return ("assrt".equals(source)?"射手":"OpenSubtitles")+" · "+name;}}
 static List<Hit> assrt(JSONObject json){ArrayList<Hit> out=new ArrayList<>();if(json==null||json.optInt("status",0)!=0)return out;JSONObject sub=json.optJSONObject("sub");JSONArray subs=sub==null?null:sub.optJSONArray("subs");if(subs==null)return out;for(int i=0;i<subs.length()&&out.size()<30;i++){JSONObject s=subs.optJSONObject(i);if(s==null)continue;long id=s.optLong("id");if(id<=0)continue;JSONObject lang=s.optJSONObject("lang");String desc=lang==null?"":lang.optString("desc");String name=s.optString("native_name",s.optString("videoname","字幕"));if(!desc.isEmpty())name=desc+" · "+name;out.add(new Hit("assrt",name,id,0,""));}return out;}
 static String assrtFile(JSONObject json){if(json==null||json.optInt("status",0)!=0)return "";JSONObject sub=json.optJSONObject("sub");JSONArray subs=sub==null?null:sub.optJSONArray("subs");if(subs==null)return "";for(int i=0;i<subs.length();i++){JSONObject s=subs.optJSONObject(i);if(s==null)continue;JSONArray files=s.optJSONArray("filelist");if(files==null)continue;for(int j=0;j<files.length();j++){JSONObject f=files.optJSONObject(j);if(f==null)continue;String url=f.optString("url");if(url.startsWith("https://")||url.startsWith("http://"))return url;}}return "";}
 static List<Hit> opensubtitles(JSONObject json){ArrayList<Hit> out=new ArrayList<>();if(json==null)return out;JSONArray data=json.optJSONArray("data");if(data==null)return out;for(int i=0;i<data.length()&&out.size()<30;i++){JSONObject row=data.optJSONObject(i);if(row==null)continue;JSONObject attr=row.optJSONObject("attributes");if(attr==null)continue;JSONArray files=attr.optJSONArray("files");JSONObject file=files==null||files.length()==0?null:files.optJSONObject(0);long fileId=file==null?0:file.optLong("file_id");if(fileId<=0)continue;String lang=attr.optString("language","字幕");String release=attr.optString("release",file.optString("file_name","字幕"));out.add(new Hit("opensubtitles",lang+" · "+release,row.optLong("id"),fileId,""));}return out;}
 static String openLink(JSONObject json){if(json==null)return "";String link=json.optString("link");return link.startsWith("https://")||link.startsWith("http://")?link:"";}
 static String folder(String title){String s=title==null?"":title.trim();if(s.isEmpty())s="未命名";s=s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").replaceAll("\\s+"," ");if(s.length()>80)s=s.substring(0,80);return s;}
 static String file(String name){String s=name==null?"":name.trim();int slash=Math.max(s.lastIndexOf('/'),s.lastIndexOf('\\'));if(slash>=0)s=s.substring(slash+1);s=s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_");if(s.isEmpty()||s.equals(".")||s.equals(".."))s="subtitle.srt";if(s.length()>96)s=s.substring(s.length()-96);String lower=s.toLowerCase(Locale.ROOT);if(!(lower.endsWith(".srt")||lower.endsWith(".ass")||lower.endsWith(".ssa")||lower.endsWith(".vtt")||lower.endsWith(".zip")))s=s+".srt";return s;}
 /** 压缩包里挑第一条字幕，优先 ASS，其次 SRT / VTT。不是压缩包就原样返回。 */
 static byte[] unwrap(byte[] raw)throws IOException{if(raw==null||raw.length<4||raw[0]!='P'||raw[1]!='K')return raw;byte[] best=null;int bestScore=-1;try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(raw))){ZipEntry entry;byte[] buf=new byte[4096];while((entry=in.getNextEntry())!=null){if(entry.isDirectory())continue;String name=entry.getName().toLowerCase(Locale.ROOT);int score=name.endsWith(".ass")||name.endsWith(".ssa")?3:name.endsWith(".srt")?2:name.endsWith(".vtt")?1:0;if(score==0)continue;ByteArrayOutputStream out=new ByteArrayOutputStream();int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>2*1024*1024)throw new IOException("字幕文件过大");out.write(buf,0,n);}if(score>bestScore){bestScore=score;best=out.toByteArray();}}}if(best==null)throw new IOException("压缩包里没有可用字幕");return best;}
 static String choice(String source,long id,long fileId){return source+":"+(fileId>0?fileId:id);}
}
