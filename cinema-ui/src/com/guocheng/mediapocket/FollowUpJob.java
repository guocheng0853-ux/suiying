package com.guocheng.mediapocket;
import android.app.job.*;
/** 0.1.106 后台定时检查追剧更新（系统 JobScheduler，联网时每 6 小时左右一次）。 */
public final class FollowUpJob extends JobService {
 @Override public boolean onStartJob(JobParameters params){FollowUp.IO.execute(()->{try{FollowUp.check(getApplicationContext());}catch(Throwable ignored){}jobFinished(params,false);});return true;}
 @Override public boolean onStopJob(JobParameters params){return true;}
}
