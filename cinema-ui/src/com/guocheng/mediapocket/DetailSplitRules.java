package com.guocheng.mediapocket;
/** Horizontal padding and column width for the landscape detail split. No Android types. */
final class DetailSplitRules {
 private DetailSplitRules(){}
 /** Put each side back to its own saved inset. The right inset must not be copied onto the left. */
 static int[] restore(int savedLeft,int savedRight){return new int[]{Math.max(0,savedLeft),Math.max(0,savedRight)};}
 /**
  * Left column width: the poster is about 36% of the area inside the two insets,
  * and the insets themselves sit outside that poster so a large cutout does not swallow it.
  */
 static int leftColumn(int screen,int insetL,int insetR,int gap){
  insetL=Math.max(0,insetL);insetR=Math.max(0,insetR);gap=Math.max(0,gap);
  int inner=Math.max(0,screen-insetL-insetR-gap);
  int poster=Math.round(inner*0.36f);
  int text=inner-poster;
  int textFloor=Math.round(inner*0.50f);
  if(text<textFloor)poster=Math.max(0,inner-textFloor);
  return insetL+poster+gap;
 }
}
