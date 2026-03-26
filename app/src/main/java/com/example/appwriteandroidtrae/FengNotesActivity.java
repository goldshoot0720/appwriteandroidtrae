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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FengNotesActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private TextInputEditText editTextSearchNotes;
    private ArticleAdapter adapter;
    private final List<AppwriteHelper.ArticleItem> allArticles = new ArrayList<>();
    private final List<AppwriteHelper.ArticleItem> filteredArticles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feng_notes);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_notes);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewArticles);
        textViewError = findViewById(R.id.textViewError);
        editTextSearchNotes = findViewById(R.id.editTextSearchNotes);

        adapter = new ArticleAdapter(this, filteredArticles);
        listView.setAdapter(adapter);

        editTextSearchNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterArticles(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadArticles();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadArticles() {
        progressBar.setVisibility(View.VISIBLE);
        textViewError.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        AppwriteHelper.getInstance(getApplicationContext())
                .listArticles(new AppwriteHelper.DataCallback<List<AppwriteHelper.ArticleItem>>() {
                    @Override
                    public void onSuccess(List<AppwriteHelper.ArticleItem> result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                            allArticles.clear();
                            allArticles.addAll(result);
                            filterArticles(editTextSearchNotes.getText() != null
                                    ? editTextSearchNotes.getText().toString()
                                    : "");
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            textViewError.setVisibility(View.VISIBLE);
                            textViewError.setText(getString(R.string.generic_load_error, error.getMessage()));
                        });
                    }
                });
    }

    private void filterArticles(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        filteredArticles.clear();

        if (normalizedQuery.isEmpty()) {
            filteredArticles.addAll(allArticles);
        } else {
            for (AppwriteHelper.ArticleItem item : allArticles) {
                String title = item.title != null ? item.title.toLowerCase(Locale.getDefault()) : "";
                String content = item.content != null ? item.content.toLowerCase(Locale.getDefault()) : "";
                if (title.contains(normalizedQuery) || content.contains(normalizedQuery)) {
                    filteredArticles.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private static class ArticleAdapter extends ArrayAdapter<AppwriteHelper.ArticleItem> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        ArticleAdapter(android.content.Context context, List<AppwriteHelper.ArticleItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_article, parent, false);
            }
            AppwriteHelper.ArticleItem item = getItem(position);

            TextView textTitle = convertView.findViewById(R.id.textArticleTitle);
            TextView textContent = convertView.findViewById(R.id.textArticleContent);
            TextView textDate = convertView.findViewById(R.id.textArticleDate);
            TextView textUrls = convertView.findViewById(R.id.textArticleUrls);

            if (item != null) {
                textTitle.setText(item.title != null && !item.title.isEmpty()
                        ? item.title
                        : getContext().getString(R.string.article_untitled));

                String content = item.content != null ? item.content : "";
                if (content.length() > 100) {
                    content = content.substring(0, 100) + "...";
                }
                textContent.setText(content);

                if (item.newDateMillis > 0) {
                    textDate.setText(dateFormat.format(new Date(item.newDateMillis)));
                } else if (item.createdAtMillis > 0) {
                    textDate.setText(dateFormat.format(new Date(item.createdAtMillis)));
                } else {
                    textDate.setText("");
                }

                int urlCount = 0;
                if (item.url1 != null && !item.url1.isEmpty()) urlCount++;
                if (item.url2 != null && !item.url2.isEmpty()) urlCount++;
                if (item.url3 != null && !item.url3.isEmpty()) urlCount++;

                if (urlCount > 0) {
                    textUrls.setVisibility(View.VISIBLE);
                    textUrls.setText(getContext().getString(R.string.article_links_count, urlCount));
                } else {
                    textUrls.setVisibility(View.GONE);
                }
            }

            return convertView;
        }
    }
}
