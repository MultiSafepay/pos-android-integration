package com.msp.posclientapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button sunmiButton;
    private Button softposButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sunmiButton = findViewById(R.id.sunmipospayment);
        softposButton = findViewById(R.id.softpayment);

        // Detect if it is a Sunmi terminal
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        boolean isSunmi = manufacturer.contains("sunmi");

        if (isSunmi) {
            sunmiButton.setVisibility(View.VISIBLE);
            softposButton.setVisibility(View.GONE);
        } else {
            sunmiButton.setVisibility(View.GONE);
            softposButton.setVisibility(View.VISIBLE);
        }

        sunmiButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            startActivity(intent);
        });

        softposButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra("launchSoftPOS", true);
            startActivity(intent);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.processMSPMiddlewareResponse(intent);
        super.onNewIntent(intent);
    }

    private void processMSPMiddlewareResponse(@NonNull Intent intent) {
        if (intent.hasExtra("status")) {
            int status = intent.getIntExtra("status", 0);
            String message = intent.getStringExtra("message");
            this.handleMiddlewareCallback(status, message);
        }
    }

    private void receivedCallbackIntent(String message, String description) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("message", message);
        intent.putExtra("description", description);
        startActivity(intent);
    }

    private void handleMiddlewareCallback(int status, String message) {
        if (message == null) message = "";
        switch (status) {
            case 875:
                receivedCallbackIntent("EXCEPTION " + message, getString(R.string.exception));
                break;
            case 471:
                receivedCallbackIntent("COMPLETED " + message, getString(R.string.completed));
                break;
            case 17:
                receivedCallbackIntent("CANCELLED " + message, getString(R.string.cancel));
                break;
            case 88:
                receivedCallbackIntent("DECLINED " + message, getString(R.string.decline));
                break;
        }
    }
}
