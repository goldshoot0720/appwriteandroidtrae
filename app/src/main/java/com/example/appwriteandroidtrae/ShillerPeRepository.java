package com.example.appwriteandroidtrae;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShillerPeRepository {

    public static final String SOURCE_URL = "https://www.multpl.com/shiller-pe";
    public static final double HISTORICAL_MAX = 44.19;
    public static final String HISTORICAL_MAX_DATE = "Dec 1999";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Pattern CURRENT_PATTERN = Pattern.compile(
            "Current\\s+Shiller\\s+PE\\s+Ratio:\\s*([0-9]+(?:\\.[0-9]+)?)",
            Pattern.CASE_INSENSITIVE
    );

    public interface Callback {
        void onSuccess(ShillerPeResult result);

        void onError(Exception error);
    }

    public static void fetchLatest(Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(parse(fetchText(SOURCE_URL)));
            } catch (Exception error) {
                callback.onError(error);
            }
        });
    }

    public static ShillerPeResult parse(String html) throws Exception {
        Matcher matcher = CURRENT_PATTERN.matcher(html == null ? "" : html);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to parse Shiller PE Ratio.");
        }
        double current = Double.parseDouble(matcher.group(1));
        return new ShillerPeResult(current, current >= HISTORICAL_MAX);
    }

    private static String fetchText(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Android FengFinance");
        connection.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8");
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            connection.disconnect();
            throw new IllegalStateException("HTTP " + code);
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } finally {
            connection.disconnect();
        }
        if (code >= 400) {
            throw new IllegalStateException("HTTP " + code);
        }
        return builder.toString();
    }

    public static class ShillerPeResult {
        public final double current;
        public final boolean newHigh;

        ShillerPeResult(double current, boolean newHigh) {
            this.current = current;
            this.newHigh = newHigh;
        }
    }
}
