package com.guocheng.mediapocket;
import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 0.1.122 Emby 详情页组件，参考 Findroid / Fladder / Streamyfin 的手机端详情页：
 * 规格标签行、操作按钮行（收藏 / 标记已看 / 下载）、演职员横排头像、带缩略图的剧集卡片和加载骨架。
 */
final class EmbyDetailParts {
 private EmbyDetailParts(){}

 /** 规格标签：圆角描边小胶囊，一行横向排开，放不下可横滑。 */
 static View badges(EmbyLibraryActivity a,List<String> labels){if(labels==null||labels.isEmpty())return null;HorizontalScrollView hs=new HorizontalScrollView(a);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  for(String l:labels){TextView t=new TextView(a);t.setText(l);t.setTextSize(Ui.sp(11));t.setTypeface(Typeface.DEFAULT_BOLD);t.setTextColor(l.equals("杜比视界")||l.startsWith("HDR")||l.equals("4K")||l.equals("全景声")?(a.clay?CinemaTheme.CLAY_DEEP:a.accentFill()):a.muted());t.setIncludeFontPadding(false);t.setPadding(a.dp(7),a.dp(3),a.dp(7),a.dp(3));GradientDrawable g=new GradientDrawable();g.setCornerRadius(a.dp(Ui.radius(4)));g.setStroke(a.dp(1),(t.getCurrentTextColor()&0xffffff)|0x99000000);t.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=a.dp(6);row.addView(t,p);}
  hs.addView(row);hs.setPadding(0,0,0,a.dp(10));return hs;}

