package com.guocheng.mediapocket;
public class PlaybackPrefsTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] a){
  String[] n={"Disable","English - [eng]","粤语 - [yue]","国语 - [chi]","Commentary [eng]"};int[] id={-1,1,2,3,4};
  check(PlaybackPrefs.pick(n,id,1,"zh")==3,"mandarin");check(PlaybackPrefs.pick(n,id,1,"yue")==2,"cantonese");check(PlaybackPrefs.pick(n,id,3,"en")==1,"english");
  check(PlaybackPrefs.pick(n,id,3,"zh")==-1,"already selected");check(PlaybackPrefs.pick(n,id,1,"auto")==-1,"auto untouched");check(PlaybackPrefs.pick(new String[]{"Track 1","Track 2"},new int[]{1,2},1,"zh")==-1,"no match");
  check(PlaybackPrefs.pick(new String[]{"Cantonese chi"},new int[]{5},1,"zh")==-1,"cantonese not mandarin");check(PlaybackPrefs.pick(new String[]{"Chinese"},new int[]{5},1,"zh")==5,"chinese");
  check(PlaybackPrefs.pick(n,null,1,"zh")==-1,"null track IDs");check(PlaybackPrefs.pick(n,new int[]{-1},1,"zh")==-1,"short track IDs");
  System.out.println("PASS: audio preference matching (mandarin/cantonese/english, commentary skipped, no-op when matched/auto).");}
}
