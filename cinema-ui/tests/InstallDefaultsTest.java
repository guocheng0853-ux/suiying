package com.guocheng.mediapocket;
public final class InstallDefaultsTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  InstallDefaults.Choice used=InstallDefaults.choose(true,"clay");
  check(!used.writeProvider&&!used.writeAppearance&&"clay".equals(used.appearance),"upgrade keeps clay");
  InstallDefaults.Choice dark=InstallDefaults.choose(true,"dark");
  check(!dark.writeProvider&&dark.writeAppearance&&"gold".equals(dark.appearance),"old teal dark folds into gold");
  check("clay".equals(InstallDefaults.choose(true,"light").appearance),"old day folds into clay");
  InstallDefaults.Choice none=InstallDefaults.choose(true,null);
  check(none.writeAppearance&&"system".equals(none.appearance),"unset appearance follows system");
  check(!InstallDefaults.choose(true,"system").writeAppearance,"system stays");
  InstallDefaults.Choice fresh=InstallDefaults.choose(false,null);
  check(fresh.writeProvider&&fresh.writeAppearance&&"system".equals(fresh.appearance),"new install follows system");
  check("clay".equals(InstallDefaults.effective("system",false))&&"gold".equals(InstallDefaults.effective("system",true))&&"gold".equals(InstallDefaults.effective("gold",false))&&"clay".equals(InstallDefaults.effective("clay",true)),"effective");
  check(InstallDefaults.label("system",true).equals("跟随系统 · 当前影院暗金")&&InstallDefaults.label("clay",true).equals("暖白陶土"),"label");
  System.out.println("PASS: two appearances plus follow-system, legacy folding, fresh installs follow system.");
 }
}
