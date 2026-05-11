package com.example.appwriteandroidtrae;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FengFinanceActivity extends AppCompatActivity {

    private static final long LOAD_TIMEOUT_MS = 18000L;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d[\\d,]*(?:\\.\\d+)?%?");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> rows = new ArrayList<>();
    private final List<MarketQuote> quotes = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView textStatus;
    private Button buttonRefresh;
    private WebView webView;
    private int loadingIndex;
    private int loadedCount;
    private boolean pageFinished;

    private final Runnable timeoutRunnable = () -> {
        if (!pageFinished) {
            addErrorQuote(getCurrentItem(), getString(R.string.feng_finance_error_timeout));
            loadNextQuote();
        }
    };

    private static final FinanceItem[] ITEMS = {
            new FinanceItem("加權指數", "TAIEX", "https://tw.stock.yahoo.com/s/tse.php", false, true),
            new FinanceItem("台積電", "2330.TW", "https://tw.stock.yahoo.com/quote/2330.TW", false, true),
            new FinanceItem("Nikkei 225 Index", ".N225", "https://www.cnbc.com/quotes/.N225", false),
            new FinanceItem("KOSPI Index", ".KS11", "https://www.cnbc.com/quotes/.KS11?qsearchterm=kospi", false),
            new FinanceItem("ICE Brent Crude", "@LCO.1", "https://www.cnbc.com/quotes/@LCO.1", false),
            new FinanceItem("U.S. 30 Year Treasury", "US30Y", "https://www.cnbc.com/quotes/US30Y", true),
            new FinanceItem("Gold COMEX", "@GC.1", "https://www.cnbc.com/quotes/@GC.1", false),
            new FinanceItem("Dow Jones Industrial Average", ".DJI", "https://www.cnbc.com/quotes/.DJI", false),
            new FinanceItem("S&P 500 Index", ".SPX", "https://www.cnbc.com/quotes/.SPX", false),
            new FinanceItem("NASDAQ Composite", ".IXIC", "https://www.cnbc.com/quotes/.IXIC", false),
            new FinanceItem("CBOE Volatility Index", ".VIX", "https://www.cnbc.com/quotes/.VIX", false),
            new FinanceItem("Bitcoin/USD Coin Metrics", "BTC.CM=", "https://www.cnbc.com/quotes/BTC.CM=", false),
            new FinanceItem("Ether/USD Coin Metrics", "ETH.CM=", "https://www.cnbc.com/quotes/ETH.CM=", false),
            new FinanceItem("Shiller PE Ratio (Max 44.19 Dec 1999)", "CAPE", ShillerPeRepository.SOURCE_URL, false, false, true)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feng_finance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.feature_feng_finance);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textStatus = findViewById(R.id.textFinanceStatus);
        buttonRefresh = findViewById(R.id.buttonRefreshFinance);
        webView = findViewById(R.id.webViewFinance);
        ListView listView = findViewById(R.id.listFinanceQuotes);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < quotes.size()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(quotes.get(position).url)));
            }
        });
        buttonRefresh.setOnClickListener(v -> refreshQuotes());

        setupWebView();
        refreshQuotes();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(false);
        settings.setBlockNetworkImage(true);
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Mobile Safari/537.36"
        );
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> readQuoteText(view), 1200);
            }

            @Override
            public void onReceivedError(
                    @NonNull WebView view,
                    @NonNull WebResourceRequest request,
                    @NonNull WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    addErrorQuote(getCurrentItem(), String.valueOf(error.getDescription()));
                    loadNextQuote();
                }
            }
        });
    }

    private void refreshQuotes() {
        buttonRefresh.setEnabled(false);
        rows.clear();
        quotes.clear();
        loadedCount = 0;
        loadingIndex = 0;
        adapter.notifyDataSetChanged();
        textStatus.setText(R.string.feng_finance_status_loading);
        loadCurrentQuote();
    }

    private void loadCurrentQuote() {
        FinanceItem item = getCurrentItem();
        if (item == null) {
            finishLoading();
            return;
        }
        pageFinished = false;
        updateProgress();
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS);
        webView.stopLoading();
        webView.loadUrl(item.url);
    }

    private void readQuoteText(WebView view) {
        String script = "(function(){return document.body ? document.body.innerText : '';})();";
        view.evaluateJavascript(script, value -> {
            pageFinished = true;
            handler.removeCallbacks(timeoutRunnable);
            FinanceItem item = getCurrentItem();
            if (item != null) {
                quotes.add(parseQuote(item, normalizeJavascriptResult(value)));
                renderRows();
            }
            loadNextQuote();
        });
    }

    private void loadNextQuote() {
        handler.removeCallbacks(timeoutRunnable);
        pageFinished = true;
        loadedCount = Math.min(ITEMS.length, loadedCount + 1);
        loadingIndex++;
        if (loadingIndex >= ITEMS.length) {
            finishLoading();
        } else {
            loadCurrentQuote();
        }
    }

    private FinanceItem getCurrentItem() {
        return loadingIndex >= 0 && loadingIndex < ITEMS.length ? ITEMS[loadingIndex] : null;
    }

    private void finishLoading() {
        buttonRefresh.setEnabled(true);
        textStatus.setText(getString(R.string.feng_finance_status_done, quotes.size()));
        renderRows();
    }

    private void updateProgress() {
        textStatus.setText(getString(
                R.string.feng_finance_status_progress,
                loadingIndex + 1,
                ITEMS.length,
                getCurrentItem().name
        ));
    }

    private void addErrorQuote(FinanceItem item, String message) {
        pageFinished = true;
        handler.removeCallbacks(timeoutRunnable);
        if (item != null) {
            quotes.add(new MarketQuote(item.name, item.symbol, item.url, "", "", "", "", "", message));
            renderRows();
        }
    }

    private MarketQuote parseQuote(FinanceItem item, String pageText) {
        String text = pageText == null ? "" : pageText;
        if (item.shillerQuote) {
            return parseShillerPeQuote(item, text);
        }
        String current = item.yahooQuote
                ? findMetric(text, "成交")
                : findCurrentValue(text, item.yieldQuote);
        if (current.isEmpty() && item.yahooQuote) {
            current = findCurrentValue(text, false);
        }
        String dayHigh = item.yahooQuote
                ? findMetricAny(text, "最高", "今日高", "當日最高")
                : findMetric(text, item.yieldQuote ? "Yield Day High" : "Day High");
        String dayLow = item.yahooQuote
                ? findMetricAny(text, "最低", "今日低", "當日最低")
                : findMetric(text, item.yieldQuote ? "Yield Day Low" : "Day Low");
        String weekHigh = item.yahooQuote
                ? findMetricAny(text, "52週高", "52 週高", "52週最高", "一年內最高")
                : findMetric(text, "52 Week High");
        String weekLow = item.yahooQuote
                ? findMetricAny(text, "52週低", "52 週低", "52週最低", "一年內最低")
                : findMetric(text, "52 Week Low");

        String badge = "";
        double currentValue = parseNumber(current);
        double highValue = parseNumber(weekHigh);
        double lowValue = parseNumber(weekLow);
        if (!Double.isNaN(currentValue) && !Double.isNaN(highValue) && currentValue >= highValue) {
            badge = getString(R.string.feng_finance_new_high);
        } else if (!Double.isNaN(currentValue) && !Double.isNaN(lowValue) && currentValue <= lowValue) {
            badge = getString(R.string.feng_finance_new_low);
        }

        if (current.isEmpty()) {
            return new MarketQuote(item.name, item.symbol, item.url, "", dayHigh, dayLow, weekHigh, weekLow,
                    getString(R.string.feng_finance_error_parse));
        }
        return new MarketQuote(item.name, item.symbol, item.url, current, dayHigh, dayLow, weekHigh, weekLow, badge);
    }

    private MarketQuote parseShillerPeQuote(FinanceItem item, String pageText) {
        try {
            ShillerPeRepository.ShillerPeResult result = ShillerPeRepository.parse(pageText);
            String badge = result.newHigh ? getString(R.string.feng_finance_new_high) : "";
            return new MarketQuote(
                    item.name,
                    item.symbol,
                    item.url,
                    String.format(Locale.US, "%.2f", result.current),
                    "",
                    "",
                    String.format(Locale.US, "%.2f (%s)", ShillerPeRepository.HISTORICAL_MAX, ShillerPeRepository.HISTORICAL_MAX_DATE),
                    "",
                    badge
            );
        } catch (Exception error) {
            return new MarketQuote(item.name, item.symbol, item.url, "", "", "", "", "",
                    getString(R.string.feng_finance_error_parse));
        }
    }

    private String findCurrentValue(String text, boolean yieldQuote) {
        String[] lines = text.split("\\n");
        String label = yieldQuote ? "Yield" : "Last";
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains(label)) {
                continue;
            }
            for (int j = i + 1; j < lines.length && j < i + 8; j++) {
                String number = firstCurrentNumber(lines[j]);
                if (!number.isEmpty()) {
                    return number;
                }
            }
        }
        return "";
    }

    private String findMetric(String text, String label) {
        Pattern pattern = Pattern.compile(Pattern.quote(label) + "\\s*\\n?\\s*(" + NUMBER_PATTERN.pattern() + ")",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String findMetricAny(String text, String... labels) {
        for (String label : labels) {
            String value = findMetric(text, label);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private String firstNumber(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : "";
    }

    private String firstCurrentNumber(String text) {
        String value = text == null ? "" : text.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.contains(":")
                || lower.contains("am")
                || lower.contains("pm")
                || lower.contains("volume")
                || lower.contains("open")
                || lower.contains("close")) {
            return "";
        }
        return firstNumber(value);
    }

    private double parseNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value.replace(",", "").replace("%", ""));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private String normalizeJavascriptResult(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        String normalized = value;
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replace("\\n", "\n")
                .replace("\\u003C", "<")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .trim();
    }

    private void renderRows() {
        rows.clear();
        for (MarketQuote quote : quotes) {
            rows.add(quote.toDisplayText());
        }
        if (rows.isEmpty()) {
            rows.add(getString(R.string.feng_finance_empty));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timeoutRunnable);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }

    private static class FinanceItem {
        final String name;
        final String symbol;
        final String url;
        final boolean yieldQuote;
        final boolean yahooQuote;
        final boolean shillerQuote;

        FinanceItem(String name, String symbol, String url, boolean yieldQuote) {
            this(name, symbol, url, yieldQuote, false);
        }

        FinanceItem(String name, String symbol, String url, boolean yieldQuote, boolean yahooQuote) {
            this(name, symbol, url, yieldQuote, yahooQuote, false);
        }

        FinanceItem(String name, String symbol, String url, boolean yieldQuote, boolean yahooQuote, boolean shillerQuote) {
            this.name = name;
            this.symbol = symbol;
            this.url = url;
            this.yieldQuote = yieldQuote;
            this.yahooQuote = yahooQuote;
            this.shillerQuote = shillerQuote;
        }
    }

    private static class MarketQuote {
        final String name;
        final String symbol;
        final String url;
        final String current;
        final String dayHigh;
        final String dayLow;
        final String weekHigh;
        final String weekLow;
        final String badge;

        MarketQuote(
                String name,
                String symbol,
                String url,
                String current,
                String dayHigh,
                String dayLow,
                String weekHigh,
                String weekLow,
                String badge
        ) {
            this.name = name;
            this.symbol = symbol;
            this.url = url;
            this.current = current;
            this.dayHigh = dayHigh;
            this.dayLow = dayLow;
            this.weekHigh = weekHigh;
            this.weekLow = weekLow;
            this.badge = badge;
        }

        String toDisplayText() {
            String title = name + "  " + symbol + (badge.isEmpty() ? "" : "  " + badge);
            if (current.isEmpty()) {
                return title + "\n--\n" + badge;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(title)
                    .append('\n')
                    .append("現價: ")
                    .append(current);
            if (!dayHigh.isEmpty() || !dayLow.isEmpty()) {
                builder.append(" | 日高低: ")
                        .append(dayHigh.isEmpty() ? "--" : dayHigh)
                        .append(" / ")
                        .append(dayLow.isEmpty() ? "--" : dayLow);
            }
            if (!weekHigh.isEmpty() || !weekLow.isEmpty()) {
                builder.append('\n')
                        .append("52週高低: ")
                        .append(weekHigh.isEmpty() ? "--" : weekHigh)
                        .append(" / ")
                        .append(weekLow.isEmpty() ? "--" : weekLow);
            }
            return builder.toString();
        }
    }
}
