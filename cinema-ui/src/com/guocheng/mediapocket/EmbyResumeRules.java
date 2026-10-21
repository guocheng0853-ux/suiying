package com.guocheng.mediapocket;
/** Switching versions keeps position; switching episodes must use the new episode's progress. */
final class EmbyResumeRules {
 static int keep(boolean replace,String current,String requested,int position){return replace&&current!=null&&!current.isEmpty()&&current.equals(requested)?position:-1;}
}
