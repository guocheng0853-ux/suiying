package com.guocheng.mediapocket;
import android.app.*;import android.content.SharedPreferences;import android.text.InputType;import android.view.View;import android.widget.*;import org.json.JSONObject;import java.util.ArrayList;
/** 「我的」和播放菜单里的弹幕、在线字幕、播放小开关。 */
final class PlayerFeatureSettings {
 private PlayerFeatureSettings(){}
 static void add(ScrapedMainActivity a){
  a.body.addView(a.button("弹弹play 弹幕",()->danmuAccount(a)));
  a.body.addView(a.button(DanmuController.label(a),()->{a.prefs.edit().putBoolean("danmu.shown",!a.prefs.getBoolean("danmu.shown",true)).apply();a.preferences();}));
  a.body.addView(a.button("弹幕样式与匹配",()->danmuStyle(a)));
  a.body.addView(a.button(assrtLabel(a.prefs),()->token(a,"射手 Token","subtitle.assrtToken","到 assrt.net 申请 Token，只保存在这台设备上。")));
  a.body.addView(a.button(openLabel(a.prefs),()->token(a,"OpenSubtitles API Key","subtitle.opensubtitlesKey","到 opensubtitles.com 申请 API Key，只保存在这台设备上。")));
  a.body.addView(a.button(whisperUrlLabel(a.prefs),()->whisperServer(a)));
  a.body.addView(a.button(whisperTokenLabel(a.prefs),()->token(a,"Whisper 令牌","whisper.token","云端 Whisper 服务的访问令牌，只保存在这台设备上。")));
  a.body.addView(a.button(a.prefs.getBoolean("pref.forceHighRefresh",false)?"强制高刷：开":"强制高刷：关",()->toggle(a,"pref.forceHighRefresh",false)));
  a.body.addView(a.button(a.prefs.getBoolean("pref.showBufferSpeed",false)?"缓冲速度：开":"缓冲速度：关",()->toggle(a,"pref.showBufferSpeed",false)));
  a.body.addView(a.button(PlaybackPrefs.rememberAspect(a.prefs)?"记忆画面比例：开":"记忆画面比例：关",()->toggle(a,"pref.rememberAspect",true)));
  a.body.addView(a.button(PlaybackPrefs.rememberBrightness(a.prefs)?"记忆亮度：开":"记忆亮度：关",()->toggle(a,"pref.rememberBrightness",false)));
  a.body.addView(a.button(PlayerSwitchRules.seekLabel(a.prefs.getInt("pref.seekSeconds",15)),()->seek(a)));
  a.body.addView(a.button(PlaybackPrefs.holdLabel(PlaybackPrefs.holdRate(a.prefs)),()->holdRate(a,true)));
 }
 static void toggle(ScrapedMainActivity a,String key,boolean fallback){a.prefs.edit().putBoolean(key,!a.prefs.getBoolean(key,fallback)).apply();a.preferences();}
 static String assrtLabel(SharedPreferences p){String v=p.getString("subtitle.assrtToken","");return v.isEmpty()?"射手 Token：未填写":"射手 Token：已填写";}
 static String openLabel(SharedPreferences p){String v=p.getString("subtitle.opensubtitlesKey","");return v.isEmpty()?"OpenSubtitles API Key：未填写":"OpenSubtitles API Key：已填写";}
 static void danmuAccount(Activity a){SharedPreferences p=a.getSharedPreferences("data",0);LinearLayout box=form(a);EditText id=field(a,"AppId",p.getString("danmu.appId",""));EditText secret=field(a,"AppSecret",p.getString("danmu.appSecret",""));secret.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);box.addView(id);box.addView(secret);TextView note=new TextView(a);note.setText("到弹弹play开放平台申请自己的应用。留空则播放时提示去填写，不会使用内置密钥。");box.addView(note);new AlertDialog.Builder(a).setTitle("弹弹play 弹幕").setView(box).setPositiveButton("保存",(d,w)->{String app=id.getText().toString().trim(),sec=secret.getText().toString().trim();String why=app.isEmpty()&&sec.isEmpty()?null:DanDanPlayRules.reason(app,sec);if(why!=null&&!(app.isEmpty()&&sec.isEmpty())){Toast.makeText(a,why,Toast.LENGTH_LONG).show();return;}p.edit().putString("danmu.appId",app).putString("danmu.appSecret",sec).apply();Toast.makeText(a,app.isEmpty()?"已清空弹幕密钥":"弹幕密钥已保存",Toast.LENGTH_SHORT).show();if(a instanceof ScrapedMainActivity)((ScrapedMainActivity)a).preferences();}).setNegativeButton("取消",null).show();}
 static void danmuStyle(Activity a){SharedPreferences p=a.getSharedPreferences("data",0);DanmuRules.Config c=DanmuController.config(p);String[] items={"透明度 "+c.opacity+"%","滚动速度 "+(int)c.speedSec+" 秒","字号 "+c.textSp,"顶部行数 "+c.topLines,"滚动行数 "+c.scrollLines,"底部行数 "+c.bottomLines,"屏蔽词"+(c.block.isEmpty()?"：未设置":"：已设置"),c.match.equals("manual")?"匹配：手动选集":"匹配：自动匹配",c.remember?"记住每片选择：开":"记住每片选择：关",c.shown?"显示弹幕：开":"显示弹幕：关"};java.util.List<CinemaMenu.Item> rows=new ArrayList<>();rows.add(CinemaMenu.item(items[0],()->pick(a,"透明度",new String[]{"40%","60%","80%","100%"},new int[]{40,60,80,100},c.opacity,"danmu.opacity")));rows.add(CinemaMenu.item(items[1],()->pickFloat(a,"滚动速度",new String[]{"4 秒（快）","8 秒","12 秒","16 秒（慢）"},new float[]{4f,8f,12f,16f},c.speedSec)));rows.add(CinemaMenu.item(items[2],()->pick(a,"字号",new String[]{"14","16","20","24"},new int[]{14,16,20,24},c.textSp,"danmu.textSp")));rows.add(CinemaMenu.item(items[3],()->pick(a,"顶部行数",new String[]{"0","2","3","5"},new int[]{0,2,3,5},c.topLines,"danmu.topLines")));rows.add(CinemaMenu.item(items[4],()->pick(a,"滚动行数",new String[]{"0","4","8","12"},new int[]{0,4,8,12},c.scrollLines,"danmu.scrollLines")));rows.add(CinemaMenu.item(items[5],()->pick(a,"底部行数",new String[]{"0","2","3","5"},new int[]{0,2,3,5},c.bottomLines,"danmu.bottomLines")));rows.add(CinemaMenu.item(items[6],()->words(a)));rows.add(CinemaMenu.item(items[7],()->{p.edit().putString("danmu.match",c.match.equals("manual")?"auto":"manual").apply();danmuStyle(a);}));rows.add(CinemaMenu.item(items[8],()->{p.edit().putBoolean("danmu.remember",!c.remember).apply();danmuStyle(a);}));rows.add(CinemaMenu.item(items[9],()->{p.edit().putBoolean("danmu.shown",!c.shown).apply();danmuStyle(a);}));CinemaMenu.show(a,"弹幕设置",rows);}
 static String whisperUrlLabel(SharedPreferences p){String v=p.getString("whisper.url","");return v.isEmpty()?"Whisper 服务器：未填写":"Whisper 服务器：已填写";}
 static String whisperTokenLabel(SharedPreferences p){String v=p.getString("whisper.token","");return v.isEmpty()?"Whisper 令牌：未填写":"Whisper 令牌：已填写";}
 static void whisperServer(ScrapedMainActivity a){EditText input=field(a,"http://服务器地址:端口",a.prefs.getString("whisper.url",""));input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);new AlertDialog.Builder(a).setTitle("Whisper 服务器").setView(input).setPositiveButton("保存",(d,w)->{a.prefs.edit().putString("whisper.url",input.getText().toString().trim()).apply();a.preferences();}).setNeutralButton("清空",(d,w)->{a.prefs.edit().putString("whisper.url","").apply();a.preferences();}).setNegativeButton("取消",null).show();}
 /** 当前播放的视频发到云端 Whisper 转写，完成后自动加载并记住。磁力/P2P 源云端取不到。 */
 static void whisper(final Activity a){
  SharedPreferences p=a.getSharedPreferences("data",0);
  String base=WhisperClient.trim(p.getString("whisper.url",""));
  if(base.isEmpty()){Toast.makeText(a,"先在「我的」里填 Whisper 服务器地址",Toast.LENGTH_LONG).show();return;}
  String token=p.getString("whisper.token","");
  JSONObject stream=TrackSelection.stream(a);String media=stream==null?"":stream.optString("url","");
  if(!media.startsWith("http://")&&!media.startsWith("https://")){Toast.makeText(a,"当前是磁力/P2P 源，云端取不到视频，请换直链线路",Toast.LENGTH_LONG).show();return;}
  final String fBase=base,fMedia=media,fToken=token;
  final android.app.ProgressDialog wait=android.app.ProgressDialog.show(a,"Whisper 生成字幕","提交任务…",true,true);
  final boolean[] cancel={false};wait.setOnCancelListener(d->cancel[0]=true);
  final long startedAt=android.os.SystemClock.elapsedRealtime();
  OnlineSubtitleSearch.IO.execute(()->{
   try{
    String resp=WhisperClient.post(WhisperClient.jobUrl(fBase),fToken,WhisperClient.jobBody(fMedia,p.getString("whisper.model","base")));
    final String jid=WhisperClient.jobId(resp);
    if(jid.isEmpty())throw new java.io.IOException("服务器没有返回任务号");
    String status="queued";long lastUi=0;
    while(!cancel[0]){
     Thread.sleep(5000);
     resp=WhisperClient.get(WhisperClient.jobUrl(fBase)+"/"+jid,fToken);
     status=WhisperClient.status(resp);
     long now=android.os.SystemClock.elapsedRealtime();
     if(now-lastUi>4000){lastUi=now;final String msg=WhisperClient.statusLabel(status)+" · 已等待 "+((now-startedAt)/60000)+" 分 "+((now-startedAt)/1000%60)+" 秒";
      a.runOnUiThread(()->{try{if(wait.isShowing())wait.setMessage(msg);}catch(Exception ignored){}});}
     if(status.equals("done"))break;
     if(status.equals("failed")){String err=WhisperClient.error(resp);throw new java.io.IOException(err==null||err.isEmpty()?"转写失败":err);}
    }
    if(cancel[0])return;
    final String srt=WhisperClient.get(WhisperClient.srtUrl(fBase,jid),fToken);
    final byte[] bytes=srt.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    a.runOnUiThread(()->{
     if(a.isFinishing()||cancel[0])return;
     try{java.io.File saved=ExternalSubtitleStore.save(a,PlayerOverlay.name(a),OnlineSubtitleRules.file("Whisper · "+PlayerOverlay.name(a),bytes),bytes);
      ExternalSubtitleStore.remember(p,PlayerOverlay.id(a),saved.getAbsolutePath(),"whisper");
      SourceSubtitleLoader.showLocal(a,bytes,"Whisper 转写",saved.getAbsolutePath());
     }catch(Exception e){OnlineSubtitleSearch.message(a,"字幕保存失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
    });
   }catch(Exception e){
    a.runOnUiThread(()->{if(cancel[0]||a.isFinishing())return;
     android.app.AlertDialog.Builder b=new android.app.AlertDialog.Builder(a).setTitle("Whisper 生成字幕").setMessage("失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage())).setPositiveButton("确定",null);
     try{b.show();}catch(Exception ignored){}});
   }
  });
 }
 static void words(Activity a){SharedPreferences p=a.getSharedPreferences("data",0);EditText input=field(a,"用逗号分开",p.getString("danmu.block",""));new AlertDialog.Builder(a).setTitle("屏蔽词").setView(input).setPositiveButton("保存",(d,w)->p.edit().putString("danmu.block",input.getText().toString().trim()).apply()).setNegativeButton("取消",null).show();}
 static void pick(Activity a,String title,String[] labels,int[] values,int current,String key){int at=0;for(int i=0;i<values.length;i++)if(values[i]==current)at=i;java.util.List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<labels.length;i++){final int n=i;rows.add(CinemaMenu.choice(labels[i],values[i]==current,()->{a.getSharedPreferences("data",0).edit().putInt(key,values[n]).apply();danmuStyle(a);}));}CinemaMenu.show(a,title,rows);}
 static void pickFloat(Activity a,String title,String[] labels,float[] values,float current){int at=0;for(int i=0;i<values.length;i++)if(values[i]==current)at=i;java.util.List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<labels.length;i++){final int n=i;rows.add(CinemaMenu.choice(labels[i],values[i]==current,()->{a.getSharedPreferences("data",0).edit().putFloat("danmu.speed",values[n]).apply();danmuStyle(a);}));}CinemaMenu.show(a,title,rows);}
 static void token(ScrapedMainActivity a,String title,String key,String hint){EditText input=field(a,hint,a.prefs.getString(key,""));input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);new AlertDialog.Builder(a).setTitle(title).setView(input).setPositiveButton("保存",(d,w)->{a.prefs.edit().putString(key,input.getText().toString().trim()).apply();a.preferences();}).setNeutralButton("清空",(d,w)->{a.prefs.edit().putString(key,"").apply();a.preferences();}).setNegativeButton("取消",null).show();}
 static void seek(Activity a){SharedPreferences p=a instanceof ScrapedMainActivity?((ScrapedMainActivity)a).prefs:a.getSharedPreferences("data",0);int cur=PlayerSwitchRules.seekSeconds(p.getInt("pref.seekSeconds",15));String[] labels={"15 秒","30 秒","60 秒"};int at=cur==30?1:cur==60?2:0;java.util.List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<labels.length;i++){final int n=i;rows.add(CinemaMenu.choice(labels[i],i==at,()->{p.edit().putInt("pref.seekSeconds",PlayerSwitchRules.SEEK_SECONDS[n]).apply();if(a instanceof ScrapedMainActivity)((ScrapedMainActivity)a).preferences();}));}CinemaMenu.show(a,"快进快退",rows);}
 static void holdRate(Activity a,boolean fromSettings){SharedPreferences p=a.getSharedPreferences("data",0);float cur=PlaybackPrefs.holdRate(p);String[] labels={"1.5×","2.0×","2.5×","3.0×"};float[] values=PlaybackPrefs.HOLD_RATES;int at=0;for(int i=0;i<values.length;i++)if(Math.abs(values[i]-cur)<0.05f)at=i;java.util.List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<labels.length;i++){final int n=i;rows.add(CinemaMenu.choice(labels[i],Math.abs(values[i]-cur)<0.05f,()->{PlaybackPrefs.setHoldRate(p,values[n]);if(fromSettings&&a instanceof ScrapedMainActivity)((ScrapedMainActivity)a).preferences();else playback(a);}));}CinemaMenu.show(a,"长按加速",rows);}
 static void playback(Activity a){SharedPreferences p=a.getSharedPreferences("data",0);String[] items={p.getBoolean("pref.forceHighRefresh",false)?"强制高刷：开":"强制高刷：关",p.getBoolean("pref.showBufferSpeed",false)?"缓冲速度：开":"缓冲速度：关",PlaybackPrefs.rememberAspect(p)?"记忆画面比例：开":"记忆画面比例：关",PlaybackPrefs.rememberBrightness(p)?"记忆亮度：开":"记忆亮度：关",PlayerSwitchRules.seekLabel(p.getInt("pref.seekSeconds",15)),PlaybackPrefs.holdLabel(PlaybackPrefs.holdRate(p))};java.util.List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<items.length;i++){final int n=i;rows.add(CinemaMenu.item(items[i],n<4?()->{String key=new String[]{"pref.forceHighRefresh","pref.showBufferSpeed","pref.rememberAspect","pref.rememberBrightness"}[n];boolean fallback=n==2;p.edit().putBoolean(key,!p.getBoolean(key,fallback)).apply();PlayerSwitches.REFRESH.remove(a);playback(a);}:n==4?()->seek(a):()->holdRate(a,false)));}CinemaMenu.show(a,"播放设置",rows);}
 static LinearLayout form(Activity a){LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*a.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,0);return box;}
 static EditText field(Activity a,String hint,String value){EditText input=new EditText(a);input.setSingleLine(true);input.setHint(hint);input.setText(value);return input;}
}
