package com.example.appwriteandroidtrae;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriceCompareActivity extends AppCompatActivity {

    private static final String PREFS = "price_compare_prefs";
    private static final String PREF_RECENT = "price_compare_recent";
    private static final int MAX_RECENT = 8;

    private EditText editUrl;
    private Spinner spinnerRange;
    private TextView textStatus;
    private TextView textResult;
    private TextView textLatest;
    private TextView textLowest;
    private TextView textHighest;
    private TextView textAverage;
    private TextView textMedian;
    private TextView textLatestLowestGap;
    private TextView textLatestLowestDelta;
    private ListView listRecent;

    private final List<String> recentLinks = new ArrayList<>();
    private ArrayAdapter<String> recentAdapter;
    private int selectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_compare);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.feature_price_compare);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        editUrl = findViewById(R.id.editTextProductUrl);
        spinnerRange = findViewById(R.id.spinnerRange);
        textStatus = findViewById(R.id.textStatus);
        textResult = findViewById(R.id.textResult);
        textLatest = findViewById(R.id.textStatLatest);
        textLowest = findViewById(R.id.textStatLowest);
        textHighest = findViewById(R.id.textStatHighest);
        textAverage = findViewById(R.id.textStatAverage);
        textMedian = findViewById(R.id.textStatMedian);
        textLatestLowestGap = findViewById(R.id.textStatLatestLowestGap);
        textLatestLowestDelta = findViewById(R.id.textStatLatestLowestDelta);
        listRecent = findViewById(R.id.listRecentLinks);

        ArrayAdapter<CharSequence> rangeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.price_compare_ranges,
                android.R.layout.simple_spinner_item
        );
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRange.setAdapter(rangeAdapter);
        spinnerRange.setSelection(0);
        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStatus(getString(R.string.price_compare_status_ready));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Button buttonGenerate = findViewById(R.id.buttonGenerate);
        Button buttonPaste = findViewById(R.id.buttonPaste);
        Button buttonClear = findViewById(R.id.buttonClear);
        Button buttonUseSelected = findViewById(R.id.buttonUseSelected);
        Button buttonRemoveSelected = findViewById(R.id.buttonRemoveSelected);
        Button buttonRefresh = findViewById(R.id.buttonRefreshRecent);

        buttonPaste.setOnClickListener(v -> pasteFromClipboard());
        buttonClear.setOnClickListener(v -> clearInputs());
        buttonGenerate.setOnClickListener(v -> generateReport());
        buttonUseSelected.setOnClickListener(v -> applySelectedLink());
        buttonRemoveSelected.setOnClickListener(v -> removeSelectedLink());
        buttonRefresh.setOnClickListener(v -> refreshRecent());

        setupRecentList();
        resetStats();
        updateStatus(getString(R.string.price_compare_status_ready));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, R.string.price_compare_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, R.string.price_compare_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, R.string.price_compare_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        editUrl.setText(text.toString().trim());
        updateStatus(getString(R.string.price_compare_status_pasted));
    }

    private void clearInputs() {
        editUrl.setText("");
        textResult.setText(R.string.price_compare_result_placeholder);
        resetStats();
        updateStatus(getString(R.string.price_compare_status_cleared));
    }

    private void generateReport() {
        String url = editUrl.getText() == null ? "" : editUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            updateStatus(getString(R.string.price_compare_status_missing_url));
            return;
        }
        updateRecent(url);
        String range = spinnerRange.getSelectedItem() == null
                ? ""
                : spinnerRange.getSelectedItem().toString();
        textResult.setText(getString(R.string.price_compare_result_format, url, range));
        updateStatus(getString(R.string.price_compare_status_done));
    }

    private void setupRecentList() {
        recentAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                recentLinks
        );
        listRecent.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listRecent.setAdapter(recentAdapter);
        listRecent.setOnItemClickListener((parent, view, position, id) -> selectedIndex = position);
        refreshRecent();
    }

    private void refreshRecent() {
        recentLinks.clear();
        String stored = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(PREF_RECENT, "");
        if (!TextUtils.isEmpty(stored)) {
            String[] items = stored.split("\n");
            Collections.addAll(recentLinks, items);
        }
        selectedIndex = -1;
        listRecent.clearChoices();
        recentAdapter.notifyDataSetChanged();
        updateStatus(getString(R.string.price_compare_status_ready));
    }

    private void updateRecent(String url) {
        recentLinks.remove(url);
        recentLinks.add(0, url);
        if (recentLinks.size() > MAX_RECENT) {
            recentLinks.subList(MAX_RECENT, recentLinks.size()).clear();
        }
        saveRecent();
        selectedIndex = 0;
        recentAdapter.notifyDataSetChanged();
    }

    private void saveRecent() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < recentLinks.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(recentLinks.get(i));
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(PREF_RECENT, builder.toString())
                .apply();
    }

    private void applySelectedLink() {
        if (selectedIndex < 0 || selectedIndex >= recentLinks.size()) {
            Toast.makeText(this, R.string.price_compare_no_selection, Toast.LENGTH_SHORT).show();
            return;
        }
        editUrl.setText(recentLinks.get(selectedIndex));
        updateStatus(getString(R.string.price_compare_status_applied));
    }

    private void removeSelectedLink() {
        if (selectedIndex < 0 || selectedIndex >= recentLinks.size()) {
            Toast.makeText(this, R.string.price_compare_no_selection, Toast.LENGTH_SHORT).show();
            return;
        }
        recentLinks.remove(selectedIndex);
        saveRecent();
        selectedIndex = -1;
        listRecent.clearChoices();
        recentAdapter.notifyDataSetChanged();
        updateStatus(getString(R.string.price_compare_status_removed));
    }

    private void resetStats() {
        textLatest.setText(R.string.price_compare_stat_placeholder);
        textLowest.setText(R.string.price_compare_stat_placeholder);
        textHighest.setText(R.string.price_compare_stat_placeholder);
        textAverage.setText(R.string.price_compare_stat_placeholder);
        textMedian.setText(R.string.price_compare_stat_placeholder);
        textLatestLowestGap.setText(R.string.price_compare_stat_placeholder);
        textLatestLowestDelta.setText(R.string.price_compare_stat_placeholder);
    }

    private void updateStatus(String status) {
        textStatus.setText(status);
    }
}
