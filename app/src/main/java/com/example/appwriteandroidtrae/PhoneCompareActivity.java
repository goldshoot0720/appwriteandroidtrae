package com.example.appwriteandroidtrae;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneCompareActivity extends AppCompatActivity {

    private static final String[] LANDTOP_URLS = {
            "https://www.landtop.com.tw/brands?brand=samsung",
            "https://www.landtop.com.tw/brands?brand=apple"
    };
    private static final String JYES_URL = "https://www.jyes.com.tw/product.php";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<PhoneDeal> deals = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText editQuery;
    private TextView textStatus;
    private Button buttonSearch;
    private Button buttonRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_compare);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.feature_phone_compare);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        editQuery = findViewById(R.id.editTextQuery);
        textStatus = findViewById(R.id.textStatus);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonRefresh = findViewById(R.id.buttonRefresh);
        ListView listResults = findViewById(R.id.listPhoneResults);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listResults.setAdapter(adapter);
        listResults.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < deals.size() && !TextUtils.isEmpty(deals.get(position).url)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(deals.get(position).url)));
            }
        });

        buttonSearch.setOnClickListener(v -> loadDeals(false));
        buttonRefresh.setOnClickListener(v -> loadDeals(true));
        adapter.add(getString(R.string.phone_compare_empty));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadDeals(boolean refresh) {
        String query = editQuery.getText() == null ? "" : editQuery.getText().toString().trim();
        setLoading(true);
        textStatus.setText(R.string.phone_compare_status_loading);

        executor.execute(() -> {
            try {
                List<PhoneDeal> fetched = new ArrayList<>();
                for (String url : LANDTOP_URLS) {
                    fetched.addAll(parseLandtop(fetchText(url, refresh), query, url));
                }
                fetched.addAll(parseJyes(fetchText(JYES_URL, refresh), query));
                List<PhoneDeal> merged = mergeDeals(fetched);
                runOnUiThread(() -> showResults(merged));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    textStatus.setText(getString(R.string.phone_compare_status_error, error.getMessage()));
                });
            }
        });
    }

    private void showResults(List<PhoneDeal> result) {
        setLoading(false);
        deals.clear();
        deals.addAll(result);
        adapter.clear();
        if (result.isEmpty()) {
            adapter.add(getString(R.string.phone_compare_empty));
        } else {
            for (PhoneDeal deal : result) {
                adapter.add(deal.toDisplayText());
            }
        }
        textStatus.setText(getString(R.string.phone_compare_status_done, result.size()));
        adapter.notifyDataSetChanged();
    }

    private void setLoading(boolean loading) {
        buttonSearch.setEnabled(!loading);
        buttonRefresh.setEnabled(!loading);
    }

    private String fetchText(String urlText, boolean refresh) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Android FengBroTools");
        connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8");
        if (refresh) {
            connection.setRequestProperty("Cache-Control", "no-cache");
        }
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            throw new IllegalStateException("HTTP " + code);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            if (code >= 400) {
                throw new IllegalStateException("HTTP " + code);
            }
            return builder.toString();
        } finally {
            connection.disconnect();
        }
    }

    private List<PhoneDeal> parseLandtop(String html, String query, String sourceUrl) {
        List<PhoneDeal> output = new ArrayList<>();
        Pattern cardPattern = Pattern.compile(
                "<a[^>]+href=\"(/products/[^\"]+)\"[\\s\\S]{0,2200}?(?:<h3[^>]*>|<div class=\"product-name[^\"]*\">|<img[^>]+alt=\")([\\s\\S]*?)(?:</h3>|</div>|\")",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = cardPattern.matcher(html);
        while (matcher.find()) {
            String url = "https://www.landtop.com.tw" + matcher.group(1);
            String name = stripTags(matcher.group(2));
            if (!matchesQuery(name, query) || !looksLikePhone(name)) {
                continue;
            }
            String chunk = html.substring(matcher.start(), Math.min(html.length(), matcher.start() + 2600));
            int price = firstPrice(chunk);
            output.add(new PhoneDeal(name, "地標網通", price, url));
        }
        return output;
    }

    private List<PhoneDeal> parseJyes(String html, String query) {
        List<PhoneDeal> output = new ArrayList<>();
        Pattern cardPattern = Pattern.compile(
                "<a[^>]+href=\"([^\"]*(?:product|detail)[^\"]*)\"[\\s\\S]{0,1800}?<[^>]*(?:title|alt)=\"([^\"]+)\"[\\s\\S]{0,1200}?(?:NT\\$|\\$)\\s*([\\d,]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = cardPattern.matcher(html);
        while (matcher.find()) {
            String name = decodeHtml(matcher.group(2));
            if (!matchesQuery(name, query) || !looksLikePhone(name)) {
                continue;
            }
            String url = matcher.group(1);
            if (!url.startsWith("http")) {
                url = "https://www.jyes.com.tw/" + url.replaceFirst("^/+", "");
            }
            output.add(new PhoneDeal(name, "傑昇通信", parsePrice(matcher.group(3)), url));
        }
        return output;
    }

    private List<PhoneDeal> mergeDeals(List<PhoneDeal> input) {
        Map<String, PhoneDeal> merged = new LinkedHashMap<>();
        for (PhoneDeal deal : input) {
            String key = normalizeName(deal.name) + "|" + deal.source;
            PhoneDeal existing = merged.get(key);
            if (existing == null || (deal.price > 0 && (existing.price == 0 || deal.price < existing.price))) {
                merged.put(key, deal);
            }
        }
        List<PhoneDeal> result = new ArrayList<>(merged.values());
        Collections.sort(result, Comparator
                .comparingInt((PhoneDeal deal) -> deal.price == 0 ? Integer.MAX_VALUE : deal.price)
                .thenComparing(deal -> deal.name));
        return result;
    }

    private boolean looksLikePhone(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("iphone")
                || lower.contains("samsung")
                || lower.contains("galaxy")
                || lower.contains("pixel")
                || lower.contains("oppo")
                || lower.contains("sony")
                || lower.contains("xiaomi")
                || lower.contains("redmi");
    }

    private boolean matchesQuery(String name, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        String haystack = normalizeName(name);
        for (String token : normalizeName(query).split(" ")) {
            if (!token.isEmpty() && !haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private int firstPrice(String value) {
        Matcher matcher = Pattern.compile("(?:NT\\$|\\$)\\s*([\\d,]+)|([\\d,]{4,})").matcher(stripTags(value));
        while (matcher.find()) {
            int price = parsePrice(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
            if (price >= 1000) {
                return price;
            }
        }
        return 0;
    }

    private int parsePrice(String value) {
        if (value == null) {
            return 0;
        }
        String digits = value.replaceAll("[^\\d]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private String stripTags(String value) {
        return decodeHtml(value == null ? "" : value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim());
    }

    private String decodeHtml(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private String normalizeName(String value) {
        return decodeHtml(value)
                .replaceAll("(?i)(\\d{3,4})G\\b", "$1GB")
                .replace('/', ' ')
                .replaceAll("[\\[\\]()]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static class PhoneDeal {
        final String name;
        final String source;
        final int price;
        final String url;

        PhoneDeal(String name, String source, int price, String url) {
            this.name = name;
            this.source = source;
            this.price = price;
            this.url = url;
        }

        String toDisplayText() {
            String priceText = price > 0 ? "NT$ " + String.format(Locale.TAIWAN, "%,d", price) : "價格待確認";
            return name + "\n" + source + " | " + priceText + "\n" + url;
        }
    }
}
