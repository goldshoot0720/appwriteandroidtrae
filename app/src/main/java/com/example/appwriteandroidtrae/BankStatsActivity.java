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

public class BankStatsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private TextView textTotalDeposit;
    private BankAdapter adapter;
    private final List<AppwriteHelper.BankItem> bankItems = new ArrayList<>();

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

        adapter = new BankAdapter(this, bankItems);
        listView.setAdapter(adapter);

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
                            bankItems.clear();
                            bankItems.addAll(result);
                            updateSummary(result);
                            adapter.notifyDataSetChanged();
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
        for (AppwriteHelper.BankItem item : items) {
            totalDeposit += item.deposit;
        }
        textTotalDeposit.setText(String.valueOf(totalDeposit));
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
            TextView textAccount = convertView.findViewById(R.id.textAccount);
            TextView textDeposit = convertView.findViewById(R.id.textDeposit);
            TextView textWithdrawals = convertView.findViewById(R.id.textWithdrawals);
            TextView textTransfer = convertView.findViewById(R.id.textTransfer);
            TextView textCard = convertView.findViewById(R.id.textCard);
            TextView textAddress = convertView.findViewById(R.id.textAddress);

            if (item != null) {
                textName.setText(item.name != null ? item.name : getContext().getString(R.string.bank_unknown_name));
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
