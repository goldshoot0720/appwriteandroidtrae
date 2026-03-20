package com.example.appwriteandroidtrae;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class OilPriceSyncWorker extends Worker {

    public OilPriceSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            OilPriceFetcher fetcher = new OilPriceFetcher();
            OilPricePoint point = fetcher.fetchLatestPoint();
            new OilPriceRepository(getApplicationContext()).savePoint(point);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
