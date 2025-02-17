package com.msp.posclientapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class PaymentActivity extends AppCompatActivity implements IProduct {

    private Product product;
    private ProductECommerce productECommerce;
    private Button checkout;
    private TextView transactionStatus, message;
    private Switch productToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        productToggle = findViewById(R.id.product_toggle);
        productToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                productECommerce = new ProductECommerce();
                product = null;
            } else {
                product = new Product();
                productECommerce = null;
            }
        });

        product = new Product();
        // productECommerce = new ProductECommerce();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Set UI and initialize objects.
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

        // Press button and proceed to checkout.
        checkout.setOnClickListener(view -> checkout());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // On waking up from callback process results
        // This intent is only called if target App (Pay App) is properly finalized.
        super.onNewIntent(intent);
    }

    private void checkout() {
        // Checkout flow initialized.
        if (product != null) {
            product.setProduct(this);
        } else if (productECommerce != null) {
            productECommerce.setProduct(this);
        }
    }

    @Override
    public void callMSPPayApp(JSONArray basket) {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
        Log.d("DEBUGGING_INTENT", "intent: " + intent);
        if (intent != null) {
            sendIntent(intent, basket);
        }
    }

    private void sendIntent(Intent intent, JSONArray basket) {
        Long amount = 61L; // replace with your actual amount
        if (!validateAmount(amount)) {
            return;
        }

        String packageName = intent.getPackage();
        intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

        // Send intent to wake up Multisafepay Pay App
        intent.putExtra("items", basket.toString());
        intent.putExtra("amount", amount); // Add total amount for product here...
        intent.putExtra("order_id", getOrderId());
        intent.putExtra("order_description", "info about the order");
        intent.putExtra("currency", "EUR");
        intent.putExtra("package_name", this.getPackageName());

        startActivity(intent);
    }

    protected String getOrderId() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // Length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /*
     ****************************************************************************
     ******************** New e-commerce product flow ***************************
     ****************************************************************************
     */

    @Override
    public void callMSPPayAppECommerce(JSONArray basket) {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
        Log.d("DEBUGGING_INTENT", "intent: " + intent);
        if (intent != null) {
            sendECommerceIntent(intent, basket);
        }
    }

    private void sendECommerceIntent(Intent intent, JSONArray basket) {
        Long amount = 399L; // replace with your actual amount
        if (!validateAmount(amount)) {
            return;
        }

        String packageName = intent.getPackage();
        intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

        // Call the setCheckoutOptions method to set the checkout options
        setCheckoutOptions(intent);

        Log.d("DEBUGGING_INTENT", "sendIntent basket: " + basket);
        intent.putExtra("items", basket.toString());
        intent.putExtra("order_id", getOrderId());
        intent.putExtra("description", "info about the order");
        intent.putExtra("currency", "EUR");
        intent.putExtra("amount", amount);
        intent.putExtra("reference", "Ref-intent-1234-dani");
        intent.putExtra("auto_close", false);
        intent.putExtra("package_name", getPackageName());

        startActivity(intent);
    }

    // intent.putExtra("checkout_options", "{\"validate_cart\":true,\"tax_tables\":{\"default\":{\"rate\":0},\"alternate\":[{\"name\":\"21_percent\",\"rules\":[{\"rate\":0.21,\"country\":\"NL\"}]}]}}");
    private void setCheckoutOptions(Intent intent) {
        try {
            JSONObject checkoutOptions = new JSONObject();

            checkoutOptions.put("validate_cart", true);

            JSONObject taxTables = new JSONObject();
            checkoutOptions.put("tax_tables", taxTables);

            JSONObject defaultTaxTable = new JSONObject();
            defaultTaxTable.put("rate", 0);
            taxTables.put("default", defaultTaxTable);

            JSONArray alternateTaxTables = new JSONArray();
            taxTables.put("alternate", alternateTaxTables);

            JSONObject alternateTaxTable = new JSONObject();
            alternateTaxTable.put("name", "_percent");
            alternateTaxTables.put(alternateTaxTable);

            JSONArray rules = new JSONArray();
            alternateTaxTable.put("rules", rules);

            JSONObject rule = new JSONObject();
            rule.put("rate", 0.21);
            rule.put("country", "NL");
            rules.put(rule);

            intent.putExtra("checkout_options", checkoutOptions.toString());
        } catch (JSONException e) {
            Log.e("DEBUGGING_INTENT", "Options JSONException occurred: " + e.getMessage());
        }
    }

    private boolean validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Amount is missing or invalid!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
