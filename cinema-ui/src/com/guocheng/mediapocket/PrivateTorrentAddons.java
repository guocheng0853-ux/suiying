package com.guocheng.mediapocket;
import android.content.Context;import org.json.*;import java.io.*;import java.net.*;import java.security.SecureRandom;import java.util.*;import java.util.concurrent.*;
/** Two private, removable Stremio add-ons served only on this device; never published to a market. */
final class PrivateTorrentAddons {
 static Server server;
 static synchronized Server server()throws IOException{if(server==null)server=new Server();return server;}
 static boolean ours(JSONObject addon){JSONObject m=addon.optJSONObject("manifest");String id=m==null?"":m.optString("id");return id.equals("com.suiying.private.eztv")||id.equals("com.suiying.private.piratebay");}
 static JSONArray sync(Context c,JSONArray existing,boolean restore)throws Exception{
  boolean first=!c.getSharedPreferences("data",0).getBoolean("privateTorrentAddons.v1",false);JSONArray out=new JSONArray();Set<String> wanted=new LinkedHashSet<>();
  for(int i=0;existing!=null&&i<existing.length();i++){JSONObject a=existing.optJSONObject(i);if(a==null)continue;if(ours(a)){String id=a.getJSONObject("manifest").getString("id");wanted.add(id.endsWith(".eztv")?TorrentProviders.EZTV:TorrentProviders.TPB);}else out.put(a);}
  if(first||restore){wanted.add(TorrentProviders.EZTV);wanted.add(TorrentProviders.TPB);}
  if(!wanted.isEmpty()){Server s=server();for(String provider:wanted){JSONObject manifest=TorrentProviders.manifest(provider);out.put(new JSONObject().put("base",s.prefix()+provider).put("name",manifest.getString("name")).put("manifest",manifest));}}
  c.getSharedPreferences("data",0).edit().putString("addons",out.toString()).putBoolean("privateTorrentAddons.v1",true).apply();return out;
 }
 static final class Server {
  final ServerSocket socket;final String token;
  final ExecutorService io=new ThreadPoolExecutor(0,4,30,TimeUnit.SECONDS,new SynchronousQueue<>(),r->{Thread t=new Thread(r,"private-stremio");t.setDaemon(true);return t;});
  Server()throws IOException{socket=new ServerSocket(0,16,InetAddress.getByName("127.0.0.1"));byte[] b=new byte[16];new SecureRandom().nextBytes(b);token=HeaderRoute.Base64Url.encode(b);Thread t=new Thread(this::accept,"private-stremio-accept");t.setDaemon(true);t.start();}
  String prefix(){return "http://127.0.0.1:"+socket.getLocalPort()+"/"+token+"/";}
  void accept(){while(!socket.isClosed())try{Socket c=socket.accept();try{io.execute(()->serve(c));}catch(RejectedExecutionException busy){c.close();}}catch(IOException e){if(socket.isClosed())return;}}
  void serve(Socket c){try(Socket client=c){client.setSoTimeout(5000);InputStream in=client.getInputStream();String line=HeaderRoute.Server.readLine(in);if(line==null)return;String[] req=line.split(" ");for(int i=0;i<40;i++){String h=HeaderRoute.Server.readLine(in);if(h==null||h.isEmpty())break;}OutputStream out=client.getOutputStream();
   if(req.length!=3||!req[0].equals("GET")){HeaderRoute.Server.status(out,405,"text/plain","GET required");return;}
   String[] path=req[1].split("/");if(path.length<4||!HeaderRoute.MessageDigestEq.equal(path[1],token)){HeaderRoute.Server.status(out,403,"text/plain","forbidden");return;}
   String provider=path[2];JSONObject response;
   try{if(path.length==4&&path[3].equals("manifest.json"))response=TorrentProviders.manifest(provider);
    else if(path.length==6&&path[3].equals("stream")&&path[5].endsWith(".json")){String id=URLDecoder.decode(path[5].substring(0,path[5].length()-5),"UTF-8");response=new JSONObject().put("streams",TorrentProviders.streams(provider,path[4],id,18000));}
    else{HeaderRoute.Server.status(out,404,"text/plain","not found");return;}}
   catch(Exception e){response=new JSONObject().put("error",e instanceof IllegalArgumentException?e.getMessage():"查询失败："+LinkProbe.reason(e));}
   byte[] body=response.toString().getBytes("UTF-8");Map<String,String> headers=new LinkedHashMap<>();headers.put("Access-Control-Allow-Origin","*");headers.put("Cache-Control","no-store");HeaderRoute.Server.header(out,200,"application/json; charset=utf-8",body.length,headers);out.write(body);out.flush();
  }catch(Exception ignored){}}
 }
}
