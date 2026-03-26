package com.example.appwriteandroidtrae;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    private final SimpleDateFormat fetchTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    private ProgressBar progressBar;
    private TextView textLatestDebt;
    private TextView textDebtSource;
    private TextView textLastFetched;
    private TextView textDebtError;
    private USDebtChartView chartView;
    private WebView debtWebView;

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

        setupWebView();

        findViewById(R.id.buttonRefreshDebt).setOnClickListener(v -> refreshDebt());

        renderHistory(new USDebtRepository(getApplicationContext()).getHistory());
        refreshDebt();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        debtWebView = new WebView(this);
        WebSettings settings = debtWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Mobile Safari/537.36"
        );
        debtWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> readDebtValue(view), 1200);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshDebt() {
        progressBar.setVisibility(View.VISIBLE);
        textDebtError.setVisibility(View.GONE);
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

        view.evaluateJavascript(script, (ValueCallback<String>) value -> {
            try {
                String raw = normalizeJavascriptResult(value);
                Matcher matcher = MONEY_PATTERN.matcher(raw);
                if (!matcher.find()) {
                    throw new Exception("Unable to locate U.S. National Debt on usdebtclock.org.");
                }

                long debtValue = Long.parseLong(matcher.group().replace("$", "").replace(",", ""));
                USDebtPoint point = new USDebtPoint(debtValue, System.currentTimeMillis());
                USDebtRepository repository = new USDebtRepository(getApplicationContext());
                repository.savePoint(point);
                renderHistory(repository.getHistory());
                progressBar.setVisibility(View.GONE);
            } catch (Exception error) {
                progressBar.setVisibility(View.GONE);
                renderHistory(new USDebtRepository(getApplicationContext()).getHistory());
                textDebtError.setVisibility(View.VISIBLE);
                textDebtError.setText(getString(R.string.us_debt_fetch_error, error.getMessage()));
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
        return normalized.replace("\\n", " ").replace("\\u003C", "<").replace("\\\"", "\"").trim();
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
        if (debtWebView != null) {
            debtWebView.stopLoading();
            debtWebView.destroy();
        }
        super.onDestroy();
    }
}
