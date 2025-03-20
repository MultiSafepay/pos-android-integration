package com.msp.posclientapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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
    private Switch alertDialogToggle;

    private static final String PREFS_NAME = "PaymentActivityPrefs";
    private static final String ALERT_DIALOG_ENABLED_KEY = "alertDialogEnabled";

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
        alertDialogToggle = findViewById(R.id.alert_dialog_toggle);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAlertDialogEnabled = prefs.getBoolean(ALERT_DIALOG_ENABLED_KEY, true);
        alertDialogToggle.setChecked(isAlertDialogEnabled);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String messageString = bundle.getString("message");
            String descriptionString = bundle.getString("description");

            message.setText(messageString);
            transactionStatus.setText(descriptionString);

            // Show alert dialog only if the saved state is enabled
            if (isAlertDialogEnabled) {
                showAlertDialog(messageString, descriptionString);
            }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Amount");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText("0.01"); // Default value
        builder.setView(input);

        // Attach the TextWatcher here
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Get the current input text
                String inputText = s.toString();

                // Allow the field to remain empty
                if (inputText.isEmpty()) {
                    return;
                }

                // Check if the input is "00" and convert it to "0.0"
                if (inputText.equals("00")) {
                    s.replace(0, s.length(), "0.0");
                    return;
                }

                // Check if the input is "0" or "0." and allow it
                if (inputText.equals("0") || inputText.equals("0.")) {
                    return;
                }

                // Remove unnecessary leading zeros unless it starts with "0."
                String formattedText = inputText.replaceFirst("^0+(?!\\.)", "");

                // Enforce two decimal places if a decimal point exists
                if (formattedText.contains(".")) {
                    int indexOfDecimal = formattedText.indexOf(".");
                    if (formattedText.length() > indexOfDecimal + 3) {
                        formattedText = formattedText.substring(0, indexOfDecimal + 3); // Keep two digits after the decimal
                    }
                }

                // Only replace the text if the formatted value differs
                if (!formattedText.equals(inputText)) {
                    s.replace(0, s.length(), formattedText);
                }
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            String amountStr = input.getText().toString();
            Log.d("DEBUG_AMOUNT", "Entered amount: " + amountStr); // Log the entered value

            try {
                double amount = Double.parseDouble(amountStr);
                long amountInCents = (long) (amount * 100); // Convert to cents
                Log.d("DEBUG_AMOUNT", "Amount in cents: " + amountInCents); // Log the converted value

                if (validateAmount(amountInCents)) {
                    if (product != null) {
                        product.setProduct(this, amountInCents); // Pass amount to Product
                    } else if (productECommerce != null) {
                        productECommerce.setProduct(this, amountInCents); // Pass amount to ProductECommerce
                    }
                } else {
                    Toast.makeText(this, "Invalid amount entered!", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Method to show AlertDialog
    private void showAlertDialog(String messageString, String descriptionString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Payment Details")
                .setMessage("Message: " + messageString + "\n\nDescription: " + descriptionString)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false); // Prevent dismiss on outside touch
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void callMSPPayApp(JSONArray basket, long amount) {
        Log.d("DEBUG_AMOUNT", "Calling MSP Pay App with amount: " + amount); // Log the amount

        Intent intent = getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
        Log.d("DEBUGGING_INTENT", "intent: " + intent);
        if (intent != null) {
            sendIntent(intent, basket, amount);
        }
    }

    private void sendIntent(Intent intent, JSONArray basket, long amount) {
        Log.d("DEBUG_AMOUNT", "Sending intent with amount: " + amount); // Log the amount

        if (!validateAmount(amount)) {
            return;
        }

        String packageName = intent.getPackage();
        intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

        intent.putExtra("items", basket.toString());
        intent.putExtra("amount", amount); // Use the amount entered by the user
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
    public void callMSPPayAppECommerce(JSONArray basket, long amount) {
        Log.d("DEBUG_AMOUNT", "Calling MSP Pay App E-Commerce with amount: " + amount); // Log the amount

        Intent intent = getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
        Log.d("DEBUGGING_INTENT", "intent: " + intent);
        if (intent != null) {
            sendECommerceIntent(intent, basket, amount);
        }
    }

    private void sendECommerceIntent(Intent intent, JSONArray basket, long amount) {
        Log.d("DEBUG_AMOUNT", "Sending e-commerce intent with amount: " + amount); // Log the amount

        if (!validateAmount(amount)) {
            return;
        }

        String packageName = intent.getPackage();
        intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

        setCheckoutOptions(intent);

        intent.putExtra("items", basket.toString());
        intent.putExtra("order_id", getOrderId());
        intent.putExtra("description", "info about the order");
        intent.putExtra("currency", "EUR");
        intent.putExtra("amount", amount); // Use the passed amount
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
            Log.d("DEBUG_AMOUNT", "Validation failed: Amount is missing or invalid!");
            Toast.makeText(this, "Amount is missing or invalid!", Toast.LENGTH_SHORT).show();
            return false;
        }
        Log.d("DEBUG_AMOUNT", "Validation succeeded: Amount is valid!");
        return true;
    }
}
