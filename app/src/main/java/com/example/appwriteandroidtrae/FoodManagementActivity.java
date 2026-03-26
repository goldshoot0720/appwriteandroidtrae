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

public class FoodManagementActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listView;
    private TextView textViewError;
    private TextInputEditText editTextSearchFoods;
    private FoodAdapter adapter;
    private final List<AppwriteHelper.FoodItem> allFoods = new ArrayList<>();
    private final List<AppwriteHelper.FoodItem> filteredFoods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.screen_title_food);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listViewFoods);
        textViewError = findViewById(R.id.textViewError);
        editTextSearchFoods = findViewById(R.id.editTextSearchFoods);

        adapter = new FoodAdapter(this, filteredFoods);
        listView.setAdapter(adapter);

        editTextSearchFoods.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFoods(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

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
                            allFoods.clear();
                            allFoods.addAll(result);
                            filterFoods(editTextSearchFoods.getText() != null
                                    ? editTextSearchFoods.getText().toString()
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

    private void filterFoods(String query) {
        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        filteredFoods.clear();

        if (normalizedQuery.isEmpty()) {
            filteredFoods.addAll(allFoods);
        } else {
            for (AppwriteHelper.FoodItem item : allFoods) {
                String name = item.name != null ? item.name.toLowerCase(Locale.getDefault()) : "";
                String shop = item.shop != null ? item.shop.toLowerCase(Locale.getDefault()) : "";
                if (name.contains(normalizedQuery) || shop.contains(normalizedQuery)) {
                    filteredFoods.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
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
                textName.setText(item.name != null && !item.name.isEmpty()
                        ? item.name
                        : getContext().getString(R.string.food_unknown_name));
                textAmount.setText(getContext().getString(
                        R.string.value_label_format,
                        getContext().getString(R.string.food_amount_label),
                        String.valueOf(item.amount)
                ));
                textPrice.setText(getContext().getString(
                        R.string.currency_value_format,
                        getContext().getString(R.string.food_price_label),
                        String.valueOf(item.price)
                ));

                if (item.shop != null && !item.shop.isEmpty()) {
                    textShop.setVisibility(View.VISIBLE);
                    textShop.setText(getContext().getString(
                            R.string.value_label_format,
                            getContext().getString(R.string.food_shop_label),
                            item.shop
                    ));
                } else {
                    textShop.setVisibility(View.GONE);
                }

                long visibleDateMillis;
                int labelRes;
                if (item.todateMillis > 0) {
                    visibleDateMillis = item.todateMillis;
                    labelRes = R.string.food_date_label;
                } else if (item.createdAtMillis > 0) {
                    visibleDateMillis = item.createdAtMillis;
                    labelRes = R.string.food_created_date_label;
                } else {
                    visibleDateMillis = item.updatedAtMillis;
                    labelRes = R.string.food_updated_date_label;
                }

                if (visibleDateMillis > 0) {
                    textDate.setVisibility(View.VISIBLE);
                    textDate.setText(getContext().getString(
                            R.string.date_label_format,
                            getContext().getString(labelRes),
                            dateFormat.format(new Date(visibleDateMillis))
                    ));
                } else {
                    textDate.setVisibility(View.GONE);
                }
            }

            return convertView;
        }
    }
}