 /** 操作按钮行：图标在上、文字在下，等分宽度。 */
 static LinearLayout actions(EmbyLibraryActivity a){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);row.setPadding(0,0,0,a.dp(6));return row;}
 static TextView action(EmbyLibraryActivity a,LinearLayout row,String icon,String label,Runnable run){TextView t=new TextView(a);t.setGravity(Gravity.CENTER);t.setTextColor(a.ink());t.setTextSize(Ui.sp(12));t.setText(icon+"\n"+label);t.setLineSpacing(a.dp(2),1f);t.setPadding(0,a.dp(6),0,a.dp(6));t.setBackground(a.selectable());CinemaControls.action(t,label);t.setOnClickListener(v->run.run());row.addView(t,new LinearLayout.LayoutParams(0,-2,1f));return t;}

 /** 演职员：圆形头像 + 名字 + 角色，横滑。没有头像时显示名字首字。 */
 static View cast(EmbyLibraryActivity a,JSONObject server,JSONObject item){JSONArray people=item.optJSONArray("People");if(people==null||people.length()==0)return null;
  LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);TextView head=a.text("演职人员",16);head.setTextColor(a.gold||!a.light?CinemaTheme.INK:a.ink());head.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));head.setPadding(0,a.dp(18),0,a.dp(8));box.addView(head);
  HorizontalScrollView hs=new HorizontalScrollView(a);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(a);hs.addView(row);box.addView(hs);int shown=0;
  for(int i=0;i<people.length()&&shown<20;i++){JSONObject p=people.optJSONObject(i);if(p==null||p.optString("Name").isEmpty())continue;String type=p.optString("Type");if(!(type.equals("Actor")||type.equals("Director")||type.equals("GuestStar")))continue;shown++;
   LinearLayout cell=new LinearLayout(a);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER_HORIZONTAL);int w=a.dp(70),h=a.dp(92);
   FrameLayout face=new FrameLayout(a);GradientDrawable plate=new GradientDrawable();plate.setCornerRadius(a.dp(10));plate.setColor(a.gold||!a.light?0xff2e2920:a.card());face.setBackground(plate);face.setClipToOutline(true);
   String tag=p.optString("PrimaryImageTag");   if(!tag.isEmpty()){ImageView img=new ImageView(a);img.setScaleType(ImageView.ScaleType.CENTER_CROP);face.addView(img,new FrameLayout.LayoutParams(-1,-1));TvPosterImages.loadPoster(a,img,EmbyApi.imageUrl(server.optString("url"),p.optString("Id"),"Primary",server.optString("token"),160,tag),w);}
   else{TextView initial=new TextView(a);initial.setText(p.optString("Name").substring(0,1));initial.setTextColor(a.gold||!a.light?CinemaTheme.MUTED:a.muted());initial.setTextSize(Ui.sp(20));initial.setGravity(Gravity.CENTER);face.addView(initial,new FrameLayout.LayoutParams(-1,-1));}
   cell.addView(face,new LinearLayout.LayoutParams(w,h));
   TextView name=new TextView(a);name.setText(p.optString("Name"));name.setTextColor(a.gold||!a.light?CinemaTheme.INK:a.ink());name.setTextSize(Ui.sp(12));name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(0,a.dp(6),0,0);cell.addView(name,new LinearLayout.LayoutParams(a.dp(76),-2));
   String role=type.equals("Director")?"导演":p.optString("Role");if(!role.isEmpty()){TextView r=new TextView(a);r.setText(role);r.setTextColor(a.gold||!a.light?CinemaTheme.MUTED:a.muted());r.setTextSize(Ui.sp(12));r.setGravity(Gravity.CENTER);r.setMaxLines(1);r.setEllipsize(android.text.TextUtils.TruncateAt.END);cell.addView(r,new LinearLayout.LayoutParams(a.dp(76),-2));}
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(a.dp(76),-2);lp.rightMargin=a.dp(8);row.addView(cell,lp);}
  return shown==0?null:box;}

 /** 剧集卡片：左边 16:9 缩略图（已看打勾、看了一半画进度条），右边标题、时长 / 剩余、两行简介。 */
 static View episode(EmbyLibraryActivity a,JSONObject server,JSONObject ep,Runnable play,Runnable more){
  JSONObject ud=ep.optJSONObject("UserData");boolean played=ud!=null&&ud.optBoolean("Played");long pos=ud==null?0:ud.optLong("PlaybackPositionTicks"),run=ep.optLong("RunTimeTicks");
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.TOP);row.setPadding(0,a.dp(6),0,a.dp(6));row.setBackground(a.selectable());row.setOnClickListener(v->play.run());row.setOnLongClickListener(v->{more.run();return true;});
  int w=a.dp(140),h=w*9/16;FrameLayout thumb=new FrameLayout(a);thumb.setBackground(a.rounded(a.card(),8));thumb.setClipToOutline(true);
  ImageView img=new ImageView(a);img.setScaleType(ImageView.ScaleType.CENTER_CROP);thumb.addView(img,new FrameLayout.LayoutParams(-1,-1));
  try{EmbyApi.ImageRef ref=EmbyApi.fallbackImage(ep,"Primary");TvPosterImages.loadPoster(a,img,EmbyApi.imageUrl(server.optString("url"),ref.itemId.isEmpty()?ep.optString("Id"):ref.itemId,ref.kind,server.optString("token"),360,ref.tag),w);}catch(Exception ignored){}
  if(played){TextView check=new TextView(a);check.setText("✓");check.setTextColor(0xffffffff);check.setTextSize(Ui.sp(11));check.setTypeface(Typeface.DEFAULT_BOLD);check.setGravity(Gravity.CENTER);GradientDrawable g=new GradientDrawable();g.setShape(GradientDrawable.OVAL);g.setColor(0xcc2e7d32);check.setBackground(g);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(a.dp(20),a.dp(20),Gravity.TOP|Gravity.END);cp.setMargins(0,a.dp(5),a.dp(5),0);thumb.addView(check,cp);}
  float pr=MediaBadgeRules.progress(run,pos,played);if(pr>0){View track=new View(a);track.setBackgroundColor(0x66000000);thumb.addView(track,new FrameLayout.LayoutParams(-1,a.dp(3),Gravity.BOTTOM));View bar=new View(a);bar.setBackgroundColor(a.accentFill());thumb.addView(bar,new FrameLayout.LayoutParams(Math.round(w*pr),a.dp(3),Gravity.BOTTOM|Gravity.START));}
  row.addView(thumb,new LinearLayout.LayoutParams(w,h));
  LinearLayout texts=new LinearLayout(a);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(a.dp(12),0,0,0);
  TextView title=new TextView(a);title.setText(MediaBadgeRules.episodeTitle(ep));title.setTextColor(played?a.muted():a.ink());title.setTextSize(Ui.sp(14));title.setTypeface(null,1);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);texts.addView(title);
  String line=MediaBadgeRules.episodeLine(run,pos,played);if(!line.isEmpty()){TextView l=new TextView(a);l.setText(line);l.setTextColor(pr>0?(a.clay?CinemaTheme.CLAY_DEEP:a.accentFill()):a.muted());l.setTextSize(Ui.sp(12));l.setPadding(0,a.dp(2),0,a.dp(2));texts.addView(l);}
  String ov=ep.optString("Overview");if(!ov.isEmpty()){TextView o=new TextView(a);o.setText(ov);o.setTextColor(a.muted());o.setTextSize(Ui.sp(12));o.setMaxLines(2);o.setEllipsize(android.text.TextUtils.TruncateAt.END);texts.addView(o);}
  row.addView(texts,new LinearLayout.LayoutParams(0,-2,1f));return row;}

 /** 加载骨架：几张灰色占位卡片，呼吸闪动，代替「正在读取…」文字。 */
 static View skeleton(EmbyLibraryActivity a,int rows){LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);
  for(int i=0;i<rows;i++){LinearLayout row=new LinearLayout(a);row.setPadding(0,a.dp(6),0,a.dp(6));int w=a.dp(140),h=w*9/16;View thumb=new View(a);thumb.setBackground(a.rounded(a.card(),8));row.addView(thumb,new LinearLayout.LayoutParams(w,h));
   LinearLayout lines=new LinearLayout(a);lines.setOrientation(LinearLayout.VERTICAL);lines.setPadding(a.dp(12),a.dp(4),0,0);for(int j=0;j<3;j++){View bar=new View(a);bar.setBackground(a.rounded(a.card(),4));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(j==0?-1:j==1?a.dp(90):a.dp(160),a.dp(j==0?14:10));bp.bottomMargin=a.dp(8);lines.addView(bar,bp);}
   row.addView(lines,new LinearLayout.LayoutParams(0,-2,1f));box.addView(row);}
  return Skeleton.pulse(box);}
}
