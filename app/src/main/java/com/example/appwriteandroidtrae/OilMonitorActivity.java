package com.example.appwriteandroidtrae;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OilMonitorActivity extends AppCompatActivity {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat fetchTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private ProgressBar progressBar;
    private TextView textLatestPrice;
    private TextView textLatestDate;
    private TextView textLastFetched;
    private TextView textOilError;
    private OilPriceChartView chartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oil_monitor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("石油監控");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBarOil);
        textLatestPrice = findViewById(R.id.textLatestPrice);
        textLatestDate = findViewById(R.id.textLatestDate);
        textLastFetched = findViewById(R.id.textLastFetched);
        textOilError = findViewById(R.id.textOilError);
        chartView = findViewById(R.id.oilPriceChart);

        findViewById(R.id.buttonRefreshOil).setOnClickListener(v -> refreshOilPrices());

        renderHistory(new OilPriceRepository(getApplicationContext()).getHistory());
        refreshOilPrices();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshOilPrices() {
        progressBar.setVisibility(View.VISIBLE);
        textOilError.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                OilPricePoint point = new OilPriceFetcher().fetchLatestPoint();
                OilPriceRepository repository = new OilPriceRepository(getApplicationContext());
                repository.savePoint(point);
                List<OilPricePoint> history = repository.getHistory();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    renderHistory(history);
                });
            } catch (Exception error) {
                List<OilPricePoint> history = new OilPriceRepository(getApplicationContext()).getHistory();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    renderHistory(history);
                    textOilError.setVisibility(View.VISIBLE);
                    textOilError.setText("石油價格抓取失敗: " + error.getMessage());
                });
            }
        }).start();
    }

    private void renderHistory(List<OilPricePoint> history) {
        List<OilPricePoint> chartData = history.size() > 30
                ? new ArrayList<>(history.subList(history.size() - 30, history.size()))
                : new ArrayList<>(history);
        chartView.setPoints(chartData);

        if (history.isEmpty()) {
            textLatestPrice.setText("最新價格: --");
            textLatestDate.setText("報價日期: --");
            textLastFetched.setText("更新時間: --");
            return;
        }

        OilPricePoint latest = history.get(history.size() - 1);
        textLatestPrice.setText(String.format(Locale.getDefault(), "最新價格: %.2f", latest.price));
        textLatestDate.setText("報價日期: " + dateFormat.format(new Date(latest.dateMillis)));
        textLastFetched.setText("更新時間: " + fetchTimeFormat.format(new Date(latest.fetchedAtMillis)));
    }
}
