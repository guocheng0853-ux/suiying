package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.drawable.GradientDrawable;import android.os.Handler;import android.os.Looper;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 右侧滑出的统一面板。宽 480dp，小屏放不下时用宽度的 45%。
 * 页签：字幕、音轨、选集、线路、章节（有章节时）、码率（Emby）、定时关闭、流信息。
 * 0.1.93：这里是“当前这部片子”的唯一入口：获取字幕（在线 / Whisper / 插件）在字幕页顶部，
 * 换片源 / 换版本 / 换线路在线路页。「更多」只留弹幕、播放设置和播放器级设置，不再重复列这些。
 * OpenSubtitles 需要 API 密钥，这里不接，改走已经安装的字幕插件。
 */
final class PlayerSidePanel {
 static final String SUB="sub",AUDIO="audio",EPISODES="episodes",CHAPTERS="chapters",BITRATE="bitrate",ROUTES="routes",SLEEP="sleep",INFO="info";
 static final Map<Activity,PlayerSidePanel> ALL=new WeakHashMap<>();
 static final long[] RATES={500_000L,1_000_000L,2_000_000L,4_000_000L,8_000_000L,20_000_000L,40_000_000L,140_000_000L};
 static final Handler TIMER=new Handler(Looper.getMainLooper());
 static Runnable sleepTask;static long sleepDeadline;
 PlayerPanelBack back;
 final Activity a;FrameLayout scrim;LinearLayout sheet;android.app.Dialog dialog;String tab=SUB;TextView body;HorizontalScrollView tabs;int seasonPick=-2,segmentPick=-1;
 PlayerSidePanel(Activity a){this.a=a;}
 static boolean open(Activity a){PlayerSidePanel p=ALL.get(a);return p!=null&&p.dialog!=null&&p.dialog.isShowing();}
 static void dispose(Activity a){PlayerSidePanel p=ALL.remove(a);if(p!=null){if(p.back!=null)p.back.dispose();p.back=null;if(p.sheet!=null)p.sheet.animate().cancel();if(p.dialog!=null)try{p.dialog.dismiss();}catch(RuntimeException ignored){}p.dialog=null;p.scrim=null;p.sheet=null;}}
 static void close(Activity a){PlayerSidePanel p=ALL.get(a);if(p!=null)p.dismiss();}
 static void open(Activity a,String tab){if(SLEEP.equals(tab)){sleepMenu(a);return;}if(INFO.equals(tab)){infoDialog(a);return;}if(BITRATE.equals(tab))tab=ROUTES;try{PlayerSidePanel p=ALL.get(a);if(p==null){p=new PlayerSidePanel(a);ALL.put(a,p);}p.show(tab==null||tab.isEmpty()?SUB:tab);}catch(Exception ignored){}}
 int dp(int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 void show(String next){tab=next;if(EPISODES.equals(tab))segmentPick=-1;if(a.isFinishing())return;if(dialog==null)build();fill();if(!dialog.isShowing()){sheet.setAlpha(0f);sheet.setTranslationX(dp(24));if(!Glass.showImmersive(a,dialog)){dialog=null;scrim=null;sheet=null;return;}sheet.animate().alpha(1f).translationX(0).setDuration(PlayerMotionRules.PANEL_IN_MS).start();}}

 /** 0.1.169：面板是一个只有面板大小的独立窗口（雾面玻璃，见 Glass），靠右、上下各留 8dp；点面板外面关闭。 */
 void build(){
  dialog=new android.app.Dialog(a,android.R.style.Theme_Translucent_NoTitleBar);dialog.setCanceledOnTouchOutside(true);dialog.setOnCancelListener(d->dismiss());
  scrim=new FrameLayout(a);
  sheet=new LinearLayout(a);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setClickable(true);sheet.setPadding(dp(12),dp(12),dp(12),dp(12));
  int width=PlayerOverlayRules.panelWidthPx(a.getResources().getDisplayMetrics().widthPixels,a.getResources().getDisplayMetrics().density);
  tabs=new HorizontalScrollView(a);tabs.setHorizontalScrollBarEnabled(false);sheet.addView(tabs,new LinearLayout.LayoutParams(-1,dp(48)));
  body=new TextView(a);ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setTag("mp-panel-box");box.setPadding(0,dp(8),0,0);scroll.addView(box);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  scrim.addView(sheet,new FrameLayout.LayoutParams(-1,-1));dialog.setContentView(scrim);
  Window w=dialog.getWindow();if(w!=null){Glass.window(a,w,GlassRules.RADIUS_DP);View decor=a.getWindow().getDecorView();int screenH=decor.getHeight()>0?decor.getHeight():a.getResources().getDisplayMetrics().heightPixels;int[] size=GlassRules.sideWindow(screenH,width,dp(GlassRules.EDGE_DP));w.setLayout(size[0],size[1]);WindowManager.LayoutParams lp=w.getAttributes();lp.gravity=Gravity.END|Gravity.CENTER_VERTICAL;lp.x=dp(GlassRules.EDGE_DP);lp.y=0;w.setAttributes(lp);w.setWindowAnimations(0);}
  dialog.setOnKeyListener((d,code,e)->{if(code==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)dismiss();return true;}return false;});
 }
 LinearLayout box(){return sheet==null?null:(LinearLayout)((ScrollView)sheet.getChildAt(1)).getChildAt(0);}
 void fill(){if(!EPISODES.equals(tab))EpisodeSources.close(a);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);ArrayList<String> idList=new ArrayList<>(),nameList=new ArrayList<>();boolean emby=TrackSelection.stream(a)!=null&&!TrackSelection.stream(a).optString("emby.item").isEmpty();
  idList.add(SUB);nameList.add("字幕");idList.add(AUDIO);nameList.add("音轨");idList.add(EPISODES);nameList.add(PlaybackReviewRules.listTitle(PlaybackReviewRules.series(PlayerOverlay.meta(a),PlayerOverlay.type(a))));idList.add(ROUTES);nameList.add("线路");
  if(EmbyPlayerUi.hasChapters(a)){idList.add(CHAPTERS);nameList.add("章节");}
  // 0.1.97：码率并入线路页；定时关闭、流信息是播放器级功能，移到「更多」，面板只管这一部片子的选择
  if(!idList.contains(tab))tab=SUB;
  String[] ids=idList.toArray(new String[0]);String[] names=nameList.toArray(new String[0]);
  for(int i=0;i<ids.length;i++){final String id=ids[i];boolean on=id.equals(tab);Button chip=new Button(a);chip.setAllCaps(false);chip.setText(names[i]);chip.setTextColor(PlayerTheme.chipText(on));chip.setTextSize(13);chip.setTypeface(android.graphics.Typeface.create(on?"sans-serif-medium":"sans-serif",android.graphics.Typeface.NORMAL));chip.setLetterSpacing(0.04f);chip.setGravity(Gravity.CENTER);chip.setMinWidth(0);chip.setMinimumWidth(0);chip.setMinHeight(dp(48));chip.setMinimumHeight(dp(48));chip.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(PlayerTheme.chipFill(on,CinemaTheme.gold(a)));chip.setBackground(g);chip.setOnClickListener(v->{tab=id;if(EPISODES.equals(id))segmentPick=-1;fill();});row.addView(chip,new LinearLayout.LayoutParams(-2,dp(48)));}
  tabs.removeAllViews();tabs.addView(row);
  LinearLayout box=box();if(box==null)return;box.removeAllViews();
  if(!SUB.equals(tab)){LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(fill(0x14ffffff,16));card.setPadding(dp(12),dp(6),dp(12),dp(8));box.addView(card,new LinearLayout.LayoutParams(-1,-2));box=card;}/* 0.1.169：其它页签也放进同样的玻璃卡片 */
  if(SUB.equals(tab))subtitles(box);else if(AUDIO.equals(tab))audio(box);else if(EPISODES.equals(tab))episodes(box);else if(CHAPTERS.equals(tab))chapters(box);else if(BITRATE.equals(tab))bitrate(box);else if(ROUTES.equals(tab))routes(box);else if(SLEEP.equals(tab))sleep(box);else info(box);
 }
 void note(LinearLayout box,String text){TextView t=new TextView(a);t.setText(text);t.setTextColor(PlayerTheme.note());t.setTextSize(13);t.setPadding(0,dp(8),0,dp(8));box.addView(t);}
 void row(LinearLayout box,String label,boolean selected,Runnable action){radio(box,label,null,selected,action);}
 /**
  * 0.1.169 字幕页改成两张可折叠的卡片（参照用户给的样式）：
  *  「轨道」——关闭、片子里的字幕轨、片源外挂直接列出来点选；下面是添加 / 搜索字幕和第二字幕。
  *  「设置」——同步加减、字号和位置滑条、样式和颜色、粗体开关，拖完马上作用到画面；VLC 自己画的内嵌字幕在关掉面板时重开一次片源套用。
  */
 static boolean tracksOpen=true,styleOpen=true;String styleBefore;
 void subtitles(LinearLayout box){
  android.content.SharedPreferences p=a.getSharedPreferences("data",0);if(styleBefore==null)styleBefore=SubtitleVlcRules.signature(SubtitleStyle.values(p));
  LinearLayout tracks=card(box,"轨道",tracksOpen,open->tracksOpen=open);
  boolean off=p.getBoolean("subtitleDisabled:"+SubtitleMemory.id(a),false);
  radio(tracks,"关闭",null,off,()->{SubtitleMemory.off(a);TrackSelection.quietSpu(a);fill();});
  EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);JSONArray subs=ui==null?new JSONArray():ui.subs;
  if(subs.length()>0){for(int i=0;i<subs.length();i++){JSONObject t=subs.optJSONObject(i);if(t==null)continue;int index=t.optInt("index",-1);String lang=t.optString("lang");radio(tracks,ui.describe(t,false),t.optBoolean("external")?"外挂":t.optString("codec").toUpperCase(Locale.ROOT),!off&&lang.equals(p.getString("emby.subLang","")),()->{ui.chooseSub(direct(),index,lang);fill();});}}
  else{Object player=vlcPlayer();int current=player==null?Integer.MIN_VALUE:TrackSelection.currentSpu(player);Object raw=null;try{raw=player==null?null:OnlineSubtitleSearch.call(player,"getSpuTracks",new Class<?>[0]);}catch(Exception ignored){}
   int[] ids=raw instanceof Object[]?new int[((Object[])raw).length]:new int[0];String[] names=raw instanceof Object[]?TrackSelection.trackNames(raw,ids):new String[0];boolean any=false;
   for(int i=0;i<ids.length;i++){if(ids[i]<0)continue;any=true;final int id=ids[i];final String name=names==null||i>=names.length||names[i]==null?"字幕 "+(i+1):names[i];radio(tracks,name,"内嵌",!off&&id==current&&!TrackSelection.cuesVisible(a),()->{try{SourceSubtitleLoader.stopAuto(a);SourceSubtitleLoader.set(a,"cues",new ArrayList<>());}catch(Exception ignored){}SubtitleMemory.embedded(a,id,name);try{TrackSelection.spu(vlcPlayer(),id);}catch(Exception ignored){}fill();});}
   JSONArray ext=TrackSelection.stream(a).optJSONArray("subtitles");
   for(int i=0;ext!=null&&i<ext.length();i++){JSONObject sx=ext.optJSONObject(i);if(sx==null||sx.optString("url").isEmpty())continue;any=true;final JSONObject item=sx;String name=sx.optString("lang",sx.optString("name","外挂字幕"));radio(tracks,name,"外挂",false,()->{TrackSelection.quietSpu(a);try{SourceSubtitleLoader.load(a,new JSONArray().put(item),false);}catch(Exception ignored){}fill();});}
   if(TrackSelection.cuesVisible(a))radio(tracks,"已载入的字幕","外挂 / 搜索",!off,()->{});
   if(!any&&player==null)radio(tracks,"播放器里的字幕",null,false,()->{EmbyPlayerUi.fallback(a,"subtitles");dismiss();});
   else if(!any&&!TrackSelection.cuesVisible(a))hint(tracks,"这条片源没有字幕轨，可以搜索或添加。");}
  action(tracks,"＋  添加字幕",()->{List<CinemaMenu.Item> rows=new ArrayList<>();rows.add(CinemaMenu.item("用已安装的字幕插件搜索",()->{Object session=OnlineSubtitleSearch.field(a,"session");if(session!=null)OnlineSubtitleSearch.start(session);}));rows.add(CinemaMenu.item("Whisper 语音生成字幕",()->PlayerFeatureSettings.whisper(a)));CinemaMenu.show(a,"添加字幕",rows);});
  action(tracks,"⌕  搜索字幕",()->{dismiss();OnlineSubtitleController.open(a);});
  String second=p.getString("subtitle2Url:"+SubtitleMemory.id(a),"");JSONArray ext2=TrackSelection.stream(a).optJSONArray("subtitles");List<CinemaMenu.Item> seconds=new ArrayList<>();seconds.add(CinemaMenu.choice("不使用第二字幕",second.isEmpty(),()->{SecondarySubtitle.clear(a);fill();}));
  for(int i=0;ext2!=null&&i<ext2.length();i++){JSONObject sx=ext2.optJSONObject(i);if(sx==null||!SubtitleAuto.secondaryUrl(sx.optString("url")))continue;final JSONObject item=sx;seconds.add(CinemaMenu.choice("外挂 · "+sx.optString("lang",sx.optString("name","外挂字幕")),sx.optString("url").equals(second),()->{SecondarySubtitle.load(a,item,true);fill();}));}
  action(tracks,"第二字幕："+(second.isEmpty()?"不使用":"已开启")+(seconds.size()>1?"  ▾":""),()->{if(seconds.size()<=1){OnlineSubtitleSearch.message(a,"第二字幕只能用片源附带的外挂文本，这条片源没有。");return;}CinemaMenu.show(a,"第二字幕",seconds);});
  LinearLayout style=card(box,"设置",styleOpen,open->styleOpen=open);SubtitlePrefs.Values v=SubtitleStyle.values(p);
  int offset=p.getInt("sub.offsetMs",0);LinearLayout sync=line(style,"字幕同步");
  sync.addView(key("−",()->offsetBy(-1)));TextView value=label((offset>0?"+":"")+String.format(Locale.ROOT,"%.1fs",offset/1000f),14,0xffffffff);value.setGravity(Gravity.CENTER);sync.addView(value,new LinearLayout.LayoutParams(dp(64),dp(40)));sync.addView(key("+",()->offsetBy(1)));sync.addView(key("↺",()->{p.edit().putInt("sub.offsetMs",0).apply();applyOffset(0);SubtitleSettings.nudge(a);fill();}));
  slider(style,"字幕大小",SubtitlePrefs.SIZES,SubtitlePrefs.SIZE_LABELS,v.size,x->p.edit().putString("sub.size",x).apply());
  slider(style,"字幕位置",SubtitlePrefs.LIFTS,SubtitlePrefs.LIFT_LABELS,v.lift,x->p.edit().putString("sub.lift",x).apply());
  segments(style,"样式",SubtitlePrefs.LOOKS,SubtitlePrefs.LOOK_LABELS,v.look,x->p.edit().putString("sub.look",x).apply());
  colors(style,v.color,x->p.edit().putString("sub.color",x).apply());
  toggle(style,"粗体",v.bold,on->p.edit().putBoolean("sub.bold",on).apply());
  action(style,"更多字幕设置 ›",()->{dismiss();SubtitleSettings.open(a);});
  hint(style,"ASS 字幕按文件自带样式显示；PGS 是图片字幕，样式改不了。");
 }
 Object vlcPlayer(){Object video=OnlineSubtitleSearch.field(a,"video");return video==null?null:OnlineSubtitleSearch.field(video,"player");}
 interface Pick {void save(String value);} interface Flip {void set(boolean on);}
 GradientDrawable fill(int color,int radiusDp){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radiusDp));return g;}
 TextView label(String text,int sp,int color){TextView t=new TextView(a);t.setText(text);t.setTextSize(sp);t.setTextColor(color);t.setIncludeFontPadding(false);return t;}
 /** 可折叠卡片：标题行点一下收起 / 展开。 */
 LinearLayout card(LinearLayout box,String title,boolean open,Flip remember){LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(fill(0x14ffffff,16));card.setPadding(dp(12),dp(4),dp(12),dp(8));
  LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);head.setMinimumHeight(dp(48));TextView name=label(title,16,0xffffffff);name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);head.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=label(open?"⌃":"⌄",18,0xccffffff);head.addView(arrow);card.addView(head);
  LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setVisibility(open?View.VISIBLE:View.GONE);card.addView(content);
  head.setOnClickListener(x->{boolean now=content.getVisibility()!=View.VISIBLE;content.setVisibility(now?View.VISIBLE:View.GONE);arrow.setText(now?"⌃":"⌄");remember.set(now);});CinemaControls.group(head,title);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);box.addView(card,lp);return content;}
 void radio(LinearLayout box,String title,String detail,boolean on,Runnable act){int accent=PlayerTheme.playbackAccent(CinemaTheme.gold(a));LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(48));row.setPadding(dp(4),dp(6),dp(4),dp(6));
  TextView dot=label(on?"◉":"○",16,on?accent:0x99ffffff);dot.setPadding(0,0,dp(12),0);row.addView(dot);LinearLayout texts=new LinearLayout(a);texts.setOrientation(LinearLayout.VERTICAL);TextView t=label(title,15,on?accent:0xffffffff);t.setMaxLines(2);texts.addView(t);if(detail!=null&&!detail.isEmpty()){TextView d=label(detail,12,0x99ffffff);d.setPadding(0,dp(3),0,0);texts.addView(d);}row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
  Glass.row(a,row,null,accent);row.setOnClickListener(x->act.run());CinemaControls.group(row,title+(on?"，已选":""));box.addView(row,new LinearLayout.LayoutParams(-1,-2));}
 void action(LinearLayout box,String text,Runnable act){TextView t=label(text,15,0xffffffff);t.setGravity(Gravity.CENTER_VERTICAL);t.setMinHeight(dp(48));t.setPadding(dp(4),0,dp(4),0);Glass.row(a,t,null,0);t.setOnClickListener(x->act.run());CinemaControls.action(t,text);box.addView(t,new LinearLayout.LayoutParams(-1,dp(48)));}
 void hint(LinearLayout box,String text){TextView t=label(text,12,0x99ffffff);t.setPadding(dp(4),dp(6),dp(4),dp(6));box.addView(t);}
 LinearLayout line(LinearLayout box,String title){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(48));TextView t=label(title,14,0xddffffff);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));box.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;}
 TextView key(String text,Runnable act){TextView t=label(text,18,0xffffffff);t.setGravity(Gravity.CENTER);t.setBackground(fill(0x1fffffff,10));t.setOnClickListener(x->act.run());CinemaControls.action(t,text);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(40),dp(40));lp.leftMargin=dp(6);t.setLayoutParams(lp);return t;}
 /** 分档滑条：拖动时只改右边的数值，松手才保存并刷新画面上的字。 */
 void slider(LinearLayout box,String title,String[] values,String[] labels,String current,Pick save){int at=SubtitlePrefs.index(values,current,0);LinearLayout head=line(box,title);TextView shown=label(labels[at],14,0xffffffff);head.addView(shown);
  SeekBar bar=new SeekBar(a);bar.setMax(values.length-1);bar.setProgress(at);int accent=PlayerTheme.playbackAccent(CinemaTheme.gold(a));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));bar.setThumbTintList(android.content.res.ColorStateList.valueOf(0xffffffff));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0x55ffffff));
  bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){shown.setText(labels[v]);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){save.save(values[s.getProgress()]);SubtitleSettings.nudge(a);}});
  bar.setContentDescription(title);box.addView(bar,new LinearLayout.LayoutParams(-1,dp(40)));}
 void segments(LinearLayout box,String title,String[] values,String[] labels,String current,Pick save){line(box,title);LinearLayout row=new LinearLayout(a);row.setBackground(fill(0x14ffffff,12));row.setPadding(dp(3),dp(3),dp(3),dp(3));int accent=PlayerTheme.playbackAccent(CinemaTheme.gold(a));
  for(int i=0;i<values.length;i++){final String value=values[i];boolean on=value.equals(current);TextView t=label(labels[i],14,on?Ui.onFill(accent):0xffffffff);t.setGravity(Gravity.CENTER);if(on)t.setBackground(fill(accent,10));t.setOnClickListener(x->{save.save(value);SubtitleSettings.nudge(a);fill();});CinemaControls.action(t,title+" "+labels[i]);t.setSelected(on);row.addView(t,new LinearLayout.LayoutParams(0,dp(40),1));}
  box.addView(row,new LinearLayout.LayoutParams(-1,-2));}
 void colors(LinearLayout box,String current,Pick save){line(box,"颜色");LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  for(int i=0;i<SubtitlePrefs.COLORS.length;i++){final String value=SubtitlePrefs.COLORS[i];boolean on=value.equals(current);View dot=new View(a);GradientDrawable g=new GradientDrawable();g.setShape(GradientDrawable.OVAL);g.setColor(SubtitlePrefs.COLOR_ARGB[i]);g.setStroke(dp(on?3:1),on?PlayerTheme.playbackAccent(CinemaTheme.gold(a)):0x55ffffff);dot.setBackground(g);dot.setOnClickListener(x->{save.save(value);SubtitleSettings.nudge(a);fill();});CinemaControls.action(dot,"颜色 "+SubtitlePrefs.COLOR_LABELS[i]);dot.setSelected(on);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(36),dp(36));lp.rightMargin=dp(14);row.addView(dot,lp);}
  box.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));}
 void toggle(LinearLayout box,String title,boolean on,Flip save){LinearLayout row=line(box,title);Switch sw=new Switch(a);sw.setChecked(on);int accent=PlayerTheme.playbackAccent(CinemaTheme.gold(a));int[][] st={new int[]{android.R.attr.state_checked},new int[]{}};sw.setThumbTintList(new android.content.res.ColorStateList(st,new int[]{0xffffffff,0xffcccccc}));sw.setTrackTintList(new android.content.res.ColorStateList(st,new int[]{accent,0x55ffffff}));sw.setOnCheckedChangeListener((b,v)->{save.set(v);SubtitleSettings.nudge(a);});sw.setContentDescription(title);row.addView(sw);}
 void offsetBy(int direction){int next=EmbyCue.stepSubtitleOffset(a.getSharedPreferences("data",0).getInt("sub.offsetMs",0),direction);a.getSharedPreferences("data",0).edit().putInt("sub.offsetMs",next).apply();applyOffset(next);fill();}
 void applyOffset(int ms){try{Object video=OnlineSubtitleSearch.field(a,"video");Object player=video==null?null:OnlineSubtitleSearch.field(video,"player");if(player!=null)OnlineSubtitleSearch.call(player,"setSpuDelay",new Class<?>[]{long.class},ms*1000L);}catch(Exception ignored){}}
 boolean direct(){JSONObject stream=TrackSelection.stream(a);return stream==null||stream.optBoolean("emby.direct",true);}
 TextView chip(String text,Runnable action){TextView t=new TextView(a);t.setText(text);t.setTextColor(0xffffffff);t.setTextSize(14);t.setPadding(dp(12),dp(8),dp(12),dp(8));t.setOnClickListener(v->action.run());return t;}
 void audio(LinearLayout box){EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);JSONArray audio=ui==null?new JSONArray():ui.audio;if(audio.length()==0){row(box,"播放器里的音轨",false,()->{EmbyPlayerUi.fallback(a,"audio");dismiss();});return;}for(int i=0;i<audio.length();i++){JSONObject t=audio.optJSONObject(i);if(t==null)continue;int index=t.optInt("index",-1),ordinal=t.optInt("ordinal",i);String lang=t.optString("language"),codec=t.optString("codec");row(box,ui.describe(t,true),false,()->{ui.chooseAudio(direct(),index,ordinal,lang,codec);dismiss();});}}
 Button seasonChip(String text,boolean on,Runnable action){Button b=new Button(a);b.setAllCaps(false);b.setText(text);b.setTextSize(13);b.setTextColor(PlayerTheme.chipText(on));b.setTypeface(android.graphics.Typeface.create(on?"sans-serif-medium":"sans-serif",android.graphics.Typeface.NORMAL));b.setLetterSpacing(0.04f);b.setGravity(Gravity.CENTER);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(PlayerTheme.chipFill(on,CinemaTheme.gold(a)));b.setBackground(g);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(48));lp.rightMargin=dp(6);b.setLayoutParams(lp);return b;}
 void episodes(LinearLayout box){
  JSONObject meta=PlayerOverlay.meta(a);ArrayList<JSONObject> list=EpisodeActions.list(meta);
  boolean episodic=PlaybackReviewRules.series(meta,PlayerOverlay.type(a));
  if(!episodic){note(box,PlaybackReviewRules.count(false,list.size()));String cur=PlayerOverlay.id(a);for(JSONObject video:list){String id=video.optString("id");row(box,PlaybackReviewRules.version(video),cur.equals(id),()->{dismiss();if(id.isEmpty()||id.equals(cur))return;String value=video.optString("fusion.value");if(!value.isEmpty())PlaybackSitePicker.playRoute(a,video.optString("fusion.flag"),video.optString("fusion.name",video.optString("title")),value);else AutoNext.handoff(a,meta,id,PlayerOverlay.type(a));});}if(list.isEmpty()&&EmbyPlayerUi.hasVersions(a))row(box,"选择服务器版本",false,()->{dismiss();EmbyPlayerUi.openVersions(a);});return;}
  if(list.isEmpty()){note(box,"没有可选择的剧集");if(meta.has("fusion.config"))row(box,"打开片源选集",false,()->{dismiss();PlaybackSitePicker.selectEpisode(a);});return;}
  String cur=PlayerOverlay.id(a);ArrayList<Integer> seasons=EpisodeActions.seasons(list);int at=EpisodeActions.indexOf(list,cur);
  if(seasons.size()<=1)seasonPick=-1;else{if(seasonPick<-1)seasonPick=at>=0&&list.get(at).has("season")?list.get(at).optInt("season"):-1;if(at>=0&&seasonPick>=0&&!EpisodeActions.inSeason(list.get(at),seasonPick))seasonPick=list.get(at).has("season")?list.get(at).optInt("season"):-1;if(seasonPick>=0&&!seasons.contains(seasonPick))seasonPick=-1;}
  if(seasons.size()>1){HorizontalScrollView chips=new HorizontalScrollView(a);chips.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(a);line.setOrientation(LinearLayout.HORIZONTAL);line.addView(seasonChip("全部",seasonPick<0,()->{seasonPick=-1;segmentPick=-1;fill();}));for(int s:seasons){final int season=s;line.addView(seasonChip("第 "+season+" 季",seasonPick==season,()->{seasonPick=season;segmentPick=-1;fill();}));}chips.addView(line);box.addView(chips,new LinearLayout.LayoutParams(-1,dp(40)));}
  ArrayList<JSONObject> shown=new ArrayList<>();for(JSONObject video:list)if(seasonPick<0||EpisodeActions.inSeason(video,seasonPick))shown.add(video);
  ArrayList<int[]> ranges=EpisodeActions.segments(shown.size());int here=-1;for(int i=0;i<shown.size();i++)if(cur.equals(shown.get(i).optString("id")))here=i;
  if(!ranges.isEmpty()){if(segmentPick<0||segmentPick>=ranges.size())segmentPick=here<0?0:EpisodeActions.segmentAt(shown.size(),here);HorizontalScrollView chips=new HorizontalScrollView(a);chips.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(a);line.setOrientation(LinearLayout.HORIZONTAL);for(int i=0;i<ranges.size();i++){final int pick=i;int[] range=ranges.get(i);Button chip=seasonChip(EpisodeActions.segmentLabel(range[0],range[1]),segmentPick==i,()->{segmentPick=pick;fill();});if(segmentPick==i)chip.setTag("mp-current-segment");line.addView(chip);}chips.addView(line);box.addView(chips,new LinearLayout.LayoutParams(-1,dp(40)));chips.post(()->{View target=line.findViewWithTag("mp-current-segment");if(target!=null)chips.scrollTo(Math.max(0,target.getLeft()-dp(12)),0);});}
  int from=0,to=shown.size();if(!ranges.isEmpty()){int[] range=ranges.get(Math.max(0,Math.min(segmentPick,ranges.size()-1)));from=range[0];to=range[1];}
  note(box,"共 "+shown.size()+" 集");
  for(int i=from;i<to;i++){JSONObject video=shown.get(i);String id=video.optString("id");boolean on=cur.equals(id);final View[] self={null};final String name=EpisodeActions.label(video);row(box,name,on,()->{if(id.isEmpty()||id.equals(cur)){dismiss();return;}String value=video.optString("fusion.value");if(!value.isEmpty()){dismiss();PlaybackSitePicker.playRoute(a,video.optString("fusion.flag"),video.optString("fusion.name",video.optString("title")),value);return;}
    // 0.1.173：Stremio 剧集在左边直接列出这一集的片源，选好就播；Emby 原地切换；没有能查片源的插件时才退回详情页流程
    if(!EmbyApi.isEmbyId(id)&&EpisodeSources.show(a,meta,id,PlayerOverlay.type(a),name)){for(int k=0;k<box.getChildCount();k++)box.getChildAt(k).setActivated(false);if(self[0]!=null)self[0].setActivated(true);return;}
    dismiss();AutoNext.handoff(a,meta,id,PlayerOverlay.type(a));});self[0]=box.getChildAt(box.getChildCount()-1);if(on)self[0].setTag("mp-current-episode");}
  View scroller=sheet==null?null:sheet.getChildAt(1);if(scroller instanceof ScrollView){ScrollView scroll=(ScrollView)scroller;scroll.post(()->{View target=box.findViewWithTag("mp-current-episode");if(target==null)return;int y=target.getTop()-Math.max(0,(scroll.getHeight()-target.getHeight())/2);scroll.scrollTo(0,Math.max(0,y));});}
 }
 void chapters(LinearLayout box){EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);if(ui==null||ui.chapters.length()==0){note(box,"没有章节");return;}for(int i=0;i<ui.chapters.length();i++){JSONObject c=ui.chapters.optJSONObject(i);if(c==null)continue;long at=c.optLong("startMs");row(box,c.optString("name"),false,()->{EmbyPlayback.seek(a,(int)Math.min(Integer.MAX_VALUE,at));dismiss();});}}
 void bitrate(LinearLayout box){JSONObject stream=TrackSelection.stream(a);if(stream==null||stream.optString("emby.item").isEmpty()){note(box,"只有 Emby 片源可以按码率重新请求。");return;}note(box,"选择后会按这个 MaxStreamingBitrate 重新向服务器要播放地址。");long current=a.getSharedPreferences("data",0).getLong("emby.bitrate",140_000_000L);for(long rate:RATES){String label=rate>=1_000_000?(rate/1_000_000)+" Mbps":(rate/1000)+" Kbps";row(box,label,rate==current,()->{a.getSharedPreferences("data",0).edit().putLong("emby.bitrate",rate).apply();dismiss();EmbyPlayback.replayBitrate(a,rate);});}}
 void routes(LinearLayout box){
  // 0.1.93：换片源 / 换版本 / 换线路统一放在这一页（原来分散在「更多」里）
  boolean any=false;JSONObject meta=PlayerOverlay.meta(a);
  if(meta.has("fusion.config")){row(box,"切换片源（影视仓其他站点 / 线路）",false,()->{dismiss();PlaybackSitePicker.show(a);});any=true;}
  if(EmbyPlayerUi.hasVersions(a)){row(box,"切换版本",false,()->{dismiss();EmbyPlayerUi.openVersions(a);});any=true;}
  // 原版「更多播放设置」的三项直接做在这里，不再弹原版白底对话框
  if(OnlineSubtitleSearch.field(a,"queue")!=null){
   row(box,"换下一个源",false,()->{dismiss();PlayerOverlay.nextSource(a);});
   boolean auto=Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"autoSwitch"));
   row(box,"自动换源："+(auto?"开":"关"),auto,()->{try{boolean on=!Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"autoSwitch"));SourceSubtitleLoader.set(a,"autoSwitch",on);a.getSharedPreferences("data",0).edit().putBoolean("autoSwitch",on).apply();OnlineSubtitleSearch.call(a,"autoLabel",new Class<?>[0]);PlayerOverlay.toast(a,on?"自动换源已开启":"自动换源已关闭");}catch(Exception ignored){}fill();});
   int wait=a.getSharedPreferences("data",0).getInt("sourceTimeout",30);
   row(box,"每个源最多等待："+wait+" 秒",false,()->waitMenu(a));
   any=true;}
  embyRoutes(box,any);
  if(TrackSelection.stream(a)!=null&&!TrackSelection.stream(a).optString("emby.item").isEmpty()){note(box,"码率");bitrate(box);}
 }
 void embyRoutes(LinearLayout box,boolean any){JSONObject stream=TrackSelection.stream(a);JSONObject server=stream==null?null:EmbyStore.get(a,stream.optString("emby.server"));if(server==null){if(!any)note(box,"当前片源没有可切换的线路。");return;}note(box,"Emby 服务器线路");String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));if(bases.length==0)note(box,"没有备用线路。");for(String base:bases){row(box,base,base.equalsIgnoreCase(server.optString("url")),()->{try{JSONObject copy=new JSONObject(server.toString());copy.put("url",base);dismiss();EmbyPlayback.playItem(a,copy,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}catch(Exception ignored){}});}note(box,"服务器插件提供的额外线路会在打开面板时尝试读取。");EmbyPlayback.IO.execute(()->{try{JSONObject extra=EmbyClient.json(a,server,"GET","/System/Ext/ServerDomains",null);JSONArray items=extra.optJSONArray("Items");if(items==null)items=extra.optJSONArray("Domains");if(items==null||items.length()==0)return;JSONArray found=items;a.runOnUiThread(()->{if(a.isFinishing()||!ROUTES.equals(tab))return;for(int i=0;i<found.length();i++){String domain=found.optString(i);if(domain.isEmpty()&&found.optJSONObject(i)!=null)domain=found.optJSONObject(i).optString("Url",found.optJSONObject(i).optString("Domain"));if(domain.isEmpty())continue;final String url=domain;LinearLayout host=box();if(host==null)return;row(host,url,false,()->{try{JSONObject copy=new JSONObject(server.toString());copy.put("url",url);dismiss();EmbyPlayback.playItem(a,copy,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}catch(Exception ignored){}});}});}catch(Exception ignored){}});}
 void sleep(LinearLayout box){note(box,sleepDeadline>System.currentTimeMillis()?"将在 "+Math.max(1,(sleepDeadline-System.currentTimeMillis()+59999)/60000)+" 分钟后暂停并关闭播放页。":"到点后暂停并关闭播放页。");int[] mins={0,15,30,45,60,90};for(int m:mins){row(box,m==0?"关闭定时":m+" 分钟",false,()->arm(m));}}
 void arm(int minutes){TIMER.removeCallbacks(sleepTask);sleepDeadline=0;if(minutes<=0){if(sheet!=null)fill();PlayerOverlay.toast(a,"已关闭定时");return;}sleepDeadline=System.currentTimeMillis()+minutes*60000L;sleepTask=()->{sleepDeadline=0;try{Object video=OnlineSubtitleSearch.field(a,"video");if(video!=null)OnlineSubtitleSearch.call(video,"pause",new Class<?>[0]);}catch(Exception ignored){}if(!a.isFinishing()){PlayerOverlay.toast(a,"定时关闭");a.finish();}};TIMER.postDelayed(sleepTask,minutes*60000L);dismiss();PlayerOverlay.toast(a,minutes+" 分钟后关闭");}
 /** 「更多」→ 定时关闭。 */
 /** 「更多」里的定时关闭行：开着时显示还剩几分钟。 */
 static String sleepLabel(){long left=sleepDeadline-System.currentTimeMillis();return left>0?"定时关闭：还剩 "+Math.max(1,(left+59999)/60000)+" 分钟":"定时关闭";}
 static void sleepMenu(Activity a){PlayerSidePanel p=ALL.get(a);if(p==null){p=new PlayerSidePanel(a);ALL.put(a,p);}final PlayerSidePanel panel=p;boolean armed=sleepDeadline>System.currentTimeMillis();List<CinemaMenu.Item> rows=new ArrayList<>();int[] mins={0,15,30,45,60,90};for(int m:mins){final int at=m;rows.add(CinemaMenu.choice(m==0?"不定时":m+" 分钟",m==0&&!armed,()->panel.arm(at)));}CinemaMenu.show(a,armed?"定时关闭 · "+Math.max(1,(sleepDeadline-System.currentTimeMillis()+59999)/60000)+" 分钟后":"定时关闭",rows);}
 /** 每个源最多等待多久再换下一个（原版 sourceTimeout）。 */
 static void waitMenu(Activity a){int cur=a.getSharedPreferences("data",0).getInt("sourceTimeout",30);int[] secs={30,45,60,120};String[] labels={"30 秒（P2P 至少 60 秒）","45 秒（P2P 至少 60 秒）","60 秒","120 秒"};List<CinemaMenu.Item> rows=new ArrayList<>();for(int i=0;i<secs.length;i++){final int v=secs[i];rows.add(CinemaMenu.choice(labels[i],cur==v,()->{a.getSharedPreferences("data",0).edit().putInt("sourceTimeout",v).apply();try{SourceSubtitleLoader.set(a,"openedAt",android.os.SystemClock.elapsedRealtime());OnlineSubtitleSearch.call(a,"autoLabel",new Class<?>[0]);}catch(Exception ignored){}PlayerSidePanel panel=ALL.get(a);if(panel!=null&&open(a)&&ROUTES.equals(panel.tab))panel.fill();}));}CinemaMenu.show(a,"每个源最多等待",rows);}
 /** 「更多」→ 流信息：播放状态、连接详情、地址、分辨率等，可复制。 */
 static void infoDialog(Activity a){String text=infoText(a);new CinemaDialog(a).setTitle("流信息").setMessage(text).setPositiveButton("复制",(d,w)->{try{((android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("流信息",text));PlayerOverlay.toast(a,"已复制");}catch(Exception ignored){}}).setNegativeButton("关闭",null).show();}
 static String infoText(Activity a){StringBuilder s=new StringBuilder();
  try{Object prefix=OnlineSubtitleSearch.call(a,"prefix",new Class<?>[0]);if(prefix!=null)s.append("播放源  ").append(prefix).append('\n');}catch(Exception ignored){}
  Object conn=OnlineSubtitleSearch.field(a,"connectionMessage");if(conn!=null&&!conn.toString().isEmpty())s.append("连接  ").append(conn).append('\n');
  try{Object v=OnlineSubtitleSearch.field(a,"video");Object diag=v==null?null:OnlineSubtitleSearch.call(v,"diagnostics",new Class<?>[0]);if(diag!=null&&!diag.toString().trim().isEmpty())s.append("解码  ").append(diag.toString().trim()).append('\n');}catch(Exception ignored){}
  PlayerSidePanel p=new PlayerSidePanel(a);s.append(p.infoBody());return s.toString().trim();}
 String infoBody(){StringBuilder s=new StringBuilder();Object diagnostic=OnlineSubtitleSearch.field(a,"state");if(diagnostic instanceof TextView)s.append("播放状态  ").append(((TextView)diagnostic).getText()).append("\n");JSONObject stream=TrackSelection.stream(a);Object video=OnlineSubtitleSearch.field(a,"video");String name=PlayerOverlay.name(a);if(!name.isEmpty())s.append("名称  ").append(name).append('\n');if(stream!=null){s.append("方式  ").append(stream.optBoolean("emby.direct",true)?"直接串流":"转码").append('\n');if(!stream.optString("emby.mediaSource").isEmpty())s.append("媒体源  ").append(stream.optString("emby.mediaSource")).append('\n');String url=stream.optString("url");int q=url.indexOf('?');if(q>=0)url=url.substring(0,q);if(!url.isEmpty())s.append("地址  ").append(url).append('\n');}if(video!=null){Object w=OnlineSubtitleSearch.field(video,"vw"),h=OnlineSubtitleSearch.field(video,"vh");if(w instanceof Integer&&h instanceof Integer&&(Integer)w>0)s.append("分辨率  ").append(w).append("×").append(h).append('\n');}int offset=a.getSharedPreferences("data",0).getInt("sub.offsetMs",0);s.append("字幕偏移  ").append(offset).append(" ms\n");s.append("倍速  ").append(PlayerOverlayRules.speedPill(PlaybackPrefs.speed(a.getSharedPreferences("data",0)))).append('\n');if(s.length()==0)s.append("还没有流信息");return s.toString();}
 void info(LinearLayout box){note(box,infoText(a));}
 void dismiss(){EpisodeSources.close(a);if(styleBefore!=null){String before=styleBefore;styleBefore=null;SubtitleSettings.restartVlc(a,before);}if(back!=null){back.dispose();back=null;}final android.app.Dialog d=dialog;dialog=null;if(d!=null&&sheet!=null&&d.isShowing()){sheet.animate().alpha(0f).translationX(dp(24)).setDuration(PlayerMotionRules.PANEL_OUT_MS).withEndAction(()->{try{d.dismiss();}catch(RuntimeException ignored){}}).start();}else if(d!=null)try{d.dismiss();}catch(RuntimeException ignored){}scrim=null;sheet=null;}
}
