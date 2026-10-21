package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.content.res.ColorStateList;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.view.*;import android.widget.*;import org.json.*;
/** Hills/Emby 风格首页：顶栏片源下拉、续播进度条、媒体库瓦片、海报角标；底栏由 ScrapedMainActivity 组装。 */
final class HomeChrome {
 final ScrapedMainActivity a;
 HomeChrome(ScrapedMainActivity a){this.a=a;}
 int dp(int v){return a.dp(v);}
 GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 /** 顶栏左侧片源下拉（替代「随影」品牌）。 */
 TextView sourceSwitcher(){
  String provider=a.prefs.getString("fusion.homeProvider","stremio"),sourceId=a.prefs.getString("home.source.v1","current");
  JSONArray servers;try{servers=EmbyStore.list(a);}catch(Exception e){servers=new JSONArray();}
  if("emby".equals(provider)){JSONObject server=HomeRules.selectedEmbyServer(servers,sourceId);if(server!=null)sourceId=server.optString("id");}
  JSONArray options;try{options=HomeRules.sourceOptions(HomeSources.list(a),servers);}catch(Exception e){try{options=HomeRules.sourceOptions(new JSONArray(),servers);}catch(Exception x){options=new JSONArray();}}
  String id=HomeRules.selectedId(provider,sourceId);
  String label=HomeRules.labelFor(options,id);
  TextView t=a.text(label+"  ▾",16,0xffffffff);t.setTypeface(null,1);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setMaxWidth(dp(260));
  t.setCompoundDrawablesWithIntrinsicBounds(new NavIcon(9,dp(18),a.gold?CinemaTheme.GOLD:a.clay?CinemaTheme.CLAY:0xffa2d0ed),null,null,null);t.setCompoundDrawablePadding(dp(6));
  t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(10),dp(6),dp(12),dp(6));t.setContentDescription("首页内容来源："+label);
  // 0.1.97：压在海报上时看不清，加半透明深色胶囊底
  t.setBackground(round(0x66000000,18));
  final JSONArray opts=options;t.setOnClickListener(v->pickSource(opts,id,t));return t;
 }
 void pickSource(JSONArray options,String current,View anchor){
  if(options==null||options.length()==0)return;
  FrameLayout overlay=new FrameLayout(a);View dim=new View(a);dim.setBackgroundColor(0xff000000);dim.setAlpha(0);overlay.addView(dim,new FrameLayout.LayoutParams(-1,-1));
  AnimatedPopup popup=new AnimatedPopup(overlay);popup.setClippingEnabled(false);popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));popup.setOutsideTouchable(true);
  LinearLayout list=new LinearLayout(a);list.setOrientation(1);list.setPadding(dp(8),dp(8),dp(8),dp(8));
  ScrollView scroll=new ScrollView(a);scroll.addView(list);scroll.setBackground(round(a.light?0xfffafafa:0xf2302e3a,14));scroll.setElevation(dp(12));scroll.setClipToOutline(true);
  final boolean[] closing={false};
  final Runnable[] after={null};
  Runnable close=()->{if(closing[0])return;closing[0]=true;if(a.isFinishing()||a.isDestroyed()){popup.finish();if(after[0]!=null)after[0].run();return;}dim.animate().alpha(0).setDuration(HomeMotionRules.MENU_MS).start();scroll.animate().alpha(0).scaleX(HomeMotionRules.menuScale(0)).scaleY(HomeMotionRules.menuScale(0)).setDuration(HomeMotionRules.MENU_MS).setInterpolator(new android.view.animation.AccelerateInterpolator()).withEndAction(()->{popup.finish();if(after[0]!=null&&!a.isFinishing())after[0].run();}).start();};
  // 返回键、点外面、选完片源都经由 dismiss() → 先播放收起动画再真正关闭
  popup.closing=close;
  dim.setOnClickListener(v->close.run());
  {TextView head=a.text("首页内容来源",12,a.muted());head.setPadding(dp(10),dp(6),dp(10),dp(4));list.addView(head);}
  for(int i=0;i<options.length();i++){JSONObject item=options.optJSONObject(i);if(item==null)continue;final JSONObject choice=item;String hint=HomeRules.sourceHint(item.optString("id"),item.optString("type"));TextView row=a.text("",14,a.ink());{String first=(current.equals(item.optString("id"))?"◉  ":"○  ")+item.optString("label");android.text.SpannableStringBuilder sb=new android.text.SpannableStringBuilder(first+"\n      "+hint);sb.setSpan(new android.text.style.RelativeSizeSpan(0.8f),first.length(),sb.length(),0);sb.setSpan(new android.text.style.ForegroundColorSpan(a.muted()),first.length(),sb.length(),0);row.setText(sb);}row.setLineSpacing(dp(2),1f);row.setPadding(dp(10),dp(10),dp(10),dp(10));row.setOnClickListener(v->{String optionId=choice.optString("id");if(HomeRules.keepsHomeProvider(optionId)){after[0]=()->EmbyLibraryActivity.manage(a);close.run();return;}String[] applied=HomeRules.apply(optionId);if(applied==null){close.run();return;}a.prefs.edit().putString("fusion.homeProvider",applied[0]).putString("home.source.v1",applied[1]).apply();a.fusionSession=null;after[0]=()->a.home();close.run();});list.addView(row);}
  int[] at=new int[2];anchor.getLocationOnScreen(at);
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(250),Math.min(dp(380),dp(44+options.length()*62)),Gravity.TOP|Gravity.LEFT);lp.leftMargin=at[0];lp.topMargin=at[1]+anchor.getHeight()+dp(4);overlay.addView(scroll,lp);
  scroll.setPivotX(dp(24));scroll.setPivotY(0);scroll.setAlpha(0);scroll.setScaleX(HomeMotionRules.menuScale(0));scroll.setScaleY(HomeMotionRules.menuScale(0));
  popup.showAtLocation(anchor,Gravity.TOP|Gravity.LEFT,0,0);
  dim.animate().alpha(HomeMotionRules.dimAlpha(1)/255f).setDuration(HomeMotionRules.MENU_MS).start();
  scroll.animate().alpha(1).scaleX(1).scaleY(1).setDuration(HomeMotionRules.MENU_MS).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
 }
 /** 继续观看横滑行，带底部蓝色进度条与集数文案。 */
 /** dismiss() 改为先播放收起动画；finish() 才真正关闭。 */
 static final class AnimatedPopup extends PopupWindow {
  Runnable closing;boolean finished;
  AnimatedPopup(View content){super(content,-1,-1,true);}
  @Override public void dismiss(){if(closing==null||finished){finish();return;}closing.run();}
  void finish(){if(finished)return;finished=true;try{if(isShowing())super.dismiss();}catch(Exception ignored){}}
 }
 void continueWatching(JSONArray history){continueWatching(a.body,history,"继续观看");}
 void continueWatching(LinearLayout parent,JSONArray history,String heading){
  if(history==null||parent==null)return;
  if(!HomeRules.sectionShown(a.prefs.getString("home.hiddenSections",""),"resume"))return;
  history=HomeRules.withoutHidden(history,a.prefs.getString("home.hiddenResume","[]"));
  if(history.length()==0)return;
  TextView head=a.heading(heading);head.setOnLongClickListener(v->{java.util.List<CinemaMenu.Item> rows=new java.util.ArrayList<>();rows.add(CinemaMenu.item("隐藏「"+heading+"」",()->{String next=HomeRules.toggleSection(a.prefs.getString("home.hiddenSections",""),"resume");a.prefs.edit().putString("home.hiddenSections",next).apply();android.widget.Toast.makeText(a,"已隐藏，可在「我的 › 首页与外观」恢复",0).show();a.home();}));CinemaMenu.show(a,heading,rows);return true;});parent.addView(head);
  HorizontalScrollView scroller=new HorizontalScrollView(a);scroller.setHorizontalScrollBarEnabled(false);scroller.setClipToPadding(false);
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.TOP);
  int available=a.getResources().getDisplayMetrics().widthPixels-dp(32),width=a.editorial()?Math.min((int)(available*.62f),dp(240)):Math.min((available-dp(10))/2,dp(220));
  for(int i=0;i<Math.min(history.length(),20);i++){
   JSONObject m=history.optJSONObject(i);if(m==null)continue;
   LinearLayout card=continueCard(m,width);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,-2);lp.rightMargin=dp(10);row.addView(card,lp);
  }
  scroller.addView(row);parent.addView(scroller);
 }
 LinearLayout continueCard(JSONObject meta,int width){
  LinearLayout c=new LinearLayout(a);c.setOrientation(1);c.setPadding(0,0,0,dp(6));
  FrameLayout frame=new FrameLayout(a);frame.setBackground(round(a.card(),10));frame.setClipToOutline(true);
  ImageView img=new ImageView(a);img.setScaleType(ImageView.ScaleType.CENTER_CROP);
  frame.addView(img,new FrameLayout.LayoutParams(-1,-1));
  // 0.1.123：剧集优先用这一集的剧照（16:9，和卡片同比例），失败再换背景图 / 海报；都没有时卡片上写片名，不再是一块空灰
  boolean episode="Episode".equals(meta.optString("emby.type"));String bg=meta.optString("background"),poster=meta.optString("poster");
  String art=episode&&!poster.isEmpty()?poster:(bg.isEmpty()?poster:bg),fallback=art.equals(bg)?poster:bg;
  TextView placeholder=a.text(HomeRules.cardTitle(meta),13,a.muted());placeholder.setGravity(Gravity.CENTER);placeholder.setPadding(dp(8),0,dp(8),0);frame.addView(placeholder,0,new FrameLayout.LayoutParams(-1,-1));
  TvPosterImages.load(a,img,art,fallback,width);
  String epId=meta.optString("lastEpisode",meta.optString("id"));
  int pos=a.prefs.getInt("pos:"+epId,0),dur=a.prefs.getInt("dur:"+epId,0);
  if(pos<=0&&meta.optLong("emby.resumeMs")>0)pos=(int)Math.min(Integer.MAX_VALUE,meta.optLong("emby.resumeMs"));
  float frac=HomeRules.progress(pos,dur,HomeRules.runtimeMinutes(meta));
  if(frac>0){
   FrameLayout barHost=new FrameLayout(a);barHost.setBackgroundColor(0x66000000);
   View fill=new View(a);fill.setBackgroundColor(a.gold?CinemaTheme.GOLD:a.clay?CinemaTheme.CLAY:0xff6eb6e0);
   FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams((int)(width*frac),-1,Gravity.LEFT|Gravity.FILL_VERTICAL);
   barHost.addView(fill,fp);
   FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(a.editorial()?3:4),Gravity.BOTTOM);frame.addView(barHost,bp);
  }
  c.addView(frame,new LinearLayout.LayoutParams(width,(int)(width/1.72f)));
  TextView title=a.text(HomeRules.cardTitle(meta),a.editorial()?14:12,a.ink());if(a.editorial())title.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(dp(1),dp(6),dp(1),0);c.addView(title);
  String line=meta.optString("emby.line");TextView sub=a.text(HomeRules.resumeLine(line.isEmpty()?HomeRules.episodeLabel(epId):line,pos,dur),a.editorial()?12:10,a.editorial()?a.muted():a.accent());sub.setPadding(0,dp(2),0,0);c.addView(sub);
  c.setOnClickListener(v->{if(meta.optBoolean("emby.directPlay"))EmbyPlayback.play(a,meta);else a.openItem(meta);});
  c.setOnLongClickListener(v->{String id=meta.optString("id");if(id.isEmpty())id=meta.optString("Id");String store=HomeRules.hideResume(a.prefs.getString("home.hiddenResume","[]"),id);a.prefs.edit().putString("home.hiddenResume",store).apply();if(EmbyApi.isEmbyId(id)){final String watch=id;EmbyPlayback.IO.execute(()->{try{JSONObject server=EmbyStore.get(a,EmbyApi.serverOf(watch));if(server!=null)EmbyClient.hideFromResume(a,server,EmbyApi.itemOf(watch));}catch(Exception ignored){}});}android.widget.Toast.makeText(a,"已从继续观看移除",0).show();a.home();return true;});
  return c;
 }
 /** 媒体库分类瓦片（电影/剧集/动画…），可附带缓存海报拼贴。 */
 void librarySection(){
  if(!HomeRules.sectionShown(a.prefs.getString("home.hiddenSections",""),"libraries"))return;
  TextView head=a.heading("媒体库");head.setOnLongClickListener(v->{java.util.List<CinemaMenu.Item> rows=new java.util.ArrayList<>();rows.add(CinemaMenu.item("隐藏「媒体库」",()->{a.prefs.edit().putString("home.hiddenSections",HomeRules.toggleSection(a.prefs.getString("home.hiddenSections",""),"libraries")).apply();android.widget.Toast.makeText(a,"已隐藏，可在「我的 › 首页与外观」恢复",0).show();a.home();}));CinemaMenu.show(a,"媒体库",rows);return true;});a.body.addView(head);
  HorizontalScrollView scroller=new HorizontalScrollView(a);scroller.setHorizontalScrollBarEnabled(false);
  LinearLayout row=new LinearLayout(a);
  String[][] tiles=HomeRules.libraryTiles();
  int w=dp(148),h=dp(88);
  for(String[] tile:tiles){
   LinearLayout card=libraryTile(tile[0],tile[1],tile[2],w,h);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(w,h);lp.rightMargin=dp(10);row.addView(card,lp);
  }
  scroller.addView(row);a.body.addView(scroller);
 }
 LinearLayout libraryTile(String title,String type,String genre,int w,int h){
  FrameLayout frame=new FrameLayout(a);frame.setBackground(round(a.card(),12));frame.setClipToOutline(true);
  JSONArray cache=null;try{cache=new JSONArray(a.prefs.getString("cinema.catalog."+type+"."+genre,""));}catch(Exception ignored){}
  int cell=Math.max(1,w/2);
  LinearLayout grid=new LinearLayout(a);grid.setOrientation(1);
  for(int r=0;r<2;r++){
   LinearLayout line=new LinearLayout(a);
   for(int c=0;c<2;c++){
    int idx=r*2+c;
    ImageView img=new ImageView(a);img.setScaleType(ImageView.ScaleType.CENTER_CROP);
    img.setBackgroundColor(a.gold?((idx%2==0)?CinemaTheme.TILE_A:CinemaTheme.TILE_B):a.clay?((idx%2==0)?CinemaTheme.TILE_C:CinemaTheme.TILE_D):(idx%2==0)?0xff2a5560:0xff1e4450);
    if(cache!=null&&idx<cache.length()){JSONObject m=cache.optJSONObject(idx);if(m!=null)TvPosterImages.load(a,img,m.optString("poster"),cell);}
    line.addView(img,new LinearLayout.LayoutParams(0,h/2,1));
   }
   grid.addView(line,new LinearLayout.LayoutParams(-1,0,1));
  }
  frame.addView(grid,new FrameLayout.LayoutParams(-1,-1));
  View shade=new View(a);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x0010343e,0xaa10343e}));
  frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  TextView label=a.text(title,15,0xffffffff);label.setTypeface(null,1);label.setGravity(Gravity.CENTER);
  frame.addView(label,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));label.setPadding(0,0,0,dp(10));
  LinearLayout wrap=new LinearLayout(a);wrap.addView(frame,new LinearLayout.LayoutParams(w,h));
  wrap.setOnClickListener(v->{
   if(genre.isEmpty()){a.mode=type;a.browse();}
   else a.genreBrowse(type,genre);
  });
  return wrap;
 }
 /** 给普通海报加角标（评分或集数）。 */
 void decorateBadge(FrameLayout frame,JSONObject meta){
  String badge=HomeRules.badge(meta);if(badge.isEmpty())return;
  // 避免重复叠加
  for(int i=0;i<frame.getChildCount();i++)if(frame.getChildAt(i) instanceof TextView && "badge".equals(frame.getChildAt(i).getTag()))return;
  TextView t=a.text(badge,10,0xffffffff);t.setTag("badge");t.setPadding(dp(5),dp(3),dp(5),dp(3));
  t.setBackground(round(badge.startsWith("★")?0xd0173b4b:0xd03a7ca8,badge.startsWith("★")?6:10));
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);p.setMargins(0,dp(6),dp(6),0);frame.addView(t,p);
 }
 /** 绑定 Hero 文案。影视仓只留标题，有简介才显示简介。 */
 void bindHeroText(JSONObject m){
  String facts=HomeRules.heroFacts(m),desc=HomeRules.heroDescription(m),cert=HomeRules.heroCert(m);
  boolean fixed=a.goldBelow();// 0.1.129：暗金 / 陶土竖屏的文字区高度固定，空的也只留白不收起，换页不触发整页重排
  if(a.heroFacts!=null){a.heroFacts.setText(facts);a.heroFacts.setVisibility(fixed||!facts.isEmpty()?View.VISIBLE:View.GONE);}
  if(a.heroDescription!=null){a.heroDescription.setText(desc);a.heroDescription.setVisibility(fixed||!desc.isEmpty()?View.VISIBLE:View.GONE);a.heroDescription.setTextColor(a.light?a.muted():a.gold?CinemaTheme.SOFT:0xffd6e8eb);}
  if(a.heroFacts!=null)a.heroFacts.setTextColor(a.light?a.muted():a.gold?CinemaTheme.SOFT:0xffd6e8eb);
  if(a.heroCert!=null){a.heroCert.setText(cert);a.heroCert.setVisibility(fixed||cert.isEmpty()?View.GONE:View.VISIBLE);}
  if(a.heroTitle!=null){String logo=m.optString("logo");a.heroTitle.setText(m.optString("name","影片"));if(!fixed)a.heroTitle.setTextSize(logo.isEmpty()?26:16);}
 }
 /** Both providers use the same edge-to-edge artwork and caption layout. */
 void dressHero(FrameLayout slide,ImageView backdrop,JSONObject m){
  if(slide==null||backdrop==null)return;
  for(int i=slide.getChildCount()-1;i>=0;i--){View child=slide.getChildAt(i);if("mp-hero-extra".equals(child.getTag()))slide.removeViewAt(i);}
  if(Build.VERSION.SDK_INT>=31)HomeAtmosphere.Flat31.apply(backdrop);
  backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
  backdrop.setImageDrawable(null);
  String bg=HomeHeroRules.backdropOf(m);
  if(!bg.isEmpty())TvPosterImages.load(a,backdrop,bg,a.getResources().getDisplayMetrics().widthPixels);
  // 横屏：片名和简介靠左，叠一道从左往右的压暗；每张海报（包括滑动中的前后页）都带，换页时不会闪
  if(a.wide()){View side=new View(a);side.setTag("mp-hero-extra");side.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x99000000,0x55000000,0x14000000,0}));slide.addView(side,1,new FrameLayout.LayoutParams(-1,-1));}
 }
}
