package com.example.appwriteandroidtrae;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class USDebtRepository {

    private static final String PREFS_NAME = "us_debt_monitor";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY_SIZE = 180;

    private final SharedPreferences preferences;

    public USDebtRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<USDebtPoint> getHistory() {
        List<USDebtPoint> history = new ArrayList<>();
        String raw = preferences.getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                history.add(new USDebtPoint(
                        object.optLong("debtValue", 0L),
                        object.optLong("fetchedAtMillis", 0L)
                ));
            }
        } catch (Exception ignored) {
        }
        history.sort(Comparator.comparingLong(point -> point.fetchedAtMillis));
        return history;
    }

    public synchronized void savePoint(USDebtPoint point) {
        List<USDebtPoint> history = getHistory();
        LocalDate targetDate = toLocalDate(point.fetchedAtMillis);
        boolean replaced = false;
        for (int i = 0; i < history.size(); i++) {
            if (toLocalDate(history.get(i).fetchedAtMillis).equals(targetDate)) {
                history.set(i, point);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            history.add(point);
        }
        history.sort(Comparator.comparingLong(item -> item.fetchedAtMillis));
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        JSONArray array = new JSONArray();
        for (USDebtPoint item : history) {
            JSONObject object = new JSONObject();
            try {
                object.put("debtValue", item.debtValue);
                object.put("fetchedAtMillis", item.fetchedAtMillis);
                array.put(object);
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply();
    }

    private LocalDate toLocalDate(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
