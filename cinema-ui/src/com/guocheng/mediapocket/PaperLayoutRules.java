package com.guocheng.mediapocket;
/** Tablet and caption spacing. No Android types, so JVM tests can lock the numbers. */
final class PaperLayoutRules {
 private PaperLayoutRules(){}
 /** sw600dp, same cutoff as a smallest-width resource qualifier. */
 static final int TABLET_SW=600;
 static final float TABLET_LANDSCAPE_HERO=0.40f;
 static boolean tablet(int smallestWidthDp){return smallestWidthDp>=TABLET_SW;}
 /** Phone portrait keeps the short fade. Phone landscape keeps the taller overlay fade. Tablets use a short one. */
 static int fadeSpanDp(boolean tablet,boolean textBelow){if(tablet)return 64;return textBelow?90:180;}
 /** Portrait, and tablet landscape, stack the title under the artwork. Phone landscape still overlays it. */
 static boolean stackCaptionBelow(boolean wide,boolean tablet){return !wide||tablet;}
 static int sectionSp(boolean tablet){return tablet?25:22;}
 static int posterSp(boolean tablet){return tablet?15:13;}
 static int resumeSp(boolean tablet){return tablet?16:14;}
 static int factsSp(boolean tablet){return tablet?14:13;}
 static int synopsisSp(boolean tablet){return tablet?15:13;}
 static int kickerBottomDp(boolean tablet){return tablet?1:2;}
 static int captionTopDp(boolean tablet){return tablet?2:2;}
 static int metaTopDp(boolean tablet){return tablet?1:2;}
 static int synopsisTopDp(boolean tablet){return tablet?2:4;}
 static int actionsTopDp(boolean tablet){return tablet?6:8;}
 static int dotsTopDp(boolean tablet){return tablet?4:6;}
 static int buttonHeightDp(boolean tablet){return 44;}
 /** Assumed caption block when sizing the portrait hero so the next row still fits. */
 static int captionReserveDp(){return 196;}
}
