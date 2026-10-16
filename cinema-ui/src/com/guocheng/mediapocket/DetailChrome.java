package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.graphics.drawable.GradientDrawable;import android.net.Uri;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/** 详情页重排：背景+logo、信息行、类型标签、片源卡片（片源/音轨/字幕）、▶ 播放 + ⋮ 底部面板。沿用 MediaPocket 深青色配色。 */
final class DetailChrome {
 final ScrapedMainActivity a;final JSONObject meta;final String id,type;TextView sourceValue;
 DetailChrome(ScrapedMainActivity a,JSONObject meta,String id,String type){this.a=a;this.meta=meta;this.id=id;this.type=type;}
 int dp(int v){return a.dp(v);}
 GradientDrawable round(int color,int radius,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
 TextView label(String s,int size,int color){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 static String info(JSONObject m){ArrayList<String> parts=new ArrayList<>();String r=m.optString("imdbRating");if(!r.isEmpty())parts.add("★ "+r);for(String k:new String[]{"releaseInfo","runtime"}){String v=m.optString(k);if(!v.isEmpty())parts.add(v);}return String.join("  ·  ",parts);}
 static String certification(JSONObject m){for(String k:new String[]{"certification","ageRating","rating","contentRating"}){Object v=m.opt(k);if(v instanceof String&&!((String)v).isEmpty()&&((String)v).length()<=8)return (String)v;}return "";}
 void apply(){LinearLayout body=a.body;FrameLayout hero=null;int heroAt=-1;for(int i=0;i<body.getChildCount();i++)if(body.getChildAt(i) instanceof FrameLayout){hero=(FrameLayout)body.getChildAt(i);heroAt=i;break;}if(hero==null)return;
  // 1. 背景底部：logo 或标题 + 信息行 + 分级徽章
  for(int i=hero.getChildCount()-1;i>=0;i--)if(hero.getChildAt(i) instanceof TextView)hero.removeViewAt(i);
  LinearLayout cap=new LinearLayout(a);cap.setOrientation(1);cap.setGravity(Gravity.CENTER_HORIZONTAL);cap.setPadding(dp(20),0,dp(20),dp(16));String logo=meta.optString("logo");TextView title=label(meta.optString("name"),logo.isEmpty()?26:17,0xffffffff);title.setTypeface(null,1);title.setGravity(Gravity.CENTER);
  if(!logo.isEmpty()){ImageView li=new ImageView(a);li.setScaleType(ImageView.ScaleType.FIT_CENTER);TvPosterImages.load(a,li,logo,dp(320));cap.addView(li,new LinearLayout.LayoutParams(dp(260),dp(80)));}
  cap.addView(title);LinearLayout infoRow=new LinearLayout(a);infoRow.setGravity(Gravity.CENTER);TextView infoText=label(info(meta),13,0xffdbe9ef);infoRow.addView(infoText);String cert=certification(meta);if(!cert.isEmpty()){TextView badge=label(cert,11,0xffffffff);badge.setPadding(dp(6),dp(1),dp(6),dp(1));badge.setBackground(round(0,4,0xccffffff));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.leftMargin=dp(8);infoRow.addView(badge,bp);}cap.addView(infoRow);
  hero.addView(cap,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  // 2. 去掉原信息行，类型改为标签
  JSONArray genres=meta.optJSONArray("genres");String joined="";if(genres!=null){ArrayList<String> g=new ArrayList<>();for(int i=0;i<genres.length();i++)g.add(genres.optString(i));joined=String.join("  ·  ",g);}
  int insertAt=heroAt+1;for(int i=body.getChildCount()-1;i>heroAt;i--){View v=body.getChildAt(i);if(!(v instanceof TextView)||v instanceof Button)continue;String t=((TextView)v).getText().toString();if(t.contains("IMDb")&&i<=heroAt+2)body.removeViewAt(i);else if(!joined.isEmpty()&&t.equals(joined)&&i<=heroAt+3)body.removeViewAt(i);}
  if(genres!=null&&genres.length()>0){HorizontalScrollView hs=new HorizontalScrollView(a);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(a);for(int i=0;i<genres.length();i++){TextView c=label(genres.optString(i),13,a.ink());c.setPadding(dp(12),dp(5),dp(12),dp(5));c.setBackground(round(0,14,a.muted()));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.rightMargin=dp(8);chips.addView(c,cp);}hs.addView(chips);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.bottomMargin=dp(10);body.addView(hs,insertAt++,hp);}
  // 3. 片源卡片
  boolean movie="movie".equals(type);LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setBackground(round(a.card(),14,0));card.setPadding(dp(14),dp(4),dp(14),dp(4));
  if(movie){sourceValue=row(card,"片源",sourceText(),()->a.streams(id,type,meta));divider(card);}
  String[] audioValue={PlaybackPrefs.AUDIO_LABELS[PlaybackPrefs.index(PlaybackPrefs.AUDIO,PlaybackPrefs.audio(a.prefs))]};TextView[] audioRow=new TextView[1];
  audioRow[0]=row(card,"音轨",audioValue[0],()->new AlertDialog.Builder(a).setTitle("音轨偏好 · 开播后自动匹配").setSingleChoiceItems(PlaybackPrefs.AUDIO_LABELS,PlaybackPrefs.index(PlaybackPrefs.AUDIO,PlaybackPrefs.audio(a.prefs)),(d,n)->{a.prefs.edit().putString("pref.audio",PlaybackPrefs.AUDIO[n]).apply();audioRow[0].setText(PlaybackPrefs.AUDIO_LABELS[n]+"  ›");d.dismiss();}).show());divider(card);
  TextView[] subRow=new TextView[1];subRow[0]=row(card,"字幕",PlaybackPrefs.SUB_LABELS[PlaybackPrefs.index(PlaybackPrefs.SUBS,PlaybackPrefs.subtitle(a.prefs))],()->new AlertDialog.Builder(a).setTitle("字幕偏好 · 开播后自动匹配").setSingleChoiceItems(PlaybackPrefs.SUB_LABELS,PlaybackPrefs.index(PlaybackPrefs.SUBS,PlaybackPrefs.subtitle(a.prefs)),(d,n)->{PlaybackPrefs.setSubtitle(a.prefs,PlaybackPrefs.SUBS[n]);subRow[0].setText(PlaybackPrefs.SUB_LABELS[n]+"  ›");d.dismiss();}).show());
  LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(-1,-2);cardLp.bottomMargin=dp(12);body.addView(card,insertAt++,cardLp);
  // 4. ▶ 播放 胶囊 + ⋮ 圆形（电影替换原「选择播放源」；剧集在选集中播放，仅加 ⋮ 不加播放）
  LinearLayout actions=new LinearLayout(a);actions.setGravity(Gravity.CENTER_VERTICAL);
  if(movie){for(int i=body.getChildCount()-1;i>=0;i--){View v=body.getChildAt(i);if(v instanceof Button&&((Button)v).getText().toString().contains("选择播放源"))body.removeViewAt(i);}
   Button play=new Button(a);play.setText("▶  播放");play.setAllCaps(false);play.setTextSize(17);play.setTypeface(null,1);play.setTextColor(a.light?0xffffffff:0xff10343e);play.setBackground(round(a.accent(),26,0));play.setOnClickListener(v->play());actions.addView(play,new LinearLayout.LayoutParams(0,dp(52),1));}
  else actions.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));
  Button more=new Button(a);more.setText("⋮");more.setTextSize(20);more.setTextColor(a.ink());more.setBackground(round(a.card(),26,0));more.setContentDescription("更多");more.setOnClickListener(v->sheet());LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(52),dp(52));mp.leftMargin=dp(10);actions.addView(more,mp);
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.bottomMargin=dp(14);body.addView(actions,insertAt,ap);}
 TextView row(LinearLayout card,String name,String value,Runnable click){LinearLayout r=new LinearLayout(a);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(0,dp(12),0,dp(12));TextView n=label(name,14,a.muted());r.addView(n,new LinearLayout.LayoutParams(dp(56),-2));TextView v=label(value+"  ›",14,a.ink());v.setMaxLines(3);r.addView(v,new LinearLayout.LayoutParams(0,-2,1));r.setOnClickListener(x->click.run());card.addView(r);return v;}
 void divider(LinearLayout card){View line=new View(a);line.setBackgroundColor(a.light?0x14000000:0x22ffffff);card.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
 JSONObject picked(){return a.picked.get(id);}
 String sourceText(){JSONObject s=picked();if(s==null)return "自动 · 点击选择片源";StreamInfo i=StreamInfo.parse(s);String name=!i.file.isEmpty()?i.file:s.optString("title",s.optString("name","片源")).split("\n")[0];List<String> t=i.tags(StreamInfo.minutes(meta.optString("runtime")));return name+(t.isEmpty()?"":"\n"+String.join(" · ",t));}
 void refresh(){if(sourceValue!=null)sourceValue.setText(sourceText()+"  ›");}
 void play(){JSONObject s=picked();if(s==null){a.streams(id,type,meta);return;}JSONArray all=a.pickedAll.get(id);a.currentSources=all==null?new JSONArray().put(s):all;a.sourceEpisode=id;a.play(s,id,type,meta);}
 /** ⋮ 底部面板：外部播放器 / 搜索其他播放源 / 复制播放链接 / 投屏（暂不支持，灰显）。 */
 void sheet(){Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout root=new LinearLayout(a);root.setOrientation(1);root.setPadding(dp(16),dp(10),dp(16),dp(18));GradientDrawable g=new GradientDrawable();g.setColor(a.surface());g.setCornerRadii(new float[]{dp(24),dp(24),dp(24),dp(24),0,0,0,0});root.setBackground(g);
  TextView handle=label("━━",14,a.muted());handle.setGravity(Gravity.CENTER);root.addView(handle);
  LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(0,dp(6),0,dp(10));ImageView poster=new ImageView(a);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);poster.setBackground(round(a.card(),8,0));poster.setClipToOutline(true);TvPosterImages.load(a,poster,meta.optString("poster"),dp(120));head.addView(poster,new LinearLayout.LayoutParams(dp(48),dp(72)));LinearLayout names=new LinearLayout(a);names.setOrientation(1);names.setPadding(dp(12),0,0,0);TextView t=label(meta.optString("name"),17,a.ink());t.setTypeface(null,1);names.addView(t);names.addView(label(info(meta),12,a.muted()));head.addView(names,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);
  JSONObject s=picked();String link=s==null?"":StreamInfo.link(s);boolean movie="movie".equals(type);
  sheetRow(root,"⇱  外部播放器播放",s!=null&&!s.optString("url").isEmpty(),()->{d.dismiss();external(s);},s==null?"先选择一个片源":"磁力片源请在应用内播放");
  sheetRow(root,"⌕  搜索其他播放源",movie,()->{d.dismiss();a.streams(id,type,meta);},"剧集请在选集中选择片源");
  sheetRow(root,"⧉  复制播放链接",!link.isEmpty(),()->{d.dismiss();ClipboardManager cm=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("播放链接",link));Toast.makeText(a,"已复制播放链接",Toast.LENGTH_SHORT).show();},"先选择一个片源");
  sheetRow(root,"▭  投屏",false,null,"暂不支持");
  d.setContentView(root);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(Math.min(a.getResources().getDisplayMetrics().widthPixels,dp(640)),-2);w.setGravity(Gravity.BOTTOM);}}
 void sheetRow(LinearLayout root,String label,boolean enabled,Runnable action,String disabledHint){LinearLayout r=new LinearLayout(a);r.setOrientation(1);r.setPadding(dp(4),dp(14),dp(4),dp(14));TextView t=label(label,16,enabled?a.ink():a.muted());r.addView(t);if(!enabled){TextView h=label(disabledHint,12,a.muted());r.addView(h);r.setAlpha(.6f);}else r.setOnClickListener(v->action.run());root.addView(r);}
 void external(JSONObject s){Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse(s.optString("url")),"video/*");i.putExtra("title",meta.optString("name"));try{a.startActivity(Intent.createChooser(i,"选择播放器"));}catch(Exception e){Toast.makeText(a,"没有可用的外部播放器",Toast.LENGTH_SHORT).show();}}
}
