package com.guocheng.mediapocket;
public final class InstallDefaultsTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  InstallDefaults.Choice used=InstallDefaults.choose(true,"clay");
  check(!used.writeProvider&&!used.writeAppearance&&"clay".equals(used.appearance),"upgrade keeps clay");
  InstallDefaults.Choice dark=InstallDefaults.choose(true,null);
  check(!dark.writeProvider&&"dark".equals(dark.appearance),"missing appearance stays dark");
  InstallDefaults.Choice fresh=InstallDefaults.choose(false,"dark");
  check(fresh.writeProvider&&fresh.writeAppearance&&"clay".equals(fresh.appearance),"new install defaults to fusion clay");
  System.out.println("PASS: install defaults only rewrite a fresh install.");
 }
}
