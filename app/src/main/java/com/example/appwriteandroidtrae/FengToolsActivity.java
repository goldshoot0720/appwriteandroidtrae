package com.example.appwriteandroidtrae;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class FengToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feng_tools);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.feature_feng_tools);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        View cardFengPriceCompare = findViewById(R.id.cardFengPriceCompare);
        View cardPhoneCompare = findViewById(R.id.cardPhoneCompare);

        cardFengPriceCompare.setOnClickListener(v ->
                startActivity(new Intent(FengToolsActivity.this, PriceCompareActivity.class)));
        cardPhoneCompare.setOnClickListener(v ->
                startActivity(new Intent(FengToolsActivity.this, PhoneCompareActivity.class)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
