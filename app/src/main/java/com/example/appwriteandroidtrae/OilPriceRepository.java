package com.example.appwriteandroidtrae;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OilPriceRepository {

    private static final String PREFS_NAME = "oil_price_monitor";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY_SIZE = 180;

    private final SharedPreferences preferences;

    public OilPriceRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<OilPricePoint> getHistory() {
        List<OilPricePoint> history = new ArrayList<>();
        String raw = preferences.getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                history.add(new OilPricePoint(
                        object.optLong("dateMillis", -1L),
                        object.optDouble("price", 0.0),
                        object.optLong("fetchedAtMillis", 0L)
                ));
            }
        } catch (Exception ignored) {
        }
        history.sort(Comparator.comparingLong(point -> point.dateMillis));
        return history;
    }

    public synchronized void savePoint(OilPricePoint point) {
        List<OilPricePoint> history = getHistory();
        boolean replaced = false;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).dateMillis == point.dateMillis) {
                history.set(i, point);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            history.add(point);
        }
        history.sort(Comparator.comparingLong(item -> item.dateMillis));
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        JSONArray array = new JSONArray();
        for (OilPricePoint item : history) {
            JSONObject object = new JSONObject();
            try {
                object.put("dateMillis", item.dateMillis);
                object.put("price", item.price);
                object.put("fetchedAtMillis", item.fetchedAtMillis);
                array.put(object);
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply();
    }
}
