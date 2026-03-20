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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FoodManagementActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private FoodAdapter adapter;
    private final List<AppwriteHelper.FoodItem> foods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("鋒兄食物");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewFoods);
        textViewError = findViewById(R.id.textViewError);

        adapter = new FoodAdapter(this, foods);
        listView.setAdapter(adapter);

        loadFoods();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadFoods() {
        progressBar.setVisibility(View.VISIBLE);
        textViewError.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        AppwriteHelper.getInstance(getApplicationContext())
                .listFoods(new AppwriteHelper.DataCallback<List<AppwriteHelper.FoodItem>>() {
                    @Override
                    public void onSuccess(List<AppwriteHelper.FoodItem> result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                            foods.clear();
                            foods.addAll(result);
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

    private static class FoodAdapter extends ArrayAdapter<AppwriteHelper.FoodItem> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        FoodAdapter(android.content.Context context, List<AppwriteHelper.FoodItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_food, parent, false);
            }
            AppwriteHelper.FoodItem item = getItem(position);

            TextView textName = convertView.findViewById(R.id.textFoodName);
            TextView textAmount = convertView.findViewById(R.id.textFoodAmount);
            TextView textPrice = convertView.findViewById(R.id.textFoodPrice);
            TextView textShop = convertView.findViewById(R.id.textFoodShop);
            TextView textDate = convertView.findViewById(R.id.textFoodDate);

            if (item != null) {
                textName.setText(item.name != null && !item.name.isEmpty() ? item.name : "未命名");
                textAmount.setText("數量: " + item.amount);
                textPrice.setText("價格: $" + item.price);
                
                if (item.shop != null && !item.shop.isEmpty()) {
                    textShop.setVisibility(View.VISIBLE);
                    textShop.setText("商店: " + item.shop);
                } else {
                    textShop.setVisibility(View.GONE);
                }
                
                if (item.todateMillis > 0) {
                    textDate.setVisibility(View.VISIBLE);
                    textDate.setText("到期: " + dateFormat.format(new Date(item.todateMillis)));
                } else {
                    textDate.setVisibility(View.GONE);
                }
            }

            return convertView;
        }
    }
}
