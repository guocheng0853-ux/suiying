package com.guocheng.mediapocket;
import android.content.*;import android.graphics.Typeface;import android.view.*;import android.widget.*;import java.util.*;import java.util.concurrent.*;import org.json.*;
/** Inline server-management extension; leaving this screen cancels the current check. */
final class EmbyDiagnostics {
 static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"emby-health");t.setDaemon(true);return t;});
 static final Map<EmbyLibraryActivity,Run> RUNS=new WeakHashMap<>();
 static void cancel(EmbyLibraryActivity a){Run r=RUNS.remove(a);if(r!=null)r.stop();}
 static void show(EmbyLibraryActivity a,JSONObject server){
  show(a,server,null,false);
 }
 static void show(EmbyLibraryActivity a,JSONObject server,EmbyHealthCheck.Row[] restored,boolean cancelled){
  if(server==null){a.servers();return;}a.screen("片源体检");
  try{Run r=new Run(a,server);if(restored!=null){System.arraycopy(restored,0,r.rows,0,5);r.busy=false;r.cancelled=cancelled;}RUNS.put(a,r);r.build();if(r.busy)r.future=IO.submit(()->{r.check.run(row->a.runOnUiThread(()->{if(r.current()&&!r.cancelled){r.rows[row.index]=row;r.render();}}));a.runOnUiThread(()->{if(r.current()&&!r.cancelled){r.busy=false;r.render();}});});}
  catch(Exception e){a.status.setText("无法开始体检，请重新打开服务器");}
 }
 static void save(EmbyLibraryActivity a,android.os.Bundle out){Run r=RUNS.get(a);if(r==null)return;out.putString("emby.health.server",r.server.optString("id"));out.putBoolean("emby.health.busy",r.busy);out.putBoolean("emby.health.cancelled",r.cancelled);try{JSONArray rows=new JSONArray();for(EmbyHealthCheck.Row row:r.rows)rows.put(new JSONObject().put("state",row.state).put("ms",row.ms).put("detail",row.detail));out.putString("emby.health.rows",rows.toString());}catch(JSONException ignored){}}
 static boolean restore(EmbyLibraryActivity a,android.os.Bundle saved){String id=saved==null?null:saved.getString("emby.health.server");if(id==null)return false;JSONObject server=EmbyStore.get(a,id);if(server==null)return false;EmbyHealthCheck.Row[] rows=null;if(!saved.getBoolean("emby.health.busy",true))try{JSONArray raw=new JSONArray(saved.getString("emby.health.rows"));if(raw.length()==5){rows=new EmbyHealthCheck.Row[5];for(int i=0;i<5;i++){JSONObject row=raw.getJSONObject(i);rows[i]=new EmbyHealthCheck.Row(i,row.getInt("state"),row.getLong("ms"),row.getString("detail"));}}}catch(Exception ignored){}show(a,server,rows,saved.getBoolean("emby.health.cancelled",false));return true;}
 static final class Run {
  final EmbyLibraryActivity a;final JSONObject server;final int epoch;final EmbyHealthCheck check;final EmbyHealthCheck.Row[] rows=new EmbyHealthCheck.Row[5];final TextView[] state=new TextView[5],detail=new TextView[5];final ProgressBar[] progress=new ProgressBar[5];Button action,copy;Future<?> future;boolean busy=true,cancelled;
  Run(EmbyLibraryActivity a,JSONObject server)throws JSONException{this.a=a;this.server=new JSONObject(server.toString());epoch=a.epoch;check=new EmbyHealthCheck(this.server,EmbyStore.deviceId(a));for(int i=0;i<5;i++)rows[i]=new EmbyHealthCheck.Row(i,EmbyHealthCheck.WAIT,0,"等待检查");}
  boolean current(){return !a.dead&&a.epoch==epoch&&RUNS.get(a)==this;}
  void stop(){cancelled=true;busy=false;check.cancel();if(future!=null)future.cancel(true);}
  void build(){
   a.status.setText(server.optString("name","Emby"));TextView intro=a.text("检查主地址及一条影片，结果仅代表当前状态。",Ui.BODY);a.body.addView(intro);TextView note=a.text("检查不会改写账号或观看进度；视频数据可读仍需实际播放确认。",Ui.SMALL);note.setTextColor(a.muted());a.body.addView(note);
   LinearLayout stages=new LinearLayout(a);stages.setOrientation(LinearLayout.VERTICAL);stages.setPadding(a.dp(16),0,a.dp(16),0);stages.setBackground(a.rounded(a.card(),Ui.R_M));LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,-2);sl.topMargin=a.dp(16);sl.bottomMargin=a.dp(16);a.body.addView(stages,sl);
   for(int i=0;i<5;i++){
    LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,a.dp(12),0,a.dp(12));LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);TextView title=a.text(EmbyHealthCheck.TITLES[i],Ui.BODY);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    progress[i]=new ProgressBar(a);progress[i].setIndeterminateTintList(android.content.res.ColorStateList.valueOf(a.clay?CinemaTheme.CLAY_DEEP:a.gold?CinemaTheme.GOLD:a.ink()));LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(a.dp(18),a.dp(18));pl.rightMargin=a.dp(8);heading.addView(progress[i],pl);state[i]=a.text("等待",Ui.SMALL);heading.addView(state[i]);row.addView(heading);
    detail[i]=a.text("等待检查",Ui.SMALL);detail[i].setTextColor(a.muted());row.addView(detail[i]);stages.addView(row,new LinearLayout.LayoutParams(-1,-2));if(i<4){View line=new View(a);line.setBackgroundColor(a.light?0x22141413:0x33ffffff);stages.addView(line,new LinearLayout.LayoutParams(-1,a.dp(1)));}
   }
   action=a.button("取消体检",()->{if(busy){stop();for(int i=0;i<5;i++)if(rows[i].state==EmbyHealthCheck.RUNNING||rows[i].state==EmbyHealthCheck.WAIT)rows[i]=new EmbyHealthCheck.Row(i,EmbyHealthCheck.SKIP,0,"已取消");render();}else show(a,EmbyStore.get(a,server.optString("id")));});action.setMinimumHeight(a.dp(48));action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(a.accentFill()));action.setTextColor(a.accentInk());a.body.addView(action,new LinearLayout.LayoutParams(-1,a.dp(48)));
   copy=a.button("复制脱敏报告",()->{((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Emby 片源体检",report()));Toast.makeText(a,"脱敏报告已复制",Toast.LENGTH_SHORT).show();});copy.setMinimumHeight(a.dp(48));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,a.dp(48));cp.topMargin=a.dp(8);a.body.addView(copy,cp);render();
  }
  String report(){return "随影 "+BackupActivity.version(a)+" · Android API "+android.os.Build.VERSION.SDK_INT+"\n"+EmbyHealthCheck.report(rows);}
  void render(){
   int passed=0,failed=0;for(int i=0;i<5;i++){EmbyHealthCheck.Row r=rows[i];if(r.state==EmbyHealthCheck.PASS)passed++;if(r.state==EmbyHealthCheck.FAIL)failed++;state[i].setText(EmbyHealthCheck.stateName(r.state)+(r.ms>0?"  "+r.ms+" ms":""));state[i].setTextColor(r.state==EmbyHealthCheck.RUNNING||r.state==EmbyHealthCheck.FAIL?(a.clay?CinemaTheme.CLAY_DEEP:a.gold?CinemaTheme.GOLD:a.ink()):a.muted());detail[i].setText(r.detail);progress[i].setVisibility(r.state==EmbyHealthCheck.RUNNING?View.VISIBLE:View.GONE);}
   a.status.setText(busy?"正在检查 "+server.optString("name","Emby") :cancelled?"体检已取消":"检查完成："+passed+" 项通过"+(failed>0?"，"+failed+" 项未通过":""));action.setText(busy?"取消体检":"重新体检");copy.setEnabled(!busy);
  }
 }
 private EmbyDiagnostics(){}
}
