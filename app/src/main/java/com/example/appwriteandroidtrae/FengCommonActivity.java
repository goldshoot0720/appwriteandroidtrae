package com.example.appwriteandroidtrae;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FengCommonActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private TextInputEditText editTextSearchCommon;
    private VoiceInputHelper voiceInputHelper;
    private CommonListAdapter adapter;
    private final List<AppwriteHelper.CommonAccountItem> allItems = new ArrayList<>();
    private final List<AppwriteHelper.CommonAccountItem> filteredItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feng_common);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_common);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewCommon);
        textViewError = findViewById(R.id.textViewError);
        editTextSearchCommon = findViewById(R.id.editTextSearchCommon);
        TextInputLayout inputLayoutSearchCommon = findViewById(R.id.inputLayoutSearchCommon);
        voiceInputHelper = new VoiceInputHelper(this);
        voiceInputHelper.bindTextInput(
                inputLayoutSearchCommon,
                editTextSearchCommon,
                R.string.voice_prompt_common
        );

        adapter = new CommonListAdapter(filteredItems);
        listView.setAdapter(adapter);
        editTextSearchCommon.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCommon(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        textViewError.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        AppwriteHelper.getInstance(getApplicationContext())
                .listCommonAccounts(new AppwriteHelper.DataCallback<List<AppwriteHelper.CommonAccountItem>>() {
                    @Override
                    public void onSuccess(List<AppwriteHelper.CommonAccountItem> result) {
                        runOnUiThread(() -> {
                            allItems.clear();
                            allItems.addAll(result);
                            progressBar.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                            filterCommon(editTextSearchCommon.getText() != null
                                    ? editTextSearchCommon.getText().toString()
                                    : "");
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> showError(getString(R.string.generic_load_error, error.getMessage())));
                    }
                });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textViewError.setVisibility(View.VISIBLE);
        textViewError.setText(message);
    }

    private void filterCommon(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        filteredItems.clear();

        if (normalizedQuery.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (AppwriteHelper.CommonAccountItem item : allItems) {
                String name = safeLower(item.name);
                String note = safeLower(item.note);
                String ref = safeLower(item.ref);
                String category = safeLower(item.category);
                if (name.contains(normalizedQuery)
                        || note.contains(normalizedQuery)
                        || ref.contains(normalizedQuery)
                        || category.contains(normalizedQuery)) {
                    filteredItems.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.getDefault()) : "";
    }

    private class CommonListAdapter extends ArrayAdapter<AppwriteHelper.CommonAccountItem> {

        CommonListAdapter(List<AppwriteHelper.CommonAccountItem> data) {
            super(FengCommonActivity.this, 0, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_common_child, parent, false);
            }

            TextView textContent = convertView.findViewById(R.id.textChildContent);
            AppwriteHelper.CommonAccountItem item = getItem(position);
            if (item != null) {
                StringBuilder display = new StringBuilder();
                if (item.name != null && !item.name.isEmpty()) {
                    display.append(item.name);
                }
                if (item.note != null && !item.note.isEmpty()) {
                    if (display.length() > 0) {
                        display.append(" - ");
                    }
                    display.append(item.note);
                }
                if (display.length() == 0 && item.ref != null && !item.ref.isEmpty()) {
                    display.append(item.ref);
                }
                textContent.setText(display.length() > 0 ? display.toString() : "未命名項目");
            }

            return convertView;
        }
    }
}
