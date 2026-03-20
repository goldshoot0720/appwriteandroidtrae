package com.example.appwriteandroidtrae;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class FengCommonActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private CommonListAdapter adapter;
    private final List<AppwriteHelper.CommonAccountItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feng_common);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("鋒兄常用");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewCommon);
        textViewError = findViewById(R.id.textViewError);

        adapter = new CommonListAdapter(items);
        listView.setAdapter(adapter);

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
                            items.clear();
                            items.addAll(result);
                            progressBar.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> showError("載入失敗: " + error.getMessage()));
                    }
                });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textViewError.setVisibility(View.VISIBLE);
        textViewError.setText(message);
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
