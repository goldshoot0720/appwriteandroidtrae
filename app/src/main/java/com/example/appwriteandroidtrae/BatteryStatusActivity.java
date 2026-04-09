package com.example.appwriteandroidtrae;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BatteryStatusActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "battery_prefs";
    private static final String KEY_LAST_FULL_TIME = "last_full_time";
    private static final int BATTERY_PROPERTY_CHARGE_TIME_REMAINING = 5;

    private TextView textLastFullTime;
    private TextView textLastFullDelta;
    private TextView textCurrentLevel;
    private TextView textEstimatedChargeTime;
    private TextView textEstimatedChargeDelta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_battery_status);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_battery);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textLastFullTime = findViewById(R.id.textLastFullTime);
        textLastFullDelta = findViewById(R.id.textLastFullDelta);
        textCurrentLevel = findViewById(R.id.textCurrentLevel);
        textEstimatedChargeTime = findViewById(R.id.textEstimatedChargeTime);
        textEstimatedChargeDelta = findViewById(R.id.textEstimatedChargeDelta);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBatteryInfo();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshBatteryInfo() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, filter);
        if (intent == null) {
            bindUnknown();
            return;
        }

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        int percent = (level >= 0 && scale > 0) ? Math.round(level * 100f / scale) : -1;

        if (percent >= 100 && (isFull || isCharging)) {
            saveLastFullTime(System.currentTimeMillis());
        }

        long lastFullTime = getLastFullTime();
        textLastFullTime.setText(formatDateTime(lastFullTime));
        textLastFullDelta.setText(formatDurationDelta(lastFullTime, System.currentTimeMillis()));
        textCurrentLevel.setText(percent >= 0
                ? getString(R.string.battery_value_percent, percent)
                : getString(R.string.battery_value_unknown));

        BatteryManager manager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        long remainingMs = manager != null
                ? manager.getLongProperty(BATTERY_PROPERTY_CHARGE_TIME_REMAINING)
                : -1L;

        if (remainingMs > 0 && isCharging) {
            long estimatedFullAt = System.currentTimeMillis() + remainingMs;
            textEstimatedChargeTime.setText(formatDateTime(estimatedFullAt));
            textEstimatedChargeDelta.setText(formatDurationDelta(System.currentTimeMillis(), estimatedFullAt));
        } else {
            textEstimatedChargeTime.setText(getString(R.string.battery_value_unknown));
            textEstimatedChargeDelta.setText(getString(R.string.battery_value_unknown));
        }
    }

    private void bindUnknown() {
        textLastFullTime.setText(getString(R.string.battery_value_unknown));
        textLastFullDelta.setText(getString(R.string.battery_value_unknown));
        textCurrentLevel.setText(getString(R.string.battery_value_unknown));
        textEstimatedChargeTime.setText(getString(R.string.battery_value_unknown));
        textEstimatedChargeDelta.setText(getString(R.string.battery_value_unknown));
    }

    private void saveLastFullTime(long timeMillis) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_FULL_TIME, timeMillis)
                .apply();
    }

    private long getLastFullTime() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getLong(KEY_LAST_FULL_TIME, -1L);
    }

    private String formatDateTime(long timeMillis) {
        if (timeMillis <= 0L) {
            return getString(R.string.battery_value_unknown);
        }
        java.text.DateFormat formatter = DateFormat.getMediumDateFormat(this);
        java.text.DateFormat timeFormatter = DateFormat.getTimeFormat(this);
        Date date = new Date(timeMillis);
        return formatter.format(date) + " " + timeFormatter.format(date);
    }

    private String formatDurationDelta(long startMillis, long endMillis) {
        if (startMillis <= 0L || endMillis <= 0L) {
            return getString(R.string.battery_value_unknown);
        }
        long diff = Math.abs(endMillis - startMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        if (days > 0) {
            return getString(R.string.battery_value_duration_days, days, hours, minutes);
        }
        if (hours > 0) {
            return getString(R.string.battery_value_duration_hours, hours, minutes);
        }
        return getString(R.string.battery_value_duration_minutes, minutes);
    }
}
