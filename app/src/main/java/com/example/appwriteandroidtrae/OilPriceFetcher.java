package com.example.appwriteandroidtrae;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OilPriceFetcher {

    private static final String SOURCE_URL = "https://www.gulfmerc.com/";
    private static final Pattern PRICE_AND_DATE_PATTERN = Pattern.compile(
            "OQD\\s+Daily\\s+Marker\\s+Price\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([0-9]{1,2}\\s+[A-Za-z]{3}[,-]\\s*[0-9]{4})",
            Pattern.CASE_INSENSITIVE
    );

    public OilPricePoint fetchLatestPoint() throws Exception {
        String html = requestHomePage();
        String plainText = normalizeToPlainText(html);
        Matcher matcher = PRICE_AND_DATE_PATTERN.matcher(plainText);
        if (!matcher.find()) {
            throw new Exception("Unable to parse OQD Daily Marker Price from gulfmerc.com.");
        }

        double price = Double.parseDouble(matcher.group(1));
        long dateMillis = parseDate(matcher.group(2));
        return new OilPricePoint(dateMillis, price, System.currentTimeMillis());
    }

    private String requestHomePage() throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(SOURCE_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            reader.close();

            if (statusCode < 200 || statusCode >= 300) {
                throw new Exception("HTTP " + statusCode + ": " + builder);
            }
            return builder.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String normalizeToPlainText(String html) {
        return html
                .replace("&nbsp;", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private long parseDate(String raw) {
        String normalized = raw.replace(" ,", ",").replaceAll("\\s+", " ").trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("d MMM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH)
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate date = LocalDate.parse(normalized, formatter);
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Unsupported oil price date: " + raw);
    }
}
