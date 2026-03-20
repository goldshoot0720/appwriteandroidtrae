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
    private BankAdapter adapter;
    private final List<AppwriteHelper.BankItem> bankItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("銀行統計");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewBanks);
        textViewError = findViewById(R.id.textViewError);

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
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            textViewError.setVisibility(View.VISIBLE);
                            textViewError.setText("載入失敗: " + error.getMessage());
                        });
                    }
                });
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
                textName.setText(item.name != null ? item.name : "Unknown Bank");
                textAccount.setText("Account: " + (item.account != null ? item.account : ""));
                textDeposit.setText("存款: " + item.deposit);
                textWithdrawals.setText("提款: " + item.withdrawals);
                textTransfer.setText("轉帳: " + item.transfer);
                textCard.setText("卡號: " + (item.card != null ? item.card : ""));
                
                if (item.address != null && !item.address.isEmpty()) {
                    textAddress.setVisibility(View.VISIBLE);
                    textAddress.setText("地址: " + item.address);
                } else {
                    textAddress.setVisibility(View.GONE);
                }
            }

            return convertView;
        }
    }
}
