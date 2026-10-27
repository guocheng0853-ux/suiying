package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.os.SystemClock;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/** 剧集自动下一集：片尾弹出 5 秒倒计时卡片（可取消），到点后回到主界面按 EpisodeActions 的下一集查询片源并开播。 */
final class AutoNext {
 static final Map<Activity,AutoNext> ALL=new WeakHashMap<>();
 /** 交给主界面的请求（同进程内存，不落盘）。 */
 static JSONObject pendingMeta;static String pendingId,pendingType;
 final Activity a;String forId,nextId;boolean cancelled;long shownAt;LinearLayout card;TextView text;
 AutoNext(Activity a){this.a=a;}
 static void tick(Activity a,boolean pip){try{AutoNext n=ALL.get(a);if(n==null){n=new AutoNext(a);ALL.put(a,n);}n.poll(pip);}catch(Exception ignored){}}
 int dp(int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 int intCall(Object v,String name){try{Object r=v==null?null:OnlineSubtitleSearch.call(v,name,new Class<?>[0]);return r instanceof Integer?(Integer)r:-1;}catch(Exception e){return -1;}}
 void poll(boolean pip){if(a.isFinishing())return;if(EmbyPlayerUi.ownsNext(a)){hide();return;}Object idv=OnlineSubtitleSearch.field(a,"id"),type=OnlineSubtitleSearch.field(a,"type"),meta=OnlineSubtitleSearch.field(a,"meta");String id=idv==null?"":idv.toString();
  if(!id.equals(forId)){forId=id;cancelled=false;hide();nextId=meta instanceof JSONObject&&!id.startsWith("tvbox:")?AutoNextRules.next((JSONObject)meta,id):null;}
  SharedPreferences p=a.getSharedPreferences("data",0);Object video=OnlineSubtitleSearch.field(a,"video");boolean ended=Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"terminal"))&&!PlaybackRepair.premature(a);
  boolean want=!pip&&AutoNextRules.offer(PlaybackPrefs.autoNext(p),PlaybackReviewRules.series(meta instanceof JSONObject?(JSONObject)meta:null,type==null?"":type.toString()),nextId!=null,cancelled,ended,intCall(video,"getCurrentPosition"),intCall(video,"getDuration"),PlaybackPrefs.autoNextLead(p));
  if(!want){if(card!=null&&!ended)hide();return;}
  if(card==null){show((JSONObject)meta);shownAt=SystemClock.elapsedRealtime();}
  int left=AutoNextRules.secondsLeft(SystemClock.elapsedRealtime()-shownAt);text.setText("下一集 · "+AutoNextRules.label((JSONObject)meta,nextId)+"\n"+left+" 秒后自动播放");place();if(left==0)go((JSONObject)meta,type.toString());}
 void place(){if(card==null||!(card.getLayoutParams() instanceof FrameLayout.LayoutParams))return;Object bottom=OnlineSubtitleSearch.field(a,"bottom");int h=bottom instanceof View&&((View)bottom).getVisibility()==View.VISIBLE?((View)bottom).getHeight():0;if(h<dp(120))h=dp(168);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)card.getLayoutParams();int want=h+dp(12);if(lp.bottomMargin==want&&lp.rightMargin==dp(76))return;lp.bottomMargin=want;lp.rightMargin=dp(76);card.setLayoutParams(lp);}
 Button button(String t,View.OnClickListener l){Button b=new Button(a);b.setText(t);b.setAllCaps(false);b.setTextColor(0xffffffff);b.setBackground(PlayerTheme.card(a.getResources().getDisplayMetrics().density));b.setOnClickListener(l);return b;}
 void show(JSONObject meta){Object root=OnlineSubtitleSearch.field(a,"root");if(!(root instanceof FrameLayout))return;card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(PlayerTheme.card(a.getResources().getDisplayMetrics().density));card.setClipToOutline(true);text=new TextView(a);text.setTextColor(0xffffffff);text.setTextSize(15);card.addView(text);LinearLayout row=new LinearLayout(a);row.addView(button("取消",v->{cancelled=true;hide();}));LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-2,-2);gap.leftMargin=dp(10);row.addView(button("立即播放",v->go(meta,String.valueOf(OnlineSubtitleSearch.field(a,"type")))),gap);card.addView(row);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.BOTTOM);lp.rightMargin=dp(24);lp.bottomMargin=dp(96);((FrameLayout)root).addView(card,lp);}
 void hide(){if(card!=null&&card.getParent() instanceof ViewGroup)((ViewGroup)card.getParent()).removeView(card);card=null;}
 void go(JSONObject meta,String type){if(nextId==null||a.isFinishing())return;cancelled=true;hide();handoff(a,meta,nextId,type);}
 static void handoff(Activity a,JSONObject meta,String nextId,String type){if(nextId==null||a==null||a.isFinishing())return;AutoNext n=ALL.get(a);if(n!=null){n.cancelled=true;n.hide();}
  // Emby library pages do not consume the main Activity's static pending request.
  // Keep this player until playItem starts its replacement successfully.
  EpisodeHandoff.dispatch(nextId,id->EmbyPlayback.switchEpisode(a,id),()->{pendingMeta=meta;pendingId=nextId;pendingType=type;a.finish();});}
 /** 主界面 onResume 调用：取出请求，返回 [id,type] 与 meta。 */
 static Object[] consume(){if(pendingId==null)return null;Object[] r={pendingId,pendingType,pendingMeta};pendingId=null;pendingType=null;pendingMeta=null;return r;}
}
