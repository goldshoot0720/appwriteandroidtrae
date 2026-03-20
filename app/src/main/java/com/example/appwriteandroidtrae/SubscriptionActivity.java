package com.example.appwriteandroidtrae;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SubscriptionActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "subscription_expiry_channel";
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private ArrayAdapter<AppwriteHelper.SubscriptionItem> adapter;
    private final List<AppwriteHelper.SubscriptionItem> subscriptionItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("訂閱管理");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewSubscriptions);
        textViewError = findViewById(R.id.textViewError);

        adapter = new SubscriptionAdapter(this, subscriptionItems);
        listView.setAdapter(adapter);

        // We create the channel here too just in case, though MainActivity does it.
        createNotificationChannel();
        ensureNotificationPermission();
        
        loadSubscriptions();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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
                            subscriptionItems.clear();
                            subscriptionItems.addAll(result);
                            adapter.notifyDataSetChanged();
                            checkExpiringSubscriptions(result);
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            textViewError.setVisibility(View.VISIBLE);
                            textViewError.setText("載入失敗: " + error.getMessage());
                        });
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "訂閱到期提醒";
            String description = "顯示最近到期的訂閱通知";
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

    private void checkExpiringSubscriptions(List<AppwriteHelper.SubscriptionItem> items) {
        long now = System.currentTimeMillis();
        long threeDaysMillis = 3L * 24L * 60L * 60L * 1000L;
        StringBuilder alertMessage = new StringBuilder();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

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

                // 組合 App 內通知訊息
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
                String priceText = item.price >= 0 ? String.valueOf(item.price) : "?";
                String currencyText = (item.currency != null && !item.currency.isEmpty()) ? item.currency : "TWD";
                alertMessage.append("• ").append(item.name)
                        .append(" - ").append(daysText).append("扣款")
                        .append(" (").append(priceText).append(" ").append(currencyText).append(")")
                        .append("\n  ").append(fmt.format(new Date(item.nextDateMillis)))
                        .append("\n\n");
            }
        }

        // 顯示 App 內彈窗通知
        if (alertMessage.length() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("訂閱即將到期提醒")
                    .setMessage(alertMessage.toString().trim())
                    .setPositiveButton("確定", null)
                    .show();
        }
    }

    private void showExpiryNotification(AppwriteHelper.SubscriptionItem item, long daysLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        int notificationId = item.id.hashCode();
        manager.notify(notificationId, builder.build());
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
            TextView textNote = convertView.findViewById(R.id.textNote);

            if (item != null) {
                String title = item.name != null ? item.name : "";
                textTitle.setText(title);

                if (item.site != null && !item.site.isEmpty()) {
                    textSubtitle.setVisibility(View.VISIBLE);
                    textSubtitle.setText(item.site);
                } else {
                    textSubtitle.setVisibility(View.GONE);
                }

                if (item.price >= 0) {
                    textPrice.setVisibility(View.VISIBLE);
                    textPrice.setText("價格: " + item.price);
                } else {
                    textPrice.setVisibility(View.GONE);
                }

                if (item.account != null && !item.account.isEmpty()) {
                    textAccount.setVisibility(View.VISIBLE);
                    textAccount.setText("帳號: " + item.account);
                } else {
                    textAccount.setVisibility(View.GONE);
                }

                if (item.nextDateMillis > 0L) {
                    textNextDate.setVisibility(View.VISIBLE);
                    String dateText = dateFormat.format(new Date(item.nextDateMillis));
                    textNextDate.setText("下次扣款日: " + dateText);
                } else {
                    textNextDate.setVisibility(View.GONE);
                }

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
