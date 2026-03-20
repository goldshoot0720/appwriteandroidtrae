package com.example.appwriteandroidtrae;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SubscriptionCheckWorker extends Worker {

    private static final String CHANNEL_ID = "subscription_expiry_channel";

    public SubscriptionCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppwriteHelper helper = AppwriteHelper.getInstance(getApplicationContext());
            List<AppwriteHelper.SubscriptionItem> items = helper.listSubscriptionsSync();
            checkExpiringSubscriptions(items);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void checkExpiringSubscriptions(List<AppwriteHelper.SubscriptionItem> items) {
        long now = System.currentTimeMillis();
        long threeDaysMillis = 3L * 24L * 60L * 60L * 1000L;
        for (AppwriteHelper.SubscriptionItem item : items) {
            if (item.nextDateMillis <= 0L) {
                continue;
            }
            if (!item.continueFlag) {
                continue;
            }
            if (item.nextDateMillis >= now && item.nextDateMillis <= now + threeDaysMillis) {
                long daysLeft = TimeUnit.MILLISECONDS.toDays(item.nextDateMillis - now);
                showExpiryNotification(item, daysLeft);
            }
        }
    }

    private void showExpiryNotification(AppwriteHelper.SubscriptionItem item, long daysLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannel();

        String daysText;
        if (daysLeft <= 0) {
            daysText = "今天";
        } else if (daysLeft == 1) {
            daysText = "明天";
        } else if (daysLeft == 2) {
            daysText = "後天";
        } else {
            daysText = daysLeft + " 天後";
        }

        String title = item.name + " - " + daysText + "扣款";

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateText = format.format(new Date(item.nextDateMillis));
        String priceText = item.price >= 0 ? String.valueOf(item.price) : "?";
        String currencyText = (item.currency != null && !item.currency.isEmpty()) ? item.currency : "TWD";
        String content = dateText + " 扣款 " + priceText + " " + currencyText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        int notificationId = item.id.hashCode();
        manager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "訂閱到期提醒";
            String description = "顯示最近到期的訂閱通知";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
