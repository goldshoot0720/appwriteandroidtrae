package com.example.appwriteandroidtrae;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppwriteHelper {

    private static final String APPWRITE_ENDPOINT = "https://sgp.cloud.appwrite.io/v1";
    private static final String APPWRITE_PROJECT_ID = "698212e50017eada99c8";
    private static final String APPWRITE_DATABASE_ID = "69821743002139037da1";
    private static final String APPWRITE_SUBSCRIPTION_COLLECTION_ID = "69b24465002d43df9b00";
    private static final String APPWRITE_SUBSCRIPTION_TABLE_HINT_ID = "wu6rul";
    private static final String APPWRITE_BANK_COLLECTION_ID = "698217de00124b27ff8a";
    private static final String APPWRITE_ARTICLE_COLLECTION_ID = "6989e1a1003c507a9937";
    private static final String APPWRITE_FOOD_COLLECTION_ID = "6982180a000316b84b3f";
    private static final String APPWRITE_COMMON_ACCOUNT_COLLECTION_ID = "698217e40016df4e7ca9";
    private static final String PREFS_COLLECTION_CACHE = "appwrite_collection_cache";
    private static final String PREFS_SUBSCRIPTION_COLLECTION_ID = "subscription_collection_id";
    private static final String PREFS_FOOD_COLLECTION_ID = "food_collection_id";

    private static AppwriteHelper instance;

    private final Context context;

    public interface DataCallback<T> {
        void onSuccess(T result);

        void onError(Exception error);
    }

    public static class SubscriptionItem implements java.io.Serializable {
        public final String id;
        public final String name;
        public final String site;
        public final int price;
        public final String note;
        public final String account;
        public final String currency;
        public final boolean continueFlag;
        public final long nextDateMillis;
        public final long createdAtMillis;
        public final long updatedAtMillis;

        public SubscriptionItem(
                String id,
                String name,
                String site,
                int price,
                String note,
                String account,
                String currency,
                boolean continueFlag,
                long nextDateMillis,
                long createdAtMillis,
                long updatedAtMillis
        ) {
            this.id = id;
            this.name = name;
            this.site = site;
            this.price = price;
            this.note = note;
            this.account = account;
            this.currency = currency;
            this.continueFlag = continueFlag;
            this.nextDateMillis = nextDateMillis;
            this.createdAtMillis = createdAtMillis;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    public static class BankItem implements java.io.Serializable {
        public final String id;
        public final String name;
        public final int deposit;
        public final String site;
        public final String address;
        public final int withdrawals;
        public final int transfer;
        public final String activity;
        public final String card;
        public final String account;
        public final long createdAtMillis;
        public final long updatedAtMillis;

        public BankItem(
                String id,
                String name,
                int deposit,
                String site,
                String address,
                int withdrawals,
                int transfer,
                String activity,
                String card,
                String account,
                long createdAtMillis,
                long updatedAtMillis
        ) {
            this.id = id;
            this.name = name;
            this.deposit = deposit;
            this.site = site;
            this.address = address;
            this.withdrawals = withdrawals;
            this.transfer = transfer;
            this.activity = activity;
            this.card = card;
            this.account = account;
            this.createdAtMillis = createdAtMillis;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    public static class ArticleItem implements java.io.Serializable {
        public final String id;
        public final String title;
        public final String content;
        public final String category;
        public final String ref;
        public final long newDateMillis;
        public final String url1;
        public final String url2;
        public final String url3;
        public final String file1;
        public final String file1name;
        public final String file1type;
        public final String file2;
        public final String file2name;
        public final String file2type;
        public final String file3;
        public final String file3name;
        public final String file3type;
        public final long createdAtMillis;
        public final long updatedAtMillis;

        public ArticleItem(
                String id,
                String title,
                String content,
                String category,
                String ref,
                long newDateMillis,
                String url1,
                String url2,
                String url3,
                String file1,
                String file1name,
                String file1type,
                String file2,
                String file2name,
                String file2type,
                String file3,
                String file3name,
                String file3type,
                long createdAtMillis,
                long updatedAtMillis
        ) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.ref = ref;
            this.newDateMillis = newDateMillis;
            this.url1 = url1;
            this.url2 = url2;
            this.url3 = url3;
            this.file1 = file1;
            this.file1name = file1name;
            this.file1type = file1type;
            this.file2 = file2;
            this.file2name = file2name;
            this.file2type = file2type;
            this.file3 = file3;
            this.file3name = file3name;
            this.file3type = file3type;
            this.createdAtMillis = createdAtMillis;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    public static class FoodItem implements java.io.Serializable {
        public final String id;
        public final String name;
        public final int amount;
        public final int price;
        public final String shop;
        public final long todateMillis;
        public final String photo;
        public final String photohash;
        public final long createdAtMillis;
        public final long updatedAtMillis;

        public FoodItem(
                String id,
                String name,
                int amount,
                int price,
                String shop,
                long todateMillis,
                String photo,
                String photohash,
                long createdAtMillis,
                long updatedAtMillis
        ) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.price = price;
            this.shop = shop;
            this.todateMillis = todateMillis;
            this.photo = photo;
            this.photohash = photohash;
            this.createdAtMillis = createdAtMillis;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    public static class CommonAccountItem implements java.io.Serializable {
        public final String id;
        public final String name;
        public final String file;
        public final String filetype;
        public final String note;
        public final String ref;
        public final String category;
        public final String hash;
        public final String cover;
        public final long createdAtMillis;
        public final long updatedAtMillis;

        public CommonAccountItem(
                String id,
                String name,
                String file,
                String filetype,
                String note,
                String ref,
                String category,
                String hash,
                String cover,
                long createdAtMillis,
                long updatedAtMillis
        ) {
            this.id = id;
            this.name = name;
            this.file = file;
            this.filetype = filetype;
            this.note = note;
            this.ref = ref;
            this.category = category;
            this.hash = hash;
            this.cover = cover;
            this.createdAtMillis = createdAtMillis;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    private AppwriteHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AppwriteHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AppwriteHelper(context);
        }
        return instance;
    }

    public void listSubscriptions(final DataCallback<List<SubscriptionItem>> callback) {
        new Thread(() -> {
            try {
                List<SubscriptionItem> items = fetchSubscriptions();
                callback.onSuccess(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void listBanks(final DataCallback<List<BankItem>> callback) {
        new Thread(() -> {
            try {
                List<BankItem> items = fetchBanks();
                callback.onSuccess(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void listArticles(final DataCallback<List<ArticleItem>> callback) {
        new Thread(() -> {
            try {
                List<ArticleItem> items = fetchArticles();
                callback.onSuccess(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void listFoods(final DataCallback<List<FoodItem>> callback) {
        new Thread(() -> {
            try {
                List<FoodItem> items = fetchFoods();
                callback.onSuccess(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void listCommonAccounts(final DataCallback<List<CommonAccountItem>> callback) {
        new Thread(() -> {
            try {
                List<CommonAccountItem> items = fetchCommonAccounts();
                callback.onSuccess(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public List<SubscriptionItem> listSubscriptionsSync() throws Exception {
        return fetchSubscriptions();
    }

    private List<SubscriptionItem> fetchSubscriptions() throws Exception {
        String cached = getCachedSubscriptionCollectionId();
        if (cached != null && !cached.isEmpty()) {
            try {
                return fetchData(cached, this::parseDocuments);
            } catch (Exception e) {
                if (!isCollectionNotFound(e)) {
                    throw e;
                }
                clearCachedSubscriptionCollectionId();
            }
        }

        try {
            return fetchData(APPWRITE_SUBSCRIPTION_COLLECTION_ID, this::parseDocuments);
        } catch (Exception e) {
            if (!isCollectionNotFound(e)) {
                throw e;
            }
            String resolved = resolveSubscriptionCollectionId();
            if (!APPWRITE_SUBSCRIPTION_COLLECTION_ID.equals(resolved)) {
                return fetchData(resolved, this::parseDocuments);
            }
            throw e;
        }
    }

    private List<BankItem> fetchBanks() throws Exception {
        return fetchData(APPWRITE_BANK_COLLECTION_ID, this::parseBanks);
    }

    private List<ArticleItem> fetchArticles() throws Exception {
        return fetchData(APPWRITE_ARTICLE_COLLECTION_ID, this::parseArticles);
    }

    private List<FoodItem> fetchFoods() throws Exception {
        String cached = getCachedFoodCollectionId();
        if (cached != null && !cached.isEmpty()) {
            try {
                return fetchData(cached, this::parseFoods);
            } catch (Exception e) {
                if (!isCollectionNotFound(e)) {
                    throw e;
                }
                clearCachedFoodCollectionId();
            }
        }

        try {
            return fetchData(APPWRITE_FOOD_COLLECTION_ID, this::parseFoods);
        } catch (Exception e) {
            if (!isCollectionNotFound(e)) {
                throw e;
            }
            String resolved = resolveFoodCollectionId();
            if (!APPWRITE_FOOD_COLLECTION_ID.equals(resolved)) {
                return fetchData(resolved, this::parseFoods);
            }
            throw e;
        }
    }

    private List<CommonAccountItem> fetchCommonAccounts() throws Exception {
        return fetchData(APPWRITE_COMMON_ACCOUNT_COLLECTION_ID, this::parseCommonAccounts);
    }

    private interface Parser<T> {
        List<T> parse(String json) throws JSONException;
    }

    private static final int PAGE_LIMIT = 100;

    private <T> List<T> fetchData(String collectionId, Parser<T> parser) throws Exception {
        List<T> allItems = new ArrayList<>();
        int offset = 0;

        while (true) {
            String path = "/databases/" + APPWRITE_DATABASE_ID
                    + "/collections/" + collectionId
                    + "/documents?limit=" + PAGE_LIMIT + "&offset=" + offset;

            String responseStr = request(path);

            List<T> pageItems = parser.parse(responseStr);
            allItems.addAll(pageItems);

            // 如果回傳數量少於 PAGE_LIMIT，代表已取完所有資料
            if (pageItems.size() < PAGE_LIMIT) {
                break;
            }
            offset += PAGE_LIMIT;
        }

        return allItems;
    }

    private String resolveSubscriptionCollectionId() throws Exception {
        String cached = getCachedSubscriptionCollectionId();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<CollectionInfo> collections = listCollections();
        String resolved = null;
        for (CollectionInfo info : collections) {
            if (APPWRITE_SUBSCRIPTION_TABLE_HINT_ID.equals(info.id)) {
                resolved = info.id;
                break;
            }
        }
        if (resolved == null) {
            for (CollectionInfo info : collections) {
                String name = info.name != null ? info.name.toLowerCase(Locale.getDefault()) : "";
                if (name.contains("subscription") || name.contains("subscribe") || name.contains("訂閱")) {
                    resolved = info.id;
                    break;
                }
            }
        }
        if (resolved == null) {
            resolved = APPWRITE_SUBSCRIPTION_COLLECTION_ID;
        }
        cacheSubscriptionCollectionId(resolved);
        return resolved;
    }

    private String resolveFoodCollectionId() throws Exception {
        String cached = getCachedFoodCollectionId();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<CollectionInfo> collections = listCollections();
        String resolved = null;
        for (CollectionInfo info : collections) {
            String name = info.name != null ? info.name.toLowerCase(Locale.getDefault()) : "";
            if (name.contains("food") || name.contains("foods") || name.contains("食物")) {
                resolved = info.id;
                break;
            }
        }
        if (resolved == null) {
            resolved = APPWRITE_FOOD_COLLECTION_ID;
        }
        cacheFoodCollectionId(resolved);
        return resolved;
    }

    private List<CollectionInfo> listCollections() throws Exception {
        List<CollectionInfo> results = new ArrayList<>();
        int offset = 0;
        while (true) {
            String path = "/databases/" + APPWRITE_DATABASE_ID
                    + "/collections?limit=" + PAGE_LIMIT + "&offset=" + offset;
            String responseStr = request(path);
            JSONObject root = new JSONObject(responseStr);
            JSONArray collections = root.optJSONArray("collections");
            if (collections == null) {
                break;
            }
            for (int i = 0; i < collections.length(); i++) {
                JSONObject collection = collections.getJSONObject(i);
                String id = collection.optString("$id");
                String name = collection.optString("name");
                results.add(new CollectionInfo(id, name));
            }
            if (collections.length() < PAGE_LIMIT) {
                break;
            }
            offset += PAGE_LIMIT;
        }
        return results;
    }

    private boolean isCollectionNotFound(Exception e) {
        if (e == null || e.getMessage() == null) {
            return false;
        }
        String message = e.getMessage();
        return message.contains("collection_not_found")
                || message.contains("Collection with the requested ID")
                || message.contains("collection with the requested ID");
    }

    private String getCachedSubscriptionCollectionId() {
        return context.getSharedPreferences(PREFS_COLLECTION_CACHE, Context.MODE_PRIVATE)
                .getString(PREFS_SUBSCRIPTION_COLLECTION_ID, "");
    }

    private void cacheSubscriptionCollectionId(String id) {
        context.getSharedPreferences(PREFS_COLLECTION_CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_SUBSCRIPTION_COLLECTION_ID, id)
                .apply();
    }

    private void clearCachedSubscriptionCollectionId() {
        context.getSharedPreferences(PREFS_COLLECTION_CACHE, Context.MODE_PRIVATE)
                .edit()
                .remove(PREFS_SUBSCRIPTION_COLLECTION_ID)
                .apply();
    }

    private String getCachedFoodCollectionId() {
        return context.getSharedPreferences(PREFS_COLLECTION_CACHE, Context.MODE_PRIVATE)
                .getString(PREFS_FOOD_COLLECTION_ID, "");
    }

    private void cacheFoodCollectionId(String id) {
        context.getSharedPreferences(PREFS_COLLECTION_CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_FOOD_COLLECTION_ID, id)
                .apply();
    }

    private void clearCachedFoodCollectionId() {
        context.getSharedPreferences(PREFS_COLLECTION_CACHE, Context.MODE_PRIVATE)
                .edit()
                .remove(PREFS_FOOD_COLLECTION_ID)
                .apply();
    }

    private static class CollectionInfo {
        final String id;
        final String name;

        CollectionInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    private String request(String path) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(APPWRITE_ENDPOINT + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", APPWRITE_PROJECT_ID);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            if (statusCode < 200 || statusCode >= 300) {
                throw new Exception("HTTP " + statusCode + ": " + responseBuilder);
            }
            return responseBuilder.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private List<SubscriptionItem> parseDocuments(String json) throws JSONException {
        List<SubscriptionItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray documents = root.optJSONArray("documents");
        if (documents == null) {
            return items;
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            String id = document.optString("$id");
            String name = extractName(document, id);
            String site = document.optString("site", "");
            int price = document.has("price") ? document.optInt("price", -1) : -1;
            String note = document.optString("note", "");
            String account = document.optString("account", "");
            String currency = document.optString("currency", "");
            boolean continueFlag = document.optBoolean("continue", false);
            long nextDate = extractNextDate(document);
            long createdAtMillis = extractDateField(document, "$createdAt");
            long updatedAtMillis = extractDateField(document, "$updatedAt");
            items.add(new SubscriptionItem(id, name, site, price, note, account, currency, continueFlag, nextDate, createdAtMillis, updatedAtMillis));
        }
        return items;
    }

    private List<BankItem> parseBanks(String json) throws JSONException {
        List<BankItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray documents = root.optJSONArray("documents");
        if (documents == null) {
            return items;
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            String id = document.optString("$id");
            String name = extractName(document, id);
            int deposit = document.optInt("deposit", 0);
            String site = document.optString("site", "");
            String address = document.optString("address", "");
            int withdrawals = document.optInt("withdrawals", 0);
            int transfer = document.optInt("transfer", 0);
            String activity = document.optString("activity", "");
            String card = document.optString("card", "");
            String account = document.optString("account", "");

            long createdAtMillis = extractDateField(document, "$createdAt");
            long updatedAtMillis = extractDateField(document, "$updatedAt");

            items.add(new BankItem(id, name, deposit, site, address, withdrawals, transfer, activity, card, account, createdAtMillis, updatedAtMillis));
        }
        return items;
    }

    private List<ArticleItem> parseArticles(String json) throws JSONException {
        List<ArticleItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray documents = root.optJSONArray("documents");
        if (documents == null) {
            return items;
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            String id = document.optString("$id", "");
            String title = document.optString("title", "");
            String content = document.optString("content", "");
            String category = document.optString("category", "");
            String ref = document.optString("ref", "");
            long newDateMillis = extractDateField(document, "newDate");
            String url1 = document.optString("url1", "");
            String url2 = document.optString("url2", "");
            String url3 = document.optString("url3", "");
            String file1 = document.optString("file1", "");
            String file1name = document.optString("file1name", "");
            String file1type = document.optString("file1type", "");
            String file2 = document.optString("file2", "");
            String file2name = document.optString("file2name", "");
            String file2type = document.optString("file2type", "");
            String file3 = document.optString("file3", "");
            String file3name = document.optString("file3name", "");
            String file3type = document.optString("file3type", "");
            long createdAtMillis = extractDateField(document, "$createdAt");
            long updatedAtMillis = extractDateField(document, "$updatedAt");

            items.add(new ArticleItem(
                id, title, content, category, ref, newDateMillis,
                url1, url2, url3,
                file1, file1name, file1type, file2, file2name, file2type, file3, file3name, file3type,
                createdAtMillis, updatedAtMillis
            ));
        }
        return items;
    }

    private List<FoodItem> parseFoods(String json) throws JSONException {
        List<FoodItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray documents = root.optJSONArray("documents");
        if (documents == null) {
            return items;
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            String id = document.optString("$id", "");
            String name = document.optString("name", "");
            int amount = document.optInt("amount", 0);
            int price = document.optInt("price", 0);
            String shop = document.optString("shop", "");
            long todateMillis = extractDateField(document, "todate");
            String photo = document.optString("photo", "");
            String photohash = document.optString("photohash", "");
            long createdAtMillis = extractDateField(document, "$createdAt");
            long updatedAtMillis = extractDateField(document, "$updatedAt");

            items.add(new FoodItem(
                id, name, amount, price, shop, todateMillis,
                photo, photohash, createdAtMillis, updatedAtMillis
            ));
        }
        return items;
    }

    private List<CommonAccountItem> parseCommonAccounts(String json) throws JSONException {
        List<CommonAccountItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray documents = root.optJSONArray("documents");
        if (documents == null) {
            return items;
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            String id = document.optString("$id", "");
            String name = document.optString("name", "");
            String file = document.optString("file", "");
            String filetype = document.optString("filetype", "");
            String note = document.optString("note", "");
            String ref = document.optString("ref", "");
            String category = document.optString("category", "");
            String hash = document.optString("hash", "");
            String cover = document.optString("cover", "");
            long createdAtMillis = extractDateField(document, "$createdAt");
            long updatedAtMillis = extractDateField(document, "$updatedAt");

            items.add(new CommonAccountItem(id, name, file, filetype, note, ref, category, hash, cover, createdAtMillis, updatedAtMillis));
        }
        return items;
    }

    private long extractDateField(JSONObject document, String fieldName) {
        Object rawValue = document.opt(fieldName);
        Long millis = parseDateValue(rawValue);
        return millis != null ? millis : -1L;
    }

    private String extractName(JSONObject document, String fallback) {
        String value = document.optString("name", null);
        if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)) {
            value = fallback;
        }
        return value;
    }

    private long extractNextDate(JSONObject document) {
        String[] candidates = new String[]{"nextdate", "nextDate", "date", "renewDate"};
        for (String fieldName : candidates) {
            long millis = extractDateField(document, fieldName);
            if (millis > 0L) {
                return millis;
            }
        }
        return -1L;
    }

    private Long parseDateValue(Object rawValue) {
        if (rawValue == null || JSONObject.NULL.equals(rawValue)) {
            return null;
        }
        if (rawValue instanceof Number) {
            long value = ((Number) rawValue).longValue();
            return value > 1_000_000_000_000L ? value : value * 1000L;
        }

        String value = String.valueOf(rawValue).trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return parseDateToMillis(value);
    }

    private Long parseDateToMillis(String value) {
        try {
            Instant instant = Instant.parse(value);
            return instant.toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return ZonedDateTime.parse(value).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            long epoch = Long.parseLong(value);
            return epoch > 1_000_000_000_000L ? epoch : epoch * 1000L;
        } catch (NumberFormatException ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(value);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        DateTimeFormatter[] dateTimeFormatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.getDefault()),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
            }
        }
        DateTimeFormatter[] dateFormatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.getDefault()),
                DateTimeFormatter.ISO_LOCAL_DATE
        };
        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                LocalDate date = LocalDate.parse(value, formatter);
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
