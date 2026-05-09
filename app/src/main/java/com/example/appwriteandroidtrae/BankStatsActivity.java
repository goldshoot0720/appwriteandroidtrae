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

public class BankStatsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private TextView textTotalDeposit;
    private TextView textBankAccountCount;
    private TextView textTicketCount;
    private TextInputEditText editTextSearchBanks;
    private VoiceInputHelper voiceInputHelper;
    private BankAdapter adapter;
    private final List<AppwriteHelper.BankItem> allBankItems = new ArrayList<>();
    private final List<AppwriteHelper.BankItem> filteredBankItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_bank);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewBanks);
        textViewError = findViewById(R.id.textViewError);
        textTotalDeposit = findViewById(R.id.textTotalDeposit);
        textBankAccountCount = findViewById(R.id.textBankAccountCount);
        textTicketCount = findViewById(R.id.textTicketCount);
        editTextSearchBanks = findViewById(R.id.editTextSearchBanks);
        TextInputLayout inputLayoutSearchBanks = findViewById(R.id.inputLayoutSearchBanks);
        voiceInputHelper = new VoiceInputHelper(this);
        voiceInputHelper.bindTextInput(
                inputLayoutSearchBanks,
                editTextSearchBanks,
                R.string.voice_prompt_bank
        );

        adapter = new BankAdapter(this, filteredBankItems);
        listView.setAdapter(adapter);
        editTextSearchBanks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBanks(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadBanks();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadBanks() {
        progressBar.setVisibility(View.VISIBLE);
        textViewError.setVisibility(View.GONE);

        AppwriteHelper.getInstance(getApplicationContext())
                .listBanks(new AppwriteHelper.DataCallback<List<AppwriteHelper.BankItem>>() {
                    @Override
                    public void onSuccess(List<AppwriteHelper.BankItem> result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            allBankItems.clear();
                            allBankItems.addAll(result);
                            filterBanks(editTextSearchBanks.getText() != null
                                    ? editTextSearchBanks.getText().toString()
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

    private void updateSummary(List<AppwriteHelper.BankItem> items) {
        long totalDeposit = 0L;
        int bankAccountCount = 0;
        int ticketCount = 0;
        for (AppwriteHelper.BankItem item : items) {
            totalDeposit += item.deposit;
            if (isTaiwanBank(item)) {
                bankAccountCount++;
            } else {
                ticketCount++;
            }
        }
        textTotalDeposit.setText(String.valueOf(totalDeposit));
        textBankAccountCount.setText(getString(R.string.bank_account_total_format, bankAccountCount));
        textTicketCount.setText(getString(R.string.bank_ticket_total_format, ticketCount));
    }

    private void filterBanks(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        filteredBankItems.clear();

        if (normalizedQuery.isEmpty()) {
            filteredBankItems.addAll(allBankItems);
        } else {
            for (AppwriteHelper.BankItem item : allBankItems) {
                String name = safeLower(item.name);
                String account = safeLower(item.account);
                String address = safeLower(item.address);
                String card = safeLower(item.card);
                String site = safeLower(item.site);
                if (name.contains(normalizedQuery)
                        || account.contains(normalizedQuery)
                        || address.contains(normalizedQuery)
                        || card.contains(normalizedQuery)
                        || site.contains(normalizedQuery)) {
                    filteredBankItems.add(item);
                }
            }
        }

        updateSummary(filteredBankItems);
        adapter.notifyDataSetChanged();
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.getDefault()) : "";
    }

    private static boolean isTaiwanBank(AppwriteHelper.BankItem item) {
        if (item == null) {
            return false;
        }
        String value = ((item.name != null ? item.name : "") + " "
                + (item.site != null ? item.site : "") + " "
                + (item.address != null ? item.address : "")).toLowerCase(Locale.TAIWAN);
        String[] bankKeywords = {
                "銀行", "信用合作社", "農會", "漁會", "郵局", "中華郵政", "郵政",
                "台灣企銀", "臺灣企銀", "台灣銀行", "臺灣銀行", "土地銀行", "合作金庫",
                "第一", "華南", "彰化", "上海商銀", "台北富邦", "富邦", "國泰世華",
                "高雄銀行", "兆豐", "花旗", "王道", "台新", "新光", "陽信", "板信",
                "三信", "聯邦", "遠東", "元大", "永豐", "玉山", "凱基", "星展",
                "滙豐", "匯豐", "渣打", "樂天", "將來", "連線商業", "line bank"
        };
        for (String keyword : bankKeywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static class BankAdapter extends ArrayAdapter<AppwriteHelper.BankItem> {

        BankAdapter(android.content.Context context, List<AppwriteHelper.BankItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bank, parent, false);
            }
            AppwriteHelper.BankItem item = getItem(position);

            TextView textName = convertView.findViewById(R.id.textBankName);
            TextView textCategory = convertView.findViewById(R.id.textBankCategory);
            TextView textAccount = convertView.findViewById(R.id.textAccount);
            TextView textDeposit = convertView.findViewById(R.id.textDeposit);
            TextView textWithdrawals = convertView.findViewById(R.id.textWithdrawals);
            TextView textTransfer = convertView.findViewById(R.id.textTransfer);
            TextView textCard = convertView.findViewById(R.id.textCard);
            TextView textAddress = convertView.findViewById(R.id.textAddress);

            if (item != null) {
                textName.setText(item.name != null ? item.name : getContext().getString(R.string.bank_unknown_name));
                textCategory.setText(getContext().getString(
                        isTaiwanBank(item) ? R.string.bank_category_bank : R.string.bank_category_ticket
                ));
                textAccount.setText(getContext().getString(
                        R.string.value_label_format,
                        getContext().getString(R.string.bank_account_label),
                        item.account != null ? item.account : ""
                ));
                textDeposit.setText(getContext().getString(
                        R.string.value_label_format,
                        getContext().getString(R.string.bank_deposit_label),
                        String.valueOf(item.deposit)
                ));
                textWithdrawals.setText(getContext().getString(
                        R.string.value_label_format,
                        getContext().getString(R.string.bank_withdraw_label),
                        String.valueOf(item.withdrawals)
                ));
                textTransfer.setText(getContext().getString(
                        R.string.value_label_format,
                        getContext().getString(R.string.bank_transfer_label),
                        String.valueOf(item.transfer)
                ));
                textCard.setText(getContext().getString(
                        R.string.value_label_format,
                        getContext().getString(R.string.bank_card_label),
                        item.card != null ? item.card : ""
                ));

                if (item.address != null && !item.address.isEmpty()) {
                    textAddress.setVisibility(View.VISIBLE);
                    textAddress.setText(getContext().getString(
                            R.string.value_label_format,
                            getContext().getString(R.string.bank_address_label),
                            item.address
                    ));
                } else {
                    textAddress.setVisibility(View.GONE);
                }
            }

            return convertView;
        }
    }
}
