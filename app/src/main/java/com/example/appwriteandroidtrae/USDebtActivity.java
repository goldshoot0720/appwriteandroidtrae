package com.example.appwriteandroidtrae;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class USDebtActivity extends AppCompatActivity {

    private static final String SOURCE_URL = "https://www.usdebtclock.org/";
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\$[0-9,]+");
    private static final long LOAD_TIMEOUT_MS = 15000L;

    private final SimpleDateFormat fetchTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ProgressBar progressBar;
    private TextView textLatestDebt;
    private TextView textDebtSource;
    private TextView textLastFetched;
    private TextView textDebtError;
    private USDebtChartView chartView;
    private WebView debtWebView;
    private boolean loadCompleted;
    private final Runnable timeoutRunnable = () -> {
        if (!loadCompleted) {
            showError("讀取 usdebtclock.org 超時");
            if (debtWebView != null) {
                debtWebView.stopLoading();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_us_debt);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_us_debt);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBarDebt);
        textLatestDebt = findViewById(R.id.textLatestDebt);
        textDebtSource = findViewById(R.id.textDebtSource);
        textLastFetched = findViewById(R.id.textDebtLastFetched);
        textDebtError = findViewById(R.id.textDebtError);
        chartView = findViewById(R.id.usDebtChart);
        debtWebView = findViewById(R.id.webViewDebt);

        setupWebView();

        findViewById(R.id.buttonRefreshDebt).setOnClickListener(v -> refreshDebt());

        renderHistory(new USDebtRepository(getApplicationContext()).getHistory());
        refreshDebt();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = debtWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(false);
        settings.setBlockNetworkImage(true);
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Mobile Safari/537.36"
        );
        debtWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadCompleted = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> readDebtValue(view), 1200);
            }

            @Override
            public void onReceivedError(
                    @NonNull WebView view,
                    @NonNull WebResourceRequest request,
                    @NonNull WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    showError(String.valueOf(error.getDescription()));
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshDebt() {
        loadCompleted = false;
        progressBar.setVisibility(View.VISIBLE);
        textDebtError.setVisibility(View.GONE);
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS);
        debtWebView.stopLoading();
        debtWebView.loadUrl(SOURCE_URL);
    }

    private void readDebtValue(WebView view) {
        String script = "(function() {"
                + "var primary = document.getElementById('layer29');"
                + "if (primary && primary.innerText) return primary.innerText;"
                + "var first = Array.from(document.querySelectorAll('div,span')).map(function(el){return (el.innerText||'').trim();}).find(function(text){return /^\\$[0-9,]+$/.test(text);});"
                + "return first || '';"
                + "})();";

        view.evaluateJavascript(script, value -> {
            try {
                String raw = normalizeJavascriptResult(value);
                Matcher matcher = MONEY_PATTERN.matcher(raw);
                if (!matcher.find()) {
                    throw new Exception("找不到 U.S. National Debt 數值");
                }

                long debtValue = Long.parseLong(matcher.group().replace("$", "").replace(",", ""));
                USDebtPoint point = new USDebtPoint(debtValue, System.currentTimeMillis());
                USDebtRepository repository = new USDebtRepository(getApplicationContext());
                repository.savePoint(point);
                renderHistory(repository.getHistory());
                loadCompleted = true;
                handler.removeCallbacks(timeoutRunnable);
                progressBar.setVisibility(View.GONE);
            } catch (Exception error) {
                showError(error.getMessage());
            }
        });
    }

    private String normalizeJavascriptResult(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        String normalized = value;
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replace("\\n", " ")
                .replace("\\u003C", "<")
                .replace("\\\"", "\"")
                .trim();
    }

    private void showError(String message) {
        loadCompleted = true;
        handler.removeCallbacks(timeoutRunnable);
        progressBar.setVisibility(View.GONE);
        renderHistory(new USDebtRepository(getApplicationContext()).getHistory());
        textDebtError.setVisibility(View.VISIBLE);
        textDebtError.setText(getString(R.string.us_debt_fetch_error, message));
    }

    private void renderHistory(List<USDebtPoint> history) {
        List<USDebtPoint> chartData = history.size() > 30
                ? new ArrayList<>(history.subList(history.size() - 30, history.size()))
                : new ArrayList<>(history);
        chartView.setPoints(chartData);

        textDebtSource.setText(R.string.us_debt_source);

        if (history.isEmpty()) {
            textLatestDebt.setText(R.string.us_debt_latest_empty);
            textLastFetched.setText(R.string.us_debt_last_fetched_empty);
            return;
        }

        USDebtPoint latest = history.get(history.size() - 1);
        textLatestDebt.setText(getString(
                R.string.us_debt_latest_value,
                currencyFormat.format(latest.debtValue)
        ));
        textLastFetched.setText(getString(
                R.string.us_debt_last_fetched,
                fetchTimeFormat.format(new Date(latest.fetchedAtMillis))
        ));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timeoutRunnable);
        if (debtWebView != null) {
            debtWebView.stopLoading();
            debtWebView.destroy();
        }
        super.onDestroy();
    }
}
