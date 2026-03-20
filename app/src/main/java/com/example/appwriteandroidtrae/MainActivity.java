package com.example.appwriteandroidtrae;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "subscription_expiry_channel";
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_bank_stats) {
                startActivity(new Intent(MainActivity.this, BankStatsActivity.class));
                return true;
            }
            return false;
        });

        View cardSubscription = findViewById(R.id.cardSubscription);
        View cardOilMonitor = findViewById(R.id.cardOilMonitor);
        View cardBankStats = findViewById(R.id.cardBankStats);
        View cardFoodManagement = findViewById(R.id.cardFoodManagement);
        View cardFengNotes = findViewById(R.id.cardFengNotes);
        View cardFengCommon = findViewById(R.id.cardFengCommon);

        cardSubscription.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SubscriptionActivity.class)));

        cardOilMonitor.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, OilMonitorActivity.class)));

        cardBankStats.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BankStatsActivity.class)));

        cardFoodManagement.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FoodManagementActivity.class)));

        cardFengNotes.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FengNotesActivity.class)));

        cardFengCommon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FengCommonActivity.class)));

        OilPriceScheduler.enqueueImmediateFetch(getApplicationContext());
        OilPriceScheduler.scheduleDailyFetch(getApplicationContext());

        createNotificationChannel();
        ensureNotificationPermission();
        scheduleDailySubscriptionCheck();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS
                );
            }
        }
    }

    private void scheduleDailySubscriptionCheck() {
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

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "subscription_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
