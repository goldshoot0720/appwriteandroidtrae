package com.example.appwriteandroidtrae;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.View;
import android.widget.TextView;

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
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "subscription_expiry_channel";
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;
    private static final String SLEEP_PREFS = "sleep_hint_prefs";
    private static final String SLEEP_PREF_DATE = "sleep_hint_date";
    private static final String SLEEP_PREF_COUNT = "sleep_hint_count";
    private static final int[] SLEEP_HINT_MINUTES = new int[]{
            0, 30, 60, 90, 120, 135, 150, 165, 180, 195, 210, 225, 240
    };

    private final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;
    private View cardSleepHint;
    private TextView textSleepHint;
    private TextView textCodeLineCount;
    private static final int CODE_LINE_COUNT = 6076;

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
            if (item.getItemId() == R.id.action_lottery_reason) {
                startActivity(new Intent(MainActivity.this, LotteryReasonActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_battery_status) {
                startActivity(new Intent(MainActivity.this, BatteryStatusActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_feng_tools) {
                startActivity(new Intent(MainActivity.this, FengToolsActivity.class));
                return true;
            }
            return false;
        });

        View cardSubscription = findViewById(R.id.cardSubscription);
        View cardOilMonitor = findViewById(R.id.cardOilMonitor);
        View cardUsDebt = findViewById(R.id.cardUsDebt);
        View cardPriceCompare = findViewById(R.id.cardPriceCompare);
        View cardBatteryStatus = findViewById(R.id.cardBatteryStatus);
        View cardBankStats = findViewById(R.id.cardBankStats);
        View cardFoodManagement = findViewById(R.id.cardFoodManagement);
        View cardFengNotes = findViewById(R.id.cardFengNotes);
        View cardFengCommon = findViewById(R.id.cardFengCommon);
        View cardLotteryReason = findViewById(R.id.cardLotteryReason);
        cardSleepHint = findViewById(R.id.cardSleepHint);
        textSleepHint = findViewById(R.id.textSleepHint);
        textCodeLineCount = findViewById(R.id.textCodeLineCount);
        View birthdayEasterEgg = findViewById(R.id.cardBirthdayEasterEgg);
        TextView textBirthdayTitle = findViewById(R.id.textBirthdayTitle);
        TextView textBirthdaySubtitle = findViewById(R.id.textBirthdaySubtitle);

        cardSubscription.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SubscriptionActivity.class)));

        cardOilMonitor.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, OilMonitorActivity.class)));

        cardUsDebt.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, USDebtActivity.class)));

        cardPriceCompare.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FengToolsActivity.class)));

        cardBatteryStatus.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BatteryStatusActivity.class)));

        cardBankStats.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BankStatsActivity.class)));

        cardFoodManagement.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FoodManagementActivity.class)));

        cardFengNotes.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FengNotesActivity.class)));

        cardFengCommon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FengCommonActivity.class)));

        cardLotteryReason.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LotteryReasonActivity.class)));

        setupBirthdayEasterEgg(birthdayEasterEgg, textBirthdayTitle, textBirthdaySubtitle);
        startSleepHintScheduler();
        if (textCodeLineCount != null) {
            textCodeLineCount.setText(getString(R.string.code_line_count_label, CODE_LINE_COUNT));
        }

        OilPriceScheduler.enqueueImmediateFetch(getApplicationContext());
        OilPriceScheduler.scheduleDailyFetch(getApplicationContext());

        createNotificationChannel();
        ensureNotificationPermission();
        scheduleDailySubscriptionCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSleepHintScheduler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSleepHintScheduler();
    }

    private void startSleepHintScheduler() {
        if (cardSleepHint == null || textSleepHint == null) {
            return;
        }
        stopSleepHintScheduler();
        ensureSleepHintDate();
        scheduleNextSleepHint(true);
    }

    private void stopSleepHintScheduler() {
        if (sleepRunnable != null) {
            sleepHandler.removeCallbacks(sleepRunnable);
        }
    }

    private void scheduleNextSleepHint(boolean allowImmediate) {
        Calendar now = Calendar.getInstance();
        int minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int nextSlot = findNextSleepSlot(minuteOfDay);
        if (nextSlot < 0) {
            cardSleepHint.setVisibility(View.GONE);
            return;
        }

        cardSleepHint.setVisibility(View.VISIBLE);
        long triggerAt = computeSlotTime(now, nextSlot);
        long nowMillis = System.currentTimeMillis();
        long delay = triggerAt - nowMillis;
        if (delay < 0 && allowImmediate) {
            delay = 0;
        }
        if (delay < 0) {
            delay = 0;
        }

        sleepRunnable = () -> {
            showSleepHint();
            scheduleNextSleepHint(false);
        };
        sleepHandler.postDelayed(sleepRunnable, delay);
    }

    private int findNextSleepSlot(int minuteOfDay) {
        for (int slot : SLEEP_HINT_MINUTES) {
            if (minuteOfDay <= slot) {
                return slot;
            }
        }
        return -1;
    }

    private long computeSlotTime(Calendar base, int minuteOfDay) {
        Calendar slot = (Calendar) base.clone();
        slot.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        slot.set(Calendar.MINUTE, minuteOfDay % 60);
        slot.set(Calendar.SECOND, 0);
        slot.set(Calendar.MILLISECOND, 0);
        return slot.getTimeInMillis();
    }

    private void ensureSleepHintDate() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());
        String savedDate = getSharedPreferences(SLEEP_PREFS, MODE_PRIVATE)
                .getString(SLEEP_PREF_DATE, "");
        if (!today.equals(savedDate)) {
            getSharedPreferences(SLEEP_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(SLEEP_PREF_DATE, today)
                    .putInt(SLEEP_PREF_COUNT, 0)
                    .apply();
        }
    }

    private void showSleepHint() {
        ensureSleepHintDate();
        int count = getSharedPreferences(SLEEP_PREFS, MODE_PRIVATE)
                .getInt(SLEEP_PREF_COUNT, 0) + 1;
        getSharedPreferences(SLEEP_PREFS, MODE_PRIVATE)
                .edit()
                .putInt(SLEEP_PREF_COUNT, count)
                .apply();

        String nowText = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new java.util.Date());
        textSleepHint.setText(nowText + " 提示第 " + count + " 次");
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

    private void setupBirthdayEasterEgg(View card, TextView title, TextView subtitle) {
        Calendar today = Calendar.getInstance();
        boolean isAprilThird = today.get(Calendar.MONTH) == Calendar.APRIL
                && today.get(Calendar.DAY_OF_MONTH) == 3;
        boolean isNovemberTwentySeventh = today.get(Calendar.MONTH) == Calendar.NOVEMBER
                && today.get(Calendar.DAY_OF_MONTH) == 27;

        if (!isAprilThird && !isNovemberTwentySeventh) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        if (isAprilThird) {
            title.setText(getString(R.string.birthday_easter_egg_title_tuge));
            subtitle.setText(getString(
                    R.string.birthday_easter_egg_subtitle_tuge,
                    String.format(Locale.TAIWAN, "%d/%d", today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH))
            ));
        } else {
            title.setText(getString(R.string.birthday_easter_egg_title_feng));
            subtitle.setText(getString(
                    R.string.birthday_easter_egg_subtitle_feng,
                    String.format(Locale.TAIWAN, "%d/%d", today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH))
            ));
        }

        card.setAlpha(0f);
        card.setTranslationY(36f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(900L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        title.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(900L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> title.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(900L)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> setupBirthdayEasterEggLoop(title, subtitle))
                        .start())
                .start();
    }

    private void setupBirthdayEasterEggLoop(TextView title, TextView subtitle) {
        title.animate()
                .rotation(-2f)
                .translationY(-6f)
                .setDuration(1100L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> title.animate()
                        .rotation(2f)
                        .translationY(0f)
                        .setDuration(1100L)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> setupBirthdayEasterEggLoop(title, subtitle))
                        .start())
                .start();

        subtitle.animate()
                .alpha(0.72f)
                .setDuration(1100L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> subtitle.animate()
                        .alpha(1f)
                        .setDuration(1100L)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start())
                .start();
    }
}
