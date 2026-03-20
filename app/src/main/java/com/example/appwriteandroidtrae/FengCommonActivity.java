package com.example.appwriteandroidtrae;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FengCommonActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ExpandableListView expandableListView;
    private TextView textViewError;
    private CommonDataAdapter adapter;
    private final List<AppwriteHelper.CommonAccountItem> items = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final Map<String, List<AppwriteHelper.CommonAccountItem>> categoryMap = new LinkedHashMap<>();

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
        expandableListView = findViewById(R.id.expandableListView);
        textViewError = findViewById(R.id.textViewError);

        adapter = new CommonDataAdapter();
        expandableListView.setAdapter(adapter);

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
        expandableListView.setVisibility(View.GONE);

        AppwriteHelper.getInstance(getApplicationContext())
                .listCommonAccounts(new AppwriteHelper.DataCallback<List<AppwriteHelper.CommonAccountItem>>() {
                    @Override
                    public void onSuccess(List<AppwriteHelper.CommonAccountItem> result) {
                        runOnUiThread(() -> {
                            items.clear();
                            items.addAll(result);
                            buildCategoryMap();
                            progressBar.setVisibility(View.GONE);
                            expandableListView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> showError("載入失敗: " + error.getMessage()));
                    }
                });
    }

    private void buildCategoryMap() {
        categoryMap.clear();
        categories.clear();
        for (AppwriteHelper.CommonAccountItem item : items) {
            String cat = (item.category != null && !item.category.isEmpty()) ? item.category : "未分類";
            if (!categoryMap.containsKey(cat)) {
                categoryMap.put(cat, new ArrayList<>());
                categories.add(cat);
            }
            categoryMap.get(cat).add(item);
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textViewError.setVisibility(View.VISIBLE);
        textViewError.setText(message);
    }

    private class CommonDataAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return categories.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            String cat = categories.get(groupPosition);
            List<AppwriteHelper.CommonAccountItem> list = categoryMap.get(cat);
            return list != null ? list.size() : 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return categories.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            String cat = categories.get(groupPosition);
            List<AppwriteHelper.CommonAccountItem> list = categoryMap.get(cat);
            return list != null ? list.get(childPosition) : null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(FengCommonActivity.this)
                        .inflate(R.layout.item_common_group, parent, false);
            }

            TextView textName = convertView.findViewById(R.id.textGroupName);
            TextView textType = convertView.findViewById(R.id.textGroupType);

            String cat = categories.get(groupPosition);
            textName.setText(cat);
            List<AppwriteHelper.CommonAccountItem> list = categoryMap.get(cat);
            textType.setText(list != null ? String.valueOf(list.size()) + " 項" : "");

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(FengCommonActivity.this)
                        .inflate(R.layout.item_common_child, parent, false);
            }

            TextView textContent = convertView.findViewById(R.id.textChildContent);
            AppwriteHelper.CommonAccountItem item = (AppwriteHelper.CommonAccountItem) getChild(groupPosition, childPosition);
            if (item != null) {
                String display = item.name;
                if (item.note != null && !item.note.isEmpty()) {
                    display += " - " + item.note;
                }
                textContent.setText(display);
            }

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
