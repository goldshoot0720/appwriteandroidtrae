package com.example.appwriteandroidtrae;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FengTubeActivity extends AppCompatActivity {

    private final List<String> rows = new ArrayList<>();
    private final List<String> rowUrls = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView textStatus;
    private Button buttonRefresh;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.TAIWAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feng_tube);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.feature_feng_tube);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textStatus = findViewById(R.id.textStatus);
        buttonRefresh = findViewById(R.id.buttonRefreshTube);
        ListView listView = findViewById(R.id.listFengTubeVideos);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < rowUrls.size() && !rowUrls.get(position).isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(rowUrls.get(position))));
            }
        });
        buttonRefresh.setOnClickListener(v -> loadVideos());

        loadVideos();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadVideos() {
        buttonRefresh.setEnabled(false);
        textStatus.setText(R.string.feng_tube_status_loading);
        rows.clear();
        rowUrls.clear();
        adapter.notifyDataSetChanged();

        FengTubeRepository.fetchLatest(new FengTubeRepository.Callback() {
            @Override
            public void onSuccess(FengTubeRepository.FengTubeResult result) {
                runOnUiThread(() -> showVideos(result));
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    buttonRefresh.setEnabled(true);
                    textStatus.setText(getString(R.string.feng_tube_status_error, error.getMessage()));
                });
            }
        });
    }

    private void showVideos(FengTubeRepository.FengTubeResult result) {
        buttonRefresh.setEnabled(true);
        rows.clear();
        rowUrls.clear();

        long freshAfter = System.currentTimeMillis() - 3L * 24L * 60L * 60L * 1000L;
        for (FengTubeRepository.FengTubeVideo video : result.videos) {
            boolean isFresh = video.publishedMillis >= freshAfter;
            String channelTitle = video.channelTitle;
            if (shouldMarkHenrenIndexUpdate(video)) {
                channelTitle += "  " + getString(R.string.feng_tube_update_badge);
            }
            String dateText = video.publishedMillis > 0
                    ? dateFormat.format(new Date(video.publishedMillis))
                    : "--";
            rows.add((isFresh ? getString(R.string.feng_tube_new_badge) + "  " : "")
                    + channelTitle
                    + "\n"
                    + video.title
                    + "\n"
                    + dateText);
            rowUrls.add(video.url);
        }

        if (rows.isEmpty()) {
            rows.add(getString(R.string.feng_tube_status_empty));
            rowUrls.add("");
            textStatus.setText(R.string.feng_tube_status_empty);
        } else {
            textStatus.setText(getString(
                    R.string.feng_tube_status_done,
                    result.videos.size(),
                    result.freshCount
            ));
        }
        adapter.notifyDataSetChanged();
    }

    private boolean shouldMarkHenrenIndexUpdate(FengTubeRepository.FengTubeVideo video) {
        return containsText(video.channelTitle, "狠人")
                && video.title != null
                && video.title.matches(".*倒台(?:指[數数])?[^0-9０-９]{0,20}[0-9０-９]+(?:[.．][0-9０-９]+)?.*");
    }

    private boolean containsText(String text, String target) {
        return text != null && text.contains(target);
    }
}
