package com.example.appwriteandroidtrae;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LotteryReasonActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LotteryReasonRepository repository = new LotteryReasonRepository();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.TAIWAN);

    private ProgressBar progressBar;
    private TextView textError;
    private View contentLayout;
    private LinearLayout sectionSuperLotto;
    private LinearLayout sectionLotto649;
    private LinearLayout sectionDaily539;
    private TextView textSuperSummary;
    private TextView textLotto649Summary;
    private TextView textDaily539Summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_reason);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.feature_lottery_reason);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        textError = findViewById(R.id.textViewError);
        contentLayout = findViewById(R.id.contentLayout);
        sectionSuperLotto = findViewById(R.id.sectionSuperLotto);
        sectionLotto649 = findViewById(R.id.sectionLotto649);
        sectionDaily539 = findViewById(R.id.sectionDaily539);
        textSuperSummary = findViewById(R.id.textSuperSummary);
        textLotto649Summary = findViewById(R.id.textLotto649Summary);
        textDaily539Summary = findViewById(R.id.textDaily539Summary);

        loadData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                LotteryReasonRepository.LotteryReasonScreenData data = repository.loadAllData();
                runOnUiThread(() -> showData(data));
            } catch (Exception error) {
                runOnUiThread(() -> showError(getString(R.string.generic_load_error, error.getMessage())));
            }
        });
    }

    private void showData(LotteryReasonRepository.LotteryReasonScreenData data) {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);

        bindSection(
                sectionSuperLotto,
                textSuperSummary,
                data.superLottoDraws,
                getString(R.string.lottery_super_summary, data.superLottoDraws.size())
        );
        bindSection(
                sectionLotto649,
                textLotto649Summary,
                data.lotto649Draws,
                getString(R.string.lottery_lotto649_summary, data.lotto649Draws.size())
        );
        bindSection(
                sectionDaily539,
                textDaily539Summary,
                data.daily539Draws,
                getString(R.string.lottery_daily539_summary, data.daily539Draws.size())
        );
    }

    private void bindSection(
            LinearLayout container,
            TextView summaryView,
            List<LotteryReasonRepository.LotteryDraw> draws,
            String summary
    ) {
        summaryView.setText(summary);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (LotteryReasonRepository.LotteryDraw draw : draws) {
            View itemView = inflater.inflate(R.layout.item_lottery_reason_draw, container, false);
            TextView textPeriod = itemView.findViewById(R.id.textPeriod);
            TextView textNumbers = itemView.findViewById(R.id.textNumbers);
            TextView textMatches = itemView.findViewById(R.id.textMatches);

            textPeriod.setText(getString(R.string.lottery_period_format, draw.period, DATE_FORMATTER.format(draw.date)));
            textNumbers.setText(buildNumbersText(draw));
            textMatches.setText(buildMatchesText(draw.matches));

            container.addView(itemView);
        }
    }

    private String buildNumbersText(LotteryReasonRepository.LotteryDraw draw) {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.lottery_numbers_label)).append(' ');
        for (int i = 0; i < draw.mainNumbers.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(formatNumber(draw.mainNumbers.get(i)));
        }
        if (draw.specialNumber != null) {
            builder.append("    ")
                    .append(getString(R.string.lottery_special_label))
                    .append(' ')
                    .append(formatNumber(draw.specialNumber));
        }
        return builder.toString();
    }

    private String buildMatchesText(List<LotteryReasonRepository.GroupMatch> matches) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            LotteryReasonRepository.GroupMatch match = matches.get(i);
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(match.group.name)
                    .append(": ")
                    .append(getString(R.string.lottery_match_main, match.getMainMatchCount()));

            if (match.group.specialNumber != null) {
                builder.append(match.specialMatched
                        ? getString(R.string.lottery_match_special_hit)
                        : getString(R.string.lottery_match_special_miss));
            }

            if (!match.matchedMainNumbers.isEmpty()) {
                builder.append("  ")
                        .append(getString(R.string.lottery_hit_numbers_label))
                        .append(' ');
                for (int index = 0; index < match.matchedMainNumbers.size(); index++) {
                    if (index > 0) {
                        builder.append(' ');
                    }
                    builder.append(formatNumber(match.matchedMainNumbers.get(index)));
                }
            }
        }
        return builder.toString();
    }

    private String formatNumber(int number) {
        return String.format(Locale.US, "%02d", number);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);
        textError.setVisibility(View.VISIBLE);
        textError.setText(message);
    }
}

