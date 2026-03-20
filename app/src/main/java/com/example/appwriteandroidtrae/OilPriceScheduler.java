package com.example.appwriteandroidtrae;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class OilPriceScheduler {

    private static final String DAILY_WORK_NAME = "oil_price_sync_daily";
    private static final String IMMEDIATE_WORK_NAME = "oil_price_sync_now";

    private OilPriceScheduler() {
    }

    public static void enqueueImmediateFetch(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OilPriceSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    public static void scheduleDailyFetch(Context context) {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long firstRun = calendar.getTimeInMillis();
        if (firstRun <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            firstRun = calendar.getTimeInMillis();
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                OilPriceSyncWorker.class,
                24, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .setInitialDelay(firstRun - now, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );
    }
}
