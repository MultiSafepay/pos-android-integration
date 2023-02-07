package com.msp.posclientapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Product product;
    private Button checkout;
    //private TextView transactionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        product = new Product();

        //set UI and initialize objects.
        //transactionStatus = findViewById(R.id.transaction_status_callback);
        //transactionStatus.setText(R.string.pending);
        checkout = findViewById(R.id.checkout);
        //Press button and proceed to checkout.
        checkout.setOnClickListener(view -> checkout());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //on waking up from callback process results
        //this intent is only called if target App (Pay App) is properly finalized.

        this.processMSPMiddlewareResponse(intent);
        super.onNewIntent(intent);
    }

    private void checkout() {

        Intent intent = new Intent(this, PaymentActivity.class);
        startActivity(intent);
    }

    private void processMSPMiddlewareResponse(@NonNull Intent intent) {
        //retrieve intent extra data including message.
        if (intent.hasExtra("status")) {
            int status = intent.getIntExtra("status", 0);
            String message = intent.getStringExtra("message");
            this.handleMiddlewareCallback(status, message);
        }
    }

    private void receivedCallbackIntent(String message) {
        //Intent intent = new Intent(this, TargetActivity.class);
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("message",message);
        intent.putExtra("description","pass data to this Activity");
        startActivity(intent);
    }

    private void handleMiddlewareCallback(int status, String message) {
        switch (status) {
            case 875: {
                this.receivedCallbackIntent(String.format("MSP Middleware EXCEPTION", message));
            }
            break;
            case 471: {
                this.receivedCallbackIntent(String.format("MSP Middleware OK", message));
            }
            break;
            case 17: {
                this.receivedCallbackIntent(String.format("MSP Middleware CANCELLED", message));
            }
            break;
            case 88: {
                this.receivedCallbackIntent(String.format("MSP Middleware DECLINED", message));
            }
            break;
        }
    }
}