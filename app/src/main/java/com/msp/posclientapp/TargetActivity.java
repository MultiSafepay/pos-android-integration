package com.msp.posclientapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class TargetActivity extends AppCompatActivity {

    private Product product;
    private TextView message, description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        message = findViewById(R.id.message);
        description = findViewById(R.id.description);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            String messageString = bundle.getString("message");
            String descriptionString = bundle.getString("description");

            message.setText(messageString);
            description.setText(descriptionString);

        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}