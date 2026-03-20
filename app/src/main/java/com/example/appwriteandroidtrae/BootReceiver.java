package com.example.appwriteandroidtrae;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {

            // 移除其他同名程式的殘留進程
            killExistingProcesses(context);

            // 立即執行一次訂閱檢查
            OneTimeWorkRequest immediateCheck = new OneTimeWorkRequest.Builder(
                    SubscriptionCheckWorker.class
            ).build();
            WorkManager.getInstance(context).enqueue(immediateCheck);

            // 排程每日背景檢查
            scheduleDailySubscriptionCheck(context);
        }
    }

    private void killExistingProcesses(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        String myPackage = context.getPackageName();
        int myPid = android.os.Process.myPid();

        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (myPackage.equals(process.processName) && process.pid != myPid) {
                    android.os.Process.killProcess(process.pid);
                }
            }
        }
    }

    private void scheduleDailySubscriptionCheck(Context context) {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, 6);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long firstRun = calendar.getTimeInMillis();
        if (firstRun <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            firstRun = calendar.getTimeInMillis();
        }
        long initialDelay = firstRun - now;

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SubscriptionCheckWorker.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "subscription_check",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );
    }
}
