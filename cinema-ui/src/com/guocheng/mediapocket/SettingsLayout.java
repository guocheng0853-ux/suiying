package com.guocheng.mediapocket;
import android.graphics.Typeface;import android.view.*;import android.widget.*;import java.util.*;
/**
 * 0.1.97「我的」分组：原版、加速、故障排查、弹幕字幕、状态栏等按钮原来按加功能的先后平铺约 30 个。
 * 页面照常生成后按标签前缀归到 7 组，每组一个小标题；「名称：开 / 关」改成开关行，其余改成「名称 … 当前值 ›」行。
 * 点击仍然执行原按钮的逻辑（performClick / performLongClick），功能不变；认不出的项放进最后的「其他」，不会丢。
 */
final class SettingsLayout {
 private SettingsLayout(){}
 static boolean keepScroll;static int savedScroll;
 /** 点开关前记下滚动位置，原按钮触发整页重建后滚回原处。 */
 static void remember(ScrapedMainActivity a){keepScroll=true;savedScroll=a.scroll==null?0:a.scroll.getScrollY();}

 static void apply(ScrapedMainActivity a){
  try{
   LinearLayout body=a.body;if(body==null)return;
   addDefaultSpeed(a);
   List<View> kids=new ArrayList<>();for(int i=0;i<body.getChildCount();i++)kids.add(body.getChildAt(i));
   View title=null;Map<Integer,List<Object[]>> groups=new TreeMap<>();
   for(View v:kids){
    if(!(v instanceof TextView)){add(groups,7,999,v,null);continue;}
    String label=((TextView)v).getText().toString();
    if(!(v instanceof Button)&&label.startsWith("随影新版")){title=v;continue;}
    // 原版设置页开头的「设置」标题和空白提示行：页面已有「随影新版 x.y.z」标题，丢掉（真机上它们落进了「其他」组）
    if(!(v instanceof Button)&&(label.trim().isEmpty()||label.trim().equals("设置")))continue;
    int[] at=SettingsRules.place(label);if(at[0]<0)continue;
    add(groups,at[0],at[1],v,label);
   }
   body.removeAllViews();
   if(title!=null)body.addView(title);
   body.addView(summary(a));
   for(Map.Entry<Integer,List<Object[]>> g:groups.entrySet()){
    List<Object[]> items=g.getValue();if(items.isEmpty())continue;items.sort((x,y)->Integer.compare((Integer)x[0],(Integer)y[0]));
    TextView head=a.text(SettingsRules.GROUPS[g.getKey()],13,a.muted());head.setTypeface(a.editorial()?CinemaTheme.paperTitle(a):Typeface.DEFAULT_BOLD);if(a.editorial())head.setTextSize(Ui.sp(21));head.setPadding(a.dp(4),a.dp(a.editorial()?28:18),0,a.dp(a.editorial()?12:6));body.addView(head);
    LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(a.rounded(a.card(),a.clay?12:16));if(a.clay)((android.graphics.drawable.GradientDrawable)card.getBackground()).setStroke(a.dp(1),CinemaTheme.LINE);card.setPadding(0,a.dp(4),0,a.dp(4));
    boolean first=true;for(Object[] it:items){View v=(View)it[1];String label=(String)it[2];if(!first){View line=new View(a);line.setBackgroundColor((a.muted()&0xffffff)|0x26000000);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,Math.max(1,a.dp(1)/2));dl.leftMargin=a.dp(16);card.addView(line,dl);}first=false;card.addView(v instanceof Button&&label!=null?row(a,(Button)v,label):detach(v));}
    body.addView(card,new LinearLayout.LayoutParams(-1,-2));
   }
   if(keepScroll&&a.scroll!=null){final int y=savedScroll;a.scroll.post(()->a.scroll.scrollTo(0,y));}
   keepScroll=false;
  }catch(Exception ignored){}
 }
 /** 0.1.124 顶部概览卡片：首页来源、Emby 服务器、收藏、离线下载，点了直达对应设置。 */
 static View summary(ScrapedMainActivity a){LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.HORIZONTAL);card.setBackground(a.rounded(a.card(),a.clay?12:16));if(a.clay)((android.graphics.drawable.GradientDrawable)card.getBackground()).setStroke(a.dp(1),CinemaTheme.LINE);card.setPadding(a.dp(8),a.dp(14),a.dp(8),a.dp(14));
  int servers=EmbyStore.list(a).length(),fav=FollowUp.favorites(a).length(),off=OfflineDownloads.list(a).length();
  stat(a,card,String.valueOf(servers),"Emby 服务器",()->EmbyLibraryActivity.manage(a));stat(a,card,String.valueOf(fav),"收藏",()->a.navClick("收藏"));stat(a,card,String.valueOf(off),"离线下载",()->a.startActivity(new android.content.Intent(a,DownloadsActivity.class)));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(8);card.setLayoutParams(lp);return card;}
 static void stat(ScrapedMainActivity a,LinearLayout card,String value,String label,Runnable go){LinearLayout cell=new LinearLayout(a);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);TextView v=a.text(value,22,a.ink());v.setTypeface(a.editorial()?CinemaTheme.paperTitle(a):Typeface.DEFAULT_BOLD);if(a.editorial())v.setTextSize(Ui.sp(28));v.setGravity(Gravity.CENTER);cell.addView(v);TextView l=a.text(label,12,a.muted());l.setGravity(Gravity.CENTER);l.setPadding(0,a.dp(2),0,0);cell.addView(l);android.util.TypedValue tv=new android.util.TypedValue();a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,tv,true);if(tv.resourceId!=0)cell.setBackgroundResource(tv.resourceId);CinemaControls.group(cell,label+"，"+value);cell.setOnClickListener(x->go.run());card.addView(cell,new LinearLayout.LayoutParams(0,-2,1f));}
 static void add(Map<Integer,List<Object[]>> groups,int group,int order,View v,String label){groups.computeIfAbsent(group,k->new ArrayList<>()).add(new Object[]{order,v,label});}
 static View detach(View v){if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);return v;}

 static View row(ScrapedMainActivity a,Button original,String label){
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(a.dp(a.clay?60:52));row.setPadding(a.dp(16),a.dp(8),a.dp(12),a.dp(8));
  android.util.TypedValue tv=new android.util.TypedValue();a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,tv,true);if(tv.resourceId!=0)row.setBackgroundResource(tv.resourceId);
  LinearLayout texts=new LinearLayout(a);texts.setOrientation(LinearLayout.VERTICAL);
  String[] t=SettingsRules.toggle(label);String[] kv=SettingsRules.split(label);
  TextView name=a.text(t!=null?t[0]:kv[0],15,a.ink());name.setPadding(0,0,0,0);texts.addView(name);
  String note=t!=null?t[2]:"";if(!note.isEmpty()){TextView n=a.text(note,12,a.muted());n.setPadding(0,a.dp(2),0,0);texts.addView(n);}
  row.addView(texts,new LinearLayout.LayoutParams(0,-2,1f));
  final Switch sw;
  if(t!=null){sw=new Switch(a);sw.setChecked("开".equals(t[1]));sw.setClickable(false);sw.setFocusable(false);if(a.clay||a.gold){int[][] states={new int[]{android.R.attr.state_checked},new int[]{}};sw.setThumbTintList(new android.content.res.ColorStateList(states,new int[]{a.gold?CinemaTheme.GOLD:CinemaTheme.CLAY_DEEP,a.muted()}));sw.setTrackTintList(new android.content.res.ColorStateList(states,new int[]{a.gold?CinemaTheme.CHIP:CinemaTheme.CLAY_TRACK,a.gold?0xff46433c:CinemaTheme.LINE}));}row.addView(sw);}
  else{sw=null;String value=kv[1];if(!value.isEmpty()){TextView current=a.text(value,Ui.SMALL,a.muted());current.setMaxLines(3);current.setEllipsize(android.text.TextUtils.TruncateAt.END);current.setPadding(0,a.dp(Ui.S1),a.dp(Ui.S2),0);texts.addView(current);}TextView right=a.text("›",Ui.SECTION,a.muted());right.setPadding(a.dp(Ui.S2),0,0,0);right.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(right);}
  row.setFocusable(true);if(android.os.Build.VERSION.SDK_INT>=28)row.setScreenReaderFocusable(true);texts.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);if(sw!=null)sw.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  row.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(sw==null?"android.widget.Button":"android.widget.Switch");info.setContentDescription(original.getText());if(sw!=null){info.setCheckable(true);info.setChecked(sw.isChecked());}}});
  row.setOnClickListener(v->{remember(a);original.performClick();if(sw!=null)row.post(()->{String[] now=SettingsRules.toggle(original.getText().toString());if(now!=null)sw.setChecked("开".equals(now[1]));});});
  row.setOnLongClickListener(v->{remember(a);return original.performLongClick();});
  return row;
 }

 /** 新增「默认倍速」：播放页底栏倍速按钮的起始值。 */
 static void addDefaultSpeed(ScrapedMainActivity a){
  float cur=PlaybackPrefs.speed(a.prefs);
  a.body.addView(a.button("默认倍速："+PlaybackPrefs.speedLabel(cur),()->{float[] all={0.5f,0.75f,1f,1.25f,1.5f,2f,2.5f,3f};List<CinemaMenu.Item> rows=new ArrayList<>();float now=PlaybackPrefs.speed(a.prefs);for(float v:all){final float rate=v;rows.add(CinemaMenu.choice(PlaybackPrefs.speedLabel(rate),Math.abs(now-rate)<.02f,()->{PlaybackPrefs.setSpeed(a.prefs,rate);remember(a);a.preferences();}));}CinemaMenu.show(a,"默认倍速",rows);}));
 }
}
