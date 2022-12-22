package com.msp.posclientapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;

public class PaymentActivity extends AppCompatActivity  implements IProduct {

    private Product product;
    private Button checkout;
    private TextView transactionStatus, message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);
        product = new Product();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //set UI and initialize objects.
        transactionStatus = findViewById(R.id.transaction_status_callback);
        checkout = findViewById(R.id.checkout);
        transactionStatus.setText(R.string.pending);
        message = findViewById(R.id.message);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            String messageString = bundle.getString("message");
            String descriptionString = bundle.getString("description");

            message.setText(messageString);
            transactionStatus.setText(descriptionString);

        }

        //Press button and proceed to checkout.
        checkout.setOnClickListener(view -> checkout());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //on waking up from callback process results
        //this intent is only called if target App (Pay App) is properly finalized.

        super.onNewIntent(intent);
    }

    private void checkout() {

        //Checkout flow initialized.
        product.setProduct(this);
    }

    @Override
    public void callMSPPayApp(JSONArray basket) {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.nokernels");
        Log.d("DEBUGGING_INTENT", "intent: " +  intent);
        if (intent != null) {
            this.sendIntent(intent, basket);
        }
    }

    private void sendIntent(Intent intent, JSONArray basket){
        //Send intent to wake up Multisafepay Pay App
        intent.putExtra("items", basket.toString());
        intent.putExtra("order_id", "POSPayApp: " + Math.random());
        intent.putExtra("order_description", "info about the order");
        intent.putExtra("currency", "EUR");
        intent.putExtra("package_name", this.getPackageName());

        this.startActivity(intent);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}