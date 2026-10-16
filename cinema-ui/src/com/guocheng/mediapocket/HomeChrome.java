package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.content.res.ColorStateList;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import org.json.*;
/** Hills/Emby 风格首页：顶栏片源下拉、续播进度条、媒体库瓦片、海报角标；底栏由 ScrapedMainActivity 组装。 */
final class HomeChrome {
 final ScrapedMainActivity a;
 HomeChrome(ScrapedMainActivity a){this.a=a;}
 int dp(int v){return a.dp(v);}
 GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 /** 顶栏左侧片源下拉（替代「随影」品牌）。 */
 TextView sourceSwitcher(){
  String provider=a.prefs.getString("fusion.homeProvider","stremio"),sourceId=a.prefs.getString("home.source.v1","current");
  JSONArray options;try{options=HomeRules.sourceOptions(HomeSources.list(a));}catch(Exception e){try{options=HomeRules.sourceOptions(new JSONArray());}catch(Exception x){options=new JSONArray();}}
  String id=HomeRules.selectedId(provider,sourceId);
  TextView t=a.text(HomeRules.labelFor(options,id)+"  ▾",16,0xffffffff);t.setTypeface(null,1);
  t.setCompoundDrawablesWithIntrinsicBounds(new NavIcon(9,dp(18),0xffa2d0ed),null,null,null);t.setCompoundDrawablePadding(dp(6));
  t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(0,dp(6),dp(8),dp(6));t.setContentDescription("切换片源");
  final JSONArray opts=options;t.setOnClickListener(v->pickSource(opts,id));return t;
 }
 void pickSource(JSONArray options,String current){
  if(options==null||options.length()==0)return;
  String[] labels=new String[options.length()];int checked=0;
  for(int i=0;i<options.length();i++){JSONObject o=options.optJSONObject(i);labels[i]=o==null?"":o.optString("label");if(o!=null&&current.equals(o.optString("id")))checked=i;}
  new AlertDialog.Builder(a).setTitle("选择片源").setSingleChoiceItems(labels,checked,(d,n)->{
   JSONObject o=options.optJSONObject(n);if(o==null){d.dismiss();return;}
   String[] applied=HomeRules.apply(o.optString("id"));
   a.prefs.edit().putString("fusion.homeProvider",applied[0]).putString("home.source.v1",applied[1]).apply();
   a.fusionSession=null;d.dismiss();a.home();
  }).show();
 }
 /** 继续观看横滑行，带底部蓝色进度条与集数文案。 */
 void continueWatching(JSONArray history){
  if(history==null||history.length()==0)return;
  a.body.addView(a.heading("继续观看"));
  HorizontalScrollView scroller=new HorizontalScrollView(a);scroller.setHorizontalScrollBarEnabled(false);scroller.setClipToPadding(false);
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.TOP);
  int available=a.getResources().getDisplayMetrics().widthPixels-dp(32),width=Math.min((available-dp(10))/2,dp(220));
  for(int i=0;i<Math.min(history.length(),20);i++){
   JSONObject m=history.optJSONObject(i);if(m==null)continue;
   LinearLayout card=continueCard(m,width);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,-2);lp.rightMargin=dp(10);row.addView(card,lp);
  }
  scroller.addView(row);a.body.addView(scroller);
 }
 LinearLayout continueCard(JSONObject meta,int width){
  LinearLayout c=new LinearLayout(a);c.setOrientation(1);c.setPadding(0,0,0,dp(6));
  FrameLayout frame=new FrameLayout(a);frame.setBackground(round(a.card(),10));frame.setClipToOutline(true);
  ImageView img=new ImageView(a);img.setScaleType(ImageView.ScaleType.CENTER_CROP);
  frame.addView(img,new FrameLayout.LayoutParams(-1,-1));
  String art=meta.optString("background",meta.optString("poster"));
  TvPosterImages.load(a,img,art,width);
  String epId=meta.optString("lastEpisode",meta.optString("id"));
  int pos=a.prefs.getInt("pos:"+epId,0),dur=a.prefs.getInt("dur:"+epId,0);
  float frac=HomeRules.progress(pos,dur,HomeRules.runtimeMinutes(meta));
  if(frac>0){
   FrameLayout barHost=new FrameLayout(a);barHost.setBackgroundColor(0x66000000);
   View fill=new View(a);fill.setBackgroundColor(0xff6eb6e0);
   FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams((int)(width*frac),-1,Gravity.LEFT|Gravity.FILL_VERTICAL);
   barHost.addView(fill,fp);
   FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(4),Gravity.BOTTOM);frame.addView(barHost,bp);
  }
  c.addView(frame,new LinearLayout.LayoutParams(width,(int)(width/1.72f)));
  TextView title=a.text(meta.optString("name","影片"),12,a.ink());title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(dp(1),dp(6),dp(1),0);c.addView(title);
  TextView sub=a.text(HomeRules.episodeLabel(epId),10,a.accent());sub.setPadding(0,dp(2),0,0);c.addView(sub);
  c.setOnClickListener(v->a.openItem(meta));
  return c;
 }
 /** 媒体库分类瓦片（电影/剧集/动画…），可附带缓存海报拼贴。 */
 void librarySection(){
  a.body.addView(a.heading("媒体库"));
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
    img.setBackgroundColor((idx%2==0)?0xff2a5560:0xff1e4450);
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
 /** 绑定 Hero 文案（评分·年份·类型 + 简介）。 */
 void bindHeroText(JSONObject m){
  if(a.heroFacts!=null)a.heroFacts.setText(HomeRules.heroFacts(m));
  if(a.heroDescription!=null)a.heroDescription.setText(HomeRules.heroDescription(m));
  if(a.heroTitle!=null){String logo=m.optString("logo");a.heroTitle.setText(m.optString("name","影片"));a.heroTitle.setTextSize(logo.isEmpty()?26:16);}
 }
}
