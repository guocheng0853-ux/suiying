package com.guocheng.mediapocket;
import android.content.Context;import android.widget.FrameLayout;
/**
 * 首页海报页。底部衔接改由盖在上面的渐变负责（透明过渡到氛围底色），
 * 这里不再铺实色底，也不再用 DST_IN 把图片切掉——实色底切在氛围色上就是一条硬边。
 */
final class FadeFrame extends FrameLayout {
 FadeFrame(Context c){super(c);setBackgroundColor(0);}
}
