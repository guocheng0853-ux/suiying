package com.guocheng.mediapocket;
import java.net.*;import java.io.*;import java.util.*;import org.json.*;
/** Five bounded stages, no credential refresh, playback reports, or persistent account writes. */
final class EmbyHealthCheck {
 static final String[] TITLES={"域名解析","服务器连接","登录状态","播放信息","视频数据"};
 static final int WAIT=0,RUNNING=1,PASS=2,FAIL=3,SKIP=4;
 interface Backend {int resolve(String host)throws Exception;Response request(String method,String url,Map<String,String> headers,String body)throws Exception;void cancel();}
 interface Observer {void update(Row row);}
 interface MediaProbe {LinkProbe.Result probe(String url);}
 static final class Response {final int code;final String body;Response(int code,String body){this.code=code;this.body=body;}}
 static final class Row {final int index,state;final long ms;final String detail;Row(int index,int state,long ms,String detail){this.index=index;this.state=state;this.ms=ms;this.detail=detail;}}
 final JSONObject server;final String device;final Backend backend;final MediaProbe media;final Row[] rows=new Row[5];volatile boolean cancelled;long started;
 EmbyHealthCheck(JSONObject server,String device)throws JSONException{this(server,device,new EmbyHealthHttp(),url->LinkProbe.probe(url,Collections.emptyMap(),6000));}
 EmbyHealthCheck(JSONObject server,String device,Backend backend,MediaProbe media)throws JSONException{this.server=new JSONObject(server.toString());this.device=device;this.backend=backend;this.media=media;for(int i=0;i<5;i++)rows[i]=new Row(i,WAIT,0,"等待检查");}
 void cancel(){cancelled=true;backend.cancel();}
 void row(int i,int state,long start,String detail,Observer o){Row r=new Row(i,state,start==0?0:Math.max(0,(System.nanoTime()-start)/1000000),detail);rows[i]=r;if(o!=null)o.update(r);}
 void skipFrom(int i,String detail,Observer o){for(;i<5;i++)row(i,SKIP,0,detail,o);}
 void run(Observer o){
  if(cancelled){skipFrom(0,"已取消",o);return;}
  String base;try{base=EmbyApi.server(server.optString("url"));}catch(Exception e){row(0,FAIL,0,"服务器地址格式不正确",o);skipFrom(1,"请先编辑服务器地址",o);return;}
  long t=System.nanoTime();row(0,RUNNING,0,"正在解析主地址…",o);
  try{int n=backend.resolve(new URI(base).getHost());if(cancelled){skipFrom(0,"已取消",o);return;}row(0,PASS,t,"域名可解析（"+n+" 个地址）",o);}catch(Exception e){row(0,FAIL,t,reason(e),o);skipFrom(1,"域名解析未通过",o);return;}
  if(cancelled){skipFrom(1,"已取消",o);return;}
  t=System.nanoTime();row(1,RUNNING,0,"正在读取服务器公开信息…",o);
  try{JSONObject info=get(base,"GET","/System/Info/Public",null,false);if(!info.has("Version")&&!info.has("ServerName"))throw new IOException("schema");row(1,PASS,t,"服务可达，返回 Emby 信息",o);}catch(Exception e){row(1,FAIL,t,reason(e),o);}
  if(cancelled){skipFrom(2,"已取消",o);return;}
  t=System.nanoTime();row(2,RUNNING,0,"正在验证现有登录状态…",o);JSONArray views;
  try{if(server.optString("token").isEmpty()||server.optString("userId").isEmpty())throw new Failure("尚未登录，请编辑服务器登录");JSONObject response=get(base,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Views",null,true);views=response.optJSONArray("Items");if(views==null)throw new IOException("schema");row(2,PASS,t,"现有登录有效，媒体库接口可读",o);}catch(Exception e){row(2,FAIL,t,reason(e),o);skipFrom(3,"登录状态未通过",o);return;}
  if(cancelled){skipFrom(3,"已取消",o);return;}
  t=System.nanoTime();row(3,RUNNING,0,"正在选取一条影片并读取播放信息…",o);EmbyApi.Choice choice;
  try{
   String library="";for(int i=0;i<views.length();i++){JSONObject v=views.optJSONObject(i);if(v!=null&&EmbyApi.library(v)){library=v.optString("Id");if(!library.isEmpty())break;}}
   if(library.isEmpty()){skipFrom(3,"没有可用于检查的媒体库",o);return;}
   String user=server.optString("userId");JSONObject sample=get(base,"GET","/Users/"+EmbyApi.enc(user)+"/Items?ParentId="+EmbyApi.enc(library)+"&Recursive=true&IncludeItemTypes=Movie,Episode&Limit=1&Fields=MediaSources",null,true);JSONArray items=sample.optJSONArray("Items");
   if(items==null||items.length()==0){skipFrom(3,"媒体库没有可用于检查的影片",o);return;}
   JSONObject item=items.optJSONObject(0);if(item==null||item.optString("Id").isEmpty())throw new IOException("schema");
   if(cancelled){skipFrom(3,"已取消",o);return;}
   JSONObject body=EmbyApi.playbackInfoBody(user,0).put("IsPlayback",false).put("AutoOpenLiveStream",false).put("EnableTranscoding",false);
   JSONObject info=get(base,"POST","/Items/"+EmbyApi.enc(item.optString("Id"))+"/PlaybackInfo?UserId="+EmbyApi.enc(user)+"&IsPlayback=false&AutoOpenLiveStream=false",body.toString(),true);
   choice=EmbyApi.select(base,item.optString("Id"),server.optString("token"),info,null,EmbyApi.clientOf(server));row(3,PASS,t,"已取得一条影片的播放信息",o);
  }catch(Exception e){row(3,FAIL,t,reason(e),o);skipFrom(4,"播放信息未通过",o);return;}
  if(cancelled){skipFrom(4,"已取消",o);return;}
  if(!choice.direct){row(4,SKIP,0,"需要服务器转码，请在实际播放时验证",o);return;}
  t=System.nanoTime();row(4,RUNNING,0,"正在读取少量视频数据…",o);
  try{LinkProbe.Result r=media.probe(choice.url);if(cancelled){row(4,SKIP,0,"已取消",o);return;}row(4,r.media?PASS:FAIL,t,r.media?"已读到视频数据，解码能力需实际播放确认":probeReason(r),o);}catch(Exception e){row(4,FAIL,t,reason(e),o);}
 }
 JSONObject get(String base,String method,String path,String body,boolean auth)throws Exception{
  if(cancelled)throw new InterruptedIOException();Map<String,String> headers=new LinkedHashMap<>();headers.put("Accept","application/json");headers.put("User-Agent","MediaPocket/"+EmbyApi.VERSION);headers.put("X-Emby-Authorization",EmbyApi.authorization(EmbyApi.clientOf(server),"Android",device,EmbyApi.VERSION,auth?server.optString("token"):""));if(auth)headers.put("X-Emby-Token",server.optString("token"));
  Response r=backend.request(method,EmbyApi.join(base,path),headers,body);if(r.code<200||r.code>=300)throw new Failure(httpReason(r.code));String text=r.body==null?"":r.body.trim();if(text.startsWith("["))return new JSONObject().put("Items",new JSONArray(text));if(!text.startsWith("{"))throw new IOException("schema");return new JSONObject(text);
 }
 static String httpReason(int code){return "HTTP "+code+(code==401?"：登录已失效，请重新登录":code==403?"：服务器拒绝访问，请检查账号权限":code==404?"：接口或影片不存在":code==409?"：服务器存在播放会话限制":code>=500?"：服务器暂时出错":"：请求未成功");}
 static String reason(Exception e){if(e instanceof Failure)return e.getMessage();if(e instanceof java.util.concurrent.TimeoutException||e instanceof SocketTimeoutException)return "连接或读取超时";if(e instanceof UnknownHostException)return "域名解析失败";if(e instanceof javax.net.ssl.SSLException)return "安全连接未通过，请检查证书";if(e instanceof ConnectException)return "服务器端口无法连接";if(e instanceof InterruptedIOException)return "已取消或检查超时";if(e instanceof JSONException)return "服务器返回的信息格式不正确";return "检查未通过，请核对服务器或网络";}
 static String probeReason(LinkProbe.Result r){java.util.regex.Matcher m=java.util.regex.Pattern.compile("HTTP (\\d{3})").matcher(r.kind);if(m.find())return httpReason(Integer.parseInt(m.group(1)));return r.detail.contains("超时")?"视频数据检查超时":"未读到可识别的视频数据";}
 static String stateName(int s){return s==RUNNING?"检查中":s==PASS?"通过":s==FAIL?"未通过":s==SKIP?"未检查":"等待";}
 String report(){return report(rows);}
 static String report(Row[] rows){StringBuilder b=new StringBuilder("随影 Emby 片源体检\n检查范围：主地址及一条影片；未覆盖备用线路和实际解码。\n地址、账号、令牌、影片名称与编号均已隐藏。\n");for(Row r:rows)b.append('\n').append(TITLES[r.index]).append("：").append(stateName(r.state)).append(r.ms>0?"（"+r.ms+" ms）":"").append('\n').append(r.detail).append('\n');return b.toString();}
 static final class Failure extends IOException {Failure(String message){super(message);}}
}
