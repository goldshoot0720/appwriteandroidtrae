package com.example.appwriteandroidtrae;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubscriptionActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "subscription_expiry_channel";
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;
    private static final String PREFS = "subscription_prefs";
    private static final String PREF_RECENT_SEARCHES = "subscription_recent_searches";
    private static final int MAX_RECENT_SEARCHES = 8;
    private static final Pattern VOICE_ADD_PATTERN = Pattern.compile(
            ".*(?:新增|加入|建立).*?(?:叫做|名稱(?:是|為)?|名叫)(.+?)(?:，|,|\\s)*(?:日期(?:是|為)?|到期(?:日|日期)?(?:是|為)?)(.+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CHINESE_DATE_PATTERN = Pattern.compile(
            "(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*(?:日|號)?\\s*(上午|早上|下午|晚上)?\\s*(\\d{1,2})?",
            Pattern.CASE_INSENSITIVE
    );

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private TextView textRecentSearches;
    private ListView listRecentSearches;
    private TextInputEditText editTextSearchSubscriptions;
    private VoiceInputHelper voiceInputHelper;
    private ArrayAdapter<AppwriteHelper.SubscriptionItem> adapter;
    private ArrayAdapter<String> recentSearchAdapter;
    private final Handler searchHistoryHandler = new Handler(Looper.getMainLooper());
    private final List<AppwriteHelper.SubscriptionItem> allSubscriptionItems = new ArrayList<>();
    private final List<AppwriteHelper.SubscriptionItem> filteredSubscriptionItems = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();
    private final Runnable rememberSearchRunnable = () -> rememberSearch(getSearchText());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_subscription);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewSubscriptions);
        textViewError = findViewById(R.id.textViewError);
        textRecentSearches = findViewById(R.id.textRecentSearches);
        listRecentSearches = findViewById(R.id.listRecentSearches);
        editTextSearchSubscriptions = findViewById(R.id.editTextSearchSubscriptions);
        TextInputLayout inputLayoutSearchSubscriptions = findViewById(R.id.inputLayoutSearchSubscriptions);
        voiceInputHelper = new VoiceInputHelper(this);
        inputLayoutSearchSubscriptions.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        inputLayoutSearchSubscriptions.setEndIconDrawable(android.R.drawable.ic_btn_speak_now);
        inputLayoutSearchSubscriptions.setEndIconContentDescription(getString(R.string.voice_input));
        inputLayoutSearchSubscriptions.setEndIconOnClickListener(v ->
                voiceInputHelper.start(getString(R.string.voice_prompt_subscription), this::handleSubscriptionVoiceText));

        adapter = new SubscriptionAdapter(this, filteredSubscriptionItems);
        listView.setAdapter(adapter);
        setupRecentSearches();

        editTextSearchSubscriptions.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSubscriptions(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleRememberSearch();
            }
        });
        editTextSearchSubscriptions.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                rememberSearch(getSearchText());
            }
            return false;
        });

        createNotificationChannel();
        ensureNotificationPermission();
        loadSubscriptions();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        searchHistoryHandler.removeCallbacks(rememberSearchRunnable);
        super.onDestroy();
    }

    private void loadSubscriptions() {
        progressBar.setVisibility(View.VISIBLE);
        textViewError.setVisibility(View.GONE);

        AppwriteHelper.getInstance(getApplicationContext())
                .listSubscriptions(new AppwriteHelper.DataCallback<List<AppwriteHelper.SubscriptionItem>>() {
                    @Override
                    public void onSuccess(List<AppwriteHelper.SubscriptionItem> result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            allSubscriptionItems.clear();
                            allSubscriptionItems.addAll(result);
                            filterSubscriptions(editTextSearchSubscriptions.getText() != null
                                    ? editTextSearchSubscriptions.getText().toString()
                                    : "");
                            checkExpiringSubscriptions(result);
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            textViewError.setVisibility(View.VISIBLE);
                            textViewError.setText(getReadableError(error));
                        });
                    }
                });
    }

    private String getReadableError(Exception error) {
        if (error == null || error.getMessage() == null) {
            return getString(R.string.generic_load_error, "");
        }
        String message = error.getMessage();
        if (message.contains("401") || message.contains("missing scopes") || message.contains("missing scope")) {
            return getString(R.string.error_unauthorized);
        }
        return getString(R.string.generic_load_error, message);
    }

    private void filterSubscriptions(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        filteredSubscriptionItems.clear();

        if (normalizedQuery.isEmpty()) {
            filteredSubscriptionItems.addAll(allSubscriptionItems);
        } else {
            for (AppwriteHelper.SubscriptionItem item : allSubscriptionItems) {
                String title = item.name != null ? item.name.toLowerCase(Locale.getDefault()) : "";
                String note = item.note != null ? item.note.toLowerCase(Locale.getDefault()) : "";
                String site = item.site != null ? item.site.toLowerCase(Locale.getDefault()) : "";
                if (title.contains(normalizedQuery)
                        || note.contains(normalizedQuery)
                        || site.contains(normalizedQuery)) {
                    filteredSubscriptionItems.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void setupRecentSearches() {
        recentSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recentSearches);
        listRecentSearches.setAdapter(recentSearchAdapter);
        listRecentSearches.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < recentSearches.size()) {
                editTextSearchSubscriptions.setText(recentSearches.get(position));
                editTextSearchSubscriptions.setSelection(editTextSearchSubscriptions.length());
            }
        });
        loadRecentSearches();
    }

    private void loadRecentSearches() {
        recentSearches.clear();
        String stored = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_RECENT_SEARCHES, "");
        if (stored != null && !stored.isEmpty()) {
            Collections.addAll(recentSearches, stored.split("\\n"));
        }
        updateRecentSearches();
    }

    private void scheduleRememberSearch() {
        searchHistoryHandler.removeCallbacks(rememberSearchRunnable);
        searchHistoryHandler.postDelayed(rememberSearchRunnable, 900L);
    }

    private void rememberSearch(String query) {
        String normalized = query.trim();
        if (normalized.isEmpty()) {
            return;
        }
        recentSearches.remove(normalized);
        recentSearches.add(0, normalized);
        if (recentSearches.size() > MAX_RECENT_SEARCHES) {
            recentSearches.subList(MAX_RECENT_SEARCHES, recentSearches.size()).clear();
        }
        persistRecentSearches();
        updateRecentSearches();
    }

    private void persistRecentSearches() {
        StringBuilder builder = new StringBuilder();
        for (String item : recentSearches) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(item.replace("\n", " ").trim());
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(PREF_RECENT_SEARCHES, builder.toString())
                .apply();
    }

    private void updateRecentSearches() {
        boolean hasRecentSearches = !recentSearches.isEmpty();
        textRecentSearches.setVisibility(View.VISIBLE);
        listRecentSearches.setVisibility(hasRecentSearches ? View.VISIBLE : View.GONE);
        recentSearchAdapter.notifyDataSetChanged();
    }

    private String getSearchText() {
        return editTextSearchSubscriptions.getText() != null
                ? editTextSearchSubscriptions.getText().toString()
                : "";
    }

    private void handleSubscriptionVoiceText(String text) {
        VoiceSubscriptionCommand command = parseVoiceSubscriptionCommand(text);
        if (command == null) {
            editTextSearchSubscriptions.setText(text);
            editTextSearchSubscriptions.setSelection(editTextSearchSubscriptions.length());
            return;
        }
        confirmCreateSubscription(command);
    }

    private VoiceSubscriptionCommand parseVoiceSubscriptionCommand(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = VOICE_ADD_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            return null;
        }
        String name = cleanVoiceName(matcher.group(1));
        Long dateMillis = parseVoiceDate(matcher.group(2));
        if (name.isEmpty() || dateMillis == null) {
            return null;
        }
        return new VoiceSubscriptionCommand(name, dateMillis);
    }

    private String cleanVoiceName(String value) {
        return value == null
                ? ""
                : value.replace("資料", "")
                        .replace("一筆", "")
                        .trim();
    }

    private Long parseVoiceDate(String value) {
        Matcher matcher = CHINESE_DATE_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return null;
        }
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        String dayPart = matcher.group(4);
        String hourText = matcher.group(5);
        int hour;
        if (hourText != null && !hourText.isEmpty()) {
            hour = Integer.parseInt(hourText);
            if (dayPart != null && (dayPart.contains("下午") || dayPart.contains("晚上")) && hour < 12) {
                hour += 12;
            }
        } else if (dayPart != null && (dayPart.contains("下午") || dayPart.contains("晚上"))) {
            hour = 15;
        } else {
            hour = 9;
        }
        return LocalDateTime.of(year, month, day, hour, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private void confirmCreateSubscription(VoiceSubscriptionCommand command) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        new AlertDialog.Builder(this)
                .setTitle(R.string.subscription_voice_add_confirm_title)
                .setMessage(getString(
                        R.string.subscription_voice_add_confirm_message,
                        command.name,
                        format.format(new Date(command.nextDateMillis))
                ))
                .setPositiveButton(R.string.voice_confirm_apply, (dialog, which) -> createSubscription(command))
                .setNegativeButton(R.string.ui_close, null)
                .show();
    }

    private void createSubscription(VoiceSubscriptionCommand command) {
        progressBar.setVisibility(View.VISIBLE);
        AppwriteHelper.getInstance(getApplicationContext())
                .createSubscription(command.name, command.nextDateMillis, new AppwriteHelper.DataCallback<AppwriteHelper.SubscriptionItem>() {
                    @Override
                    public void onSuccess(AppwriteHelper.SubscriptionItem result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            loadSubscriptions();
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            textViewError.setVisibility(View.VISIBLE);
                            textViewError.setText(getReadableError(error));
                        });
                    }
                });
    }

    private static class VoiceSubscriptionCommand {
        final String name;
        final long nextDateMillis;

        VoiceSubscriptionCommand(String name, long nextDateMillis) {
            this.name = name;
            this.nextDateMillis = nextDateMillis;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS
            );
        }
    }

    private void checkExpiringSubscriptions(List<AppwriteHelper.SubscriptionItem> items) {
        long now = System.currentTimeMillis();
        long threeDaysMillis = 3L * 24L * 60L * 60L * 1000L;
        StringBuilder alertMessage = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (AppwriteHelper.SubscriptionItem item : items) {
            if (item.nextDateMillis <= 0L || !item.continueFlag) {
                continue;
            }
            if (item.nextDateMillis >= now && item.nextDateMillis <= now + threeDaysMillis) {
                long daysLeft = TimeUnit.MILLISECONDS.toDays(item.nextDateMillis - now);
                showExpiryNotification(item, daysLeft);

                String currencyText = item.currency != null && !item.currency.isEmpty() ? item.currency : "TWD";
                if (alertMessage.length() > 0) {
                    alertMessage.append("\n\n");
                }
                alertMessage.append(getString(
                        R.string.subscription_alert_item,
                        item.name,
                        getDueText(daysLeft),
                        item.price >= 0 ? String.valueOf(item.price) : "?",
                        currencyText,
                        format.format(new Date(item.nextDateMillis))
                ));
            }
        }

        if (alertMessage.length() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.subscription_alert_title)
                    .setMessage(alertMessage.toString())
                    .setPositiveButton(R.string.ui_close, null)
                    .show();
        }
    }

    private void showExpiryNotification(AppwriteHelper.SubscriptionItem item, long daysLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String title = getString(R.string.subscription_title_format, item.name, getDueText(daysLeft));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateText = format.format(new Date(item.nextDateMillis));
        String priceText = item.price >= 0 ? String.valueOf(item.price) : "?";
        String currencyText = item.currency != null && !item.currency.isEmpty() ? item.currency : "TWD";
        String content = getString(R.string.subscription_notification_content, dateText, priceText, currencyText);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(item.id.hashCode(), builder.build());
    }

    private String getDueText(long daysLeft) {
        if (daysLeft <= 0) {
            return getString(R.string.ui_today_due);
        }
        if (daysLeft == 1) {
            return getString(R.string.ui_due_tomorrow);
        }
        if (daysLeft == 2) {
            return getString(R.string.ui_due_in_two_days);
        }
        return getString(R.string.ui_due_in_days, daysLeft);
    }

    private static class SubscriptionAdapter extends ArrayAdapter<AppwriteHelper.SubscriptionItem> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        SubscriptionAdapter(android.content.Context context, List<AppwriteHelper.SubscriptionItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext()).inflate(R.layout.item_subscription, parent, false);
            }
            AppwriteHelper.SubscriptionItem item = getItem(position);
            TextView textTitle = convertView.findViewById(R.id.textTitle);
            TextView textSubtitle = convertView.findViewById(R.id.textSubtitle);
            TextView textPrice = convertView.findViewById(R.id.textPrice);
            TextView textAccount = convertView.findViewById(R.id.textAccount);
            TextView textNextDate = convertView.findViewById(R.id.textNextDate);
            TextView textNextDateDelta = convertView.findViewById(R.id.textNextDateDelta);
            TextView textCreatedDate = convertView.findViewById(R.id.textCreatedDate);
            TextView textNote = convertView.findViewById(R.id.textNote);

            if (item != null) {
                textTitle.setText(item.name != null ? item.name : "");

                if (item.site != null && !item.site.isEmpty()) {
                    textSubtitle.setVisibility(View.VISIBLE);
                    textSubtitle.setText(item.site);
                } else {
                    textSubtitle.setVisibility(View.GONE);
                }

                if (item.price >= 0) {
                    textPrice.setVisibility(View.VISIBLE);
                    textPrice.setText(getContext().getString(
                            R.string.value_label_format,
                            getContext().getString(R.string.subscription_price_label),
                            String.valueOf(item.price)
                    ));
                } else {
                    textPrice.setVisibility(View.GONE);
                }

                if (item.account != null && !item.account.isEmpty()) {
                    textAccount.setVisibility(View.VISIBLE);
                    textAccount.setText(getContext().getString(
                            R.string.value_label_format,
                            getContext().getString(R.string.subscription_account_label),
                            item.account
                    ));
                } else {
                    textAccount.setVisibility(View.GONE);
                }

                if (item.nextDateMillis > 0L) {
                    textNextDate.setVisibility(View.VISIBLE);
                    textNextDate.setText(getContext().getString(
                            R.string.date_label_format,
                            getContext().getString(R.string.subscription_next_date_label),
                            dateFormat.format(new Date(item.nextDateMillis))
                    ));
                    long daysDelta = TimeUnit.MILLISECONDS.toDays(item.nextDateMillis - System.currentTimeMillis());
                    if (daysDelta >= 0) {
                        textNextDateDelta.setText(getContext().getString(
                                R.string.subscription_days_until,
                                daysDelta
                        ));
                    } else {
                        textNextDateDelta.setText(getContext().getString(
                                R.string.subscription_days_overdue,
                                Math.abs(daysDelta)
                        ));
                    }
                    textNextDateDelta.setVisibility(View.VISIBLE);
                } else {
                    textNextDate.setVisibility(View.GONE);
                    textNextDateDelta.setVisibility(View.GONE);
                }

                textCreatedDate.setVisibility(View.GONE);

                if (item.note != null && !item.note.isEmpty()) {
                    textNote.setVisibility(View.VISIBLE);
                    textNote.setText(item.note);
                } else {
                    textNote.setVisibility(View.GONE);
                }
            }

            return convertView;
        }
    }
}
