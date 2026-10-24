package com.guocheng.mediapocket;
import android.app.Activity;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;
/** 0.1.106「离线下载」页：列出下载记录和进度，点已下载的直接播放，长按删除。每秒刷新一次进度。 */
public final class DownloadsActivity extends Activity {
 LinearLayout list;TextView empty;boolean light,gold,clay;final Handler tick=new Handler(Looper.getMainLooper());
 final Runnable refresh=new Runnable(){public void run(){render();tick.postDelayed(this,1000);}};
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 int ink(){return light?(clay?CinemaTheme.SLATE:0xff173740):gold?CinemaTheme.INK:0xfff5fafb;}int muted(){return light?(clay?CinemaTheme.STONE:0xff52757c):gold?CinemaTheme.MUTED:0xffafcbd2;}int surface(){return light?(clay?CinemaTheme.PAPER:0xffedf5f5):gold?CinemaTheme.BG:0xff10343e;}int card(){return light?(clay?CinemaTheme.SHEET:0xffffffff):gold?CinemaTheme.CARD:0xff19434e;}
 @Override protected void onCreate(Bundle saved){light=CinemaTheme.light(this);gold=!light&&CinemaTheme.gold(this);clay=light&&CinemaTheme.clay(this);getTheme().applyStyle(light?android.R.style.Theme_Material_Light_NoActionBar:android.R.style.Theme_Material_NoActionBar,true);super.onCreate(saved);
  getWindow().setStatusBarColor(surface());getWindow().setNavigationBarColor(surface());CinemaTheme.barIcons(this,light);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(surface());root.setPadding(dp(16),dp(8),dp(16),0);setContentView(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);Button back=new Button(this);back.setText("‹ 返回");back.setOnClickListener(v->finish());header.addView(back);TextView title=text("离线下载",18,ink());if(clay||gold)CinemaTheme.section(title,this,20);else title.setTypeface(null,1);title.setPadding(dp(12),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
  TextView hint=text("点已下载的直接播放（不用联网），下载中点一下暂停、再点继续，失败的点一下重试，长按删除。文件存在本应用目录里，卸载随影时会一起删除。",12,muted());hint.setPadding(0,dp(6),0,dp(10));root.addView(hint);
  ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  empty=text("还没有下载。\n\n在 Emby 电影 / 单集详情点右上角「⬇ 下载」，或在选集上长按某一集即可下载。磁力和 m3u8 分段流暂不支持。",14,muted());empty.setPadding(0,dp(24),0,0);
}
 @Override protected void onResume(){super.onResume();OfflineDownloads.resumeStale(this);tick.post(refresh);}
 @Override protected void onPause(){super.onPause();tick.removeCallbacks(refresh);}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(Ui.sp(size));t.setTextColor(color);return t;}
 void render(){JSONArray all=OfflineDownloads.list(this);
  if(all.length()==0){if(empty.getParent()==null){list.removeAllViews();list.addView(empty);}return;}
  if(list.getChildCount()!=all.length()||empty.getParent()!=null){list.removeAllViews();for(int i=0;i<all.length();i++)list.addView(row(all.optJSONObject(i)));}
  for(int i=0;i<all.length()&&i<list.getChildCount();i++){JSONObject rec=all.optJSONObject(i);View v=list.getChildAt(i);if(rec==null||!(v instanceof LinearLayout))continue;Object[] st=OfflineDownloads.describe(this,rec);TextView line=(TextView)((LinearLayout)v).getChildAt(1);line.setText((String)st[0]);v.setTag(OfflineDownloads.key(rec));}}
 View row(JSONObject rec){LinearLayout r=new LinearLayout(this);r.setOrientation(1);r.setPadding(dp(14),dp(12),dp(14),dp(12));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(card());bg.setCornerRadius(dp(Ui.radius(12)));r.setBackground(bg);r.setFocusable(true);r.setClickable(true);
  TextView name=text(rec==null?"":rec.optString("name","离线影片"),15,ink());name.setMaxLines(2);r.addView(name);TextView line=text("",12,muted());line.setPadding(0,dp(4),0,0);r.addView(line);
  r.setOnClickListener(v->{JSONObject now=rec==null?null:OfflineDownloads.find(this,OfflineDownloads.key(rec));if(now==null)return;Object[] st=OfflineDownloads.describe(this,now);String status=now.optString("status");if(Boolean.TRUE.equals(st[1]))OfflineDownloads.play(this,now);else if("running".equals(status)||"queued".equals(status)){OfflineDownloads.pause(this,now);render();}else if("paused".equals(status)||"failed".equals(status)){OfflineDownloads.resume(this,now);render();}else Toast.makeText(this,"还没下载完",Toast.LENGTH_SHORT).show();});
  r.setOnLongClickListener(v->{JSONObject cur=rec==null?null:OfflineDownloads.find(this,OfflineDownloads.key(rec));if(cur==null)return true;final JSONObject rec2=cur;new CinemaDialog(this).setTitle("删除下载？").setMessage(rec2.optString("name")+"\n\n正在下载的会取消，已下载的文件会删除；观看进度保留。").setPositiveButton("删除",(d,w)->{OfflineDownloads.remove(this,rec2);list.removeAllViews();render();}).setNegativeButton("取消",null).show();return true;});
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);r.setLayoutParams(p);return r;}
}
