package com.msp.posclientapp;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class PaymentActivity extends AppCompatActivity implements IProduct {

    private static final int REQUEST_CODE_SOFTPOS = 1001;
    private static final String SOFTPOS_PACKAGE = "com.phonepos.mspsoftposapp";
    private static final String SOFTPOS_ACTION_MANUAL_PAYMENT = "com.phonepos.mspsoftposapp.ACTION_MANUAL_PAYMENT";
    private static final String SOFTPOS_COMPONENT_1848 = "com.phonepos.mspsoftposapp.ManualPayInputActivity";

    private static final String PKG_SUNMI   = "com.multisafepay.pos.sunmi";
    private static final String PKG_SOFTPOS = "com.phonepos.mspsoftposapp";
    private static final String TARGET_ACTIVITY = "com.multisafepay.pos.middleware.IntentActivity";

    private Product product;
    private ProductECommerce productECommerce;
    private Button checkout;
    private TextView transactionStatus, message;
    private Switch productToggle;
    private Switch alertDialogToggle;
    private TextView paymentModeChip;

    private static final String PREFS_NAME = "PaymentActivityPrefs";
    private static final String ALERT_DIALOG_ENABLED_KEY = "alertDialogEnabled";

    private Long lastEnteredAmountCents = null;

    private JSONArray cartItems;
    private LinearLayout productListLayout;
    private TextView cartTotal;
    private Button clearCartButton;
    private LinearLayout productButtonsContainer;
    private View advancedSection;
    private TextView advancedToggle;
    private TextView cartEmptyHint;
    private boolean advancedVisible = false;

    /** Product catalog prices in minor units (cents). */
    private final Map<String, Integer> productPrices = new LinkedHashMap<>();
    private final Map<String, Integer> productIcons = new LinkedHashMap<>();
    private final Map<String, Integer> productQuantities = new HashMap<>();
    private boolean refundsSupported = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        cartItems = new JSONArray();
        productListLayout = findViewById(R.id.product_list);
        cartTotal = findViewById(R.id.cart_total);
        clearCartButton = findViewById(R.id.clear_cart_button);
        productButtonsContainer = findViewById(R.id.product_buttons_container);
        cartEmptyHint = findViewById(R.id.cart_empty_hint);
        advancedSection = findViewById(R.id.advanced_section);
        advancedToggle = findViewById(R.id.advanced_toggle);
        if (advancedToggle != null && advancedSection != null) {
            advancedToggle.setOnClickListener(v -> {
                advancedVisible = !advancedVisible;
                advancedSection.setVisibility(advancedVisible ? View.VISIBLE : View.GONE);
                advancedToggle.setText(advancedVisible
                        ? R.string.advanced_options_hide
                        : R.string.advanced_options_show);
            });
        }

        boolean isSunmiDevice = Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase("sunmi");
        refundsSupported = isSunmiDevice && isPackageInstalled(PKG_SUNMI);

        // Live-testing catalog (realistic amounts, cents).
        productPrices.put(getString(R.string.product_water), 1);         // €0.01
        productPrices.put(getString(R.string.product_te), 10);           // €0.10
        productPrices.put(getString(R.string.product_juice), 200);       // €2.00
        productPrices.put(getString(R.string.product_coffee), 250);      // €2.50
        productPrices.put(getString(R.string.product_croissant), 300);   // €3.00
        productPrices.put(getString(R.string.product_laptop), 70000);              // €700.00
        productPrices.put(getString(R.string.product_diamond_chocolate), 100000);  // €1000.00

        productIcons.put(getString(R.string.product_water), R.drawable.ic_product_water);
        productIcons.put(getString(R.string.product_te), R.drawable.ic_product_tea);
        productIcons.put(getString(R.string.product_juice), R.drawable.ic_product_juice);
        productIcons.put(getString(R.string.product_coffee), R.drawable.ic_product_coffee);
        productIcons.put(getString(R.string.product_croissant), R.drawable.ic_product_croissant);
        productIcons.put(getString(R.string.product_laptop), R.drawable.ic_product_coffee_machine);
        productIcons.put(getString(R.string.product_diamond_chocolate), R.drawable.ic_product_catering);

        clearCartButton.setOnClickListener(v -> {
            cartItems = new JSONArray();
            productQuantities.clear();
            for (String productName : productPrices.keySet()) {
                productQuantities.put(productName, 0);
                TextView qtyView = productButtonsContainer.findViewWithTag("qty_" + productName);
                if (qtyView != null) qtyView.setText("0");
            }
            refreshCartList();
            updateCartTotal();
            Toast.makeText(this, getString(R.string.cart_cleared), Toast.LENGTH_SHORT).show();
        });

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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        transactionStatus = findViewById(R.id.transaction_status_callback);
        checkout = findViewById(R.id.checkout);
        paymentModeChip = findViewById(R.id.payment_mode_chip);
        bindPaymentModeChip();

        Button firstSubscriptionBtn = findViewById(R.id.btnFirstSubscription);
        firstSubscriptionBtn.setOnClickListener(v -> askSubscriptionDataAndLaunch());

        message = findViewById(R.id.message);
        alertDialogToggle = findViewById(R.id.alert_dialog_toggle);

        Button refundButton = findViewById(R.id.btnUnreferencedRefund);
        refundButton.setOnClickListener(v -> {
            if (refundsSupported) {
                showRefundAmountDialog();
            } else {
                Toast.makeText(this, R.string.refund_not_available_softpos, Toast.LENGTH_LONG).show();
            }
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAlertDialogEnabled = prefs.getBoolean(ALERT_DIALOG_ENABLED_KEY, true);
        alertDialogToggle.setChecked(isAlertDialogEnabled);

        setupProductControls();
        updateCartTotal();

        // Footer status is no longer shown; keep views empty for layout compatibility.
        if (transactionStatus != null) {
            transactionStatus.setText("");
        }
        View statusRow = findViewById(R.id.transaction_status);
        if (statusRow != null && statusRow.getParent() instanceof View) {
            ((View) statusRow.getParent()).setVisibility(View.GONE);
        }
        if (message != null) {
            message.setVisibility(View.GONE);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String resultStatus = bundle.getString("result_status");
            String messageString = bundle.getString("message");
            String descriptionString = bundle.getString("description");

            if (resultStatus != null || messageString != null || descriptionString != null) {
                if (isAlertDialogEnabled) {
                    showPaymentDetailsDialog(
                            messageString,
                            descriptionString,
                            resultStatus,
                            shouldClearCartForStatus(resultStatus)
                    );
                }
            }
        }

        checkout.setOnClickListener(view -> {
            buildCartItems();
            long total = calculateCartTotal(cartItems);
            if (total > 0) {
                boolean isECommerce = productToggle != null && productToggle.isChecked();
                if (isECommerce) {
                    callMSPPayAppECommerce(cartItems, total);
                } else {
                    callMSPPayApp(cartItems, total);
                }
            } else {
                Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show();
            }
        });

        Button manualAmountButton = findViewById(R.id.manual_amount_button);
        manualAmountButton.setOnClickListener(v -> showAmountInputDialog());
    }

    private void bindPaymentModeChip() {
        if (paymentModeChip == null) return;
        boolean smartPos = isPackageInstalled(PKG_SUNMI);
        paymentModeChip.setText(smartPos ? R.string.mode_smartpos : R.string.mode_softpos);
    }

    private void setCheckoutEnabled(boolean enabled) {
        if (checkout == null) return;
        checkout.setEnabled(enabled);
        checkout.setClickable(enabled);
        checkout.setAlpha(1f);
        checkout.setText(enabled ? R.string.checkout : R.string.checkout_add_items);
    }

    private void showRefundAmountDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.amount_hint);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                String inputText = s.toString();
                if (inputText.isEmpty()) return;
                if (inputText.equals("00")) { s.replace(0, s.length(), "0.0"); return; }
                if (inputText.equals("0") || inputText.equals("0.")) return;
                String formattedText = inputText.replaceFirst("^0+(?!\\.)", "");
                if (formattedText.contains(".")) {
                    int indexOfDecimal = formattedText.indexOf(".");
                    if (formattedText.length() > indexOfDecimal + 3) {
                        formattedText = formattedText.substring(0, indexOfDecimal + 3);
                    }
                    String integerPart = formattedText.substring(0, indexOfDecimal);
                    if (integerPart.length() > 5) {
                        formattedText = integerPart.substring(0, 5) + formattedText.substring(indexOfDecimal);
                    }
                } else {
                    if (formattedText.length() > 5) {
                        formattedText = formattedText.substring(0, 5);
                    }
                }
                if (!formattedText.equals(inputText)) {
                    s.replace(0, s.length(), formattedText);
                }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.enter_refund_amount_title)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, which) -> {
                    String amountStr = input.getText().toString();
                    try {
                        double amount = Double.parseDouble(amountStr);
                        long amountInCents = (long) (amount * 100);
                        if (validateAmount(amountInCents)) {
                            sendRefundIntent(amountInCents);
                        } else {
                            Toast.makeText(this, R.string.invalid_refund_amount, Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel_button, (d, which) -> d.cancel())
                .create();
        dialog.show();
        styleDialogButtons(dialog);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) return;
        if (intent.hasExtra("status")) {
            int statusCode = intent.getIntExtra("status", 0);
            String msg = intent.getStringExtra("message");
            String resultStatus = mapMiddlewareStatusCode(statusCode);
            boolean isAlertDialogEnabled = alertDialogToggle != null && alertDialogToggle.isChecked();
            if (isAlertDialogEnabled) {
                showPaymentDetailsDialog(
                        msg != null ? msg : "(no message)",
                        "App-to-App callback",
                        resultStatus,
                        shouldClearCartForStatus(resultStatus)
                );
            }
            Log.d("MSP_CALLBACK", "status=" + statusCode + " mapped=" + resultStatus + " message=" + msg);
        }
    }

    private void showAmountInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_amount_euros_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        AlertDialog dialog = builder
                .setPositiveButton(R.string.ok, (d, which) -> {
                    String amountStr = input.getText().toString();
                    if (!amountStr.isEmpty()) {
                        try {
                            double euros = Double.parseDouble(amountStr);
                            long amountCents = (long) (euros * 100);
                            lastEnteredAmountCents = amountCents;
                            callMSPPayApp(null, amountCents);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel_button, (d, which) -> d.cancel())
                .create();

        dialog.setOnShowListener(dlg -> {
            styleDialogButtons(dialog);
            input.addTextChangedListener(new TextWatcher() {
                private boolean isEditing = false;
                private String previousText = "";
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { previousText = s.toString(); }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable s) {
                    if (isEditing) return;
                    isEditing = true;
                    String current = s.toString();
                    if (previousText.isEmpty() && current.equals("0")) {
                        input.setText("0.");
                        input.setSelection(input.getText().length());
                        isEditing = false;
                        return;
                    }
                    String[] parts = current.split("\\.");
                    String integerPart = parts[0];
                    if (integerPart.length() > 5) {
                        if (!previousText.equals(current)) {
                            input.setText(previousText);
                            input.setSelection(Math.min(previousText.length(), input.getText().length()));
                        }
                        isEditing = false;
                        return;
                    }
                    if (integerPart.length() == 5 && current.contains(".")) {
                        if (!previousText.equals(current)) {
                            input.setText(previousText);
                            input.setSelection(Math.min(previousText.length(), input.getText().length()));
                        }
                        isEditing = false;
                        return;
                    }
                    if (current.contains(".")) {
                        int index = current.indexOf(".");
                        if (current.length() - index - 1 > 2) {
                            String trimmed = current.substring(0, index + 3);
                            input.setText(trimmed);
                            input.setSelection(trimmed.length());
                        }
                    }
                    isEditing = false;
                }
            });
        });
        dialog.show();
    }

    private JSONArray buildManualItem(long amountCents) {
        JSONArray array = new JSONArray();
        try {
            JSONObject item = new JSONObject();
            item.put("name", "Manual amount");
            item.put("unit_price", amountCents / 100.0);
            item.put("quantity", 1);
            item.put("tax_rate", 0.21);
            array.put(item);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array;
    }

    private void setupProductControls() {
        productButtonsContainer.removeAllViews();
        for (String productName : productPrices.keySet()) {
            View row = getLayoutInflater().inflate(R.layout.item_product, productButtonsContainer, false);
            ImageView imageView = row.findViewById(R.id.product_image);
            TextView nameView = row.findViewById(R.id.product_name);
            TextView priceView = row.findViewById(R.id.product_price);
            TextView quantity = row.findViewById(R.id.product_qty);
            View minus = row.findViewById(R.id.product_minus);
            View plus = row.findViewById(R.id.product_plus);

            int priceCents = productPrices.get(productName);
            Integer iconRes = productIcons.get(productName);
            if (imageView != null && iconRes != null) {
                imageView.setImageResource(iconRes);
            }
            nameView.setText(productName);
            priceView.setText(getString(R.string.product_price_format, priceCents / 100.0));
            quantity.setText("0");
            quantity.setTag("qty_" + productName);
            minus.setOnClickListener(v -> adjustProductQuantity(productName, -1));
            plus.setOnClickListener(v -> adjustProductQuantity(productName, 1));

            productButtonsContainer.addView(row);
        }
    }

    private void adjustProductQuantity(String productName, int delta) {
        int current = productQuantities.containsKey(productName) ? productQuantities.get(productName) : 0;
        int updated = Math.max(current + delta, 0);
        productQuantities.put(productName, updated);
        TextView qtyView = productButtonsContainer.findViewWithTag("qty_" + productName);
        if (qtyView != null) qtyView.setText(String.valueOf(updated));
        refreshCartList();
        updateCartTotal();
    }

    private void refreshCartList() {
        productListLayout.removeAllViews();
        boolean hasItems = false;
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            if (entry.getValue() > 0) {
                hasItems = true;
                String name = entry.getKey();
                int quantity = entry.getValue();
                int price = productPrices.get(name);
                View line = getLayoutInflater().inflate(R.layout.item_cart_line, productListLayout, false);
                TextView nameView = line.findViewById(R.id.cart_item_name);
                TextView metaView = line.findViewById(R.id.cart_item_meta);
                nameView.setText(name);
                metaView.setText(getString(R.string.cart_line_meta, price / 100.0, quantity));
                productListLayout.addView(line);
            }
        }
        if (cartEmptyHint != null) {
            cartEmptyHint.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        }
    }

    private void buildCartItems() {
        cartItems = new JSONArray();
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            int quantity = entry.getValue();
            if (quantity > 0) {
                try {
                    JSONObject item = new JSONObject();
                    item.put("name", entry.getKey());
                    // Basket unit_price is in major currency units (euros), not cents.
                    item.put("unit_price", productPrices.get(entry.getKey()) / 100.0);
                    item.put("quantity", quantity);
                    item.put("tax_rate", 0.21);
                    cartItems.put(item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long calculateCartTotal(JSONArray cart) {
        long totalCents = 0;
        for (int i = 0; i < cart.length(); i++) {
            try {
                JSONObject item = cart.getJSONObject(i);
                double priceEuros = item.getDouble("unit_price");
                int qty = item.getInt("quantity");
                totalCents += Math.round(priceEuros * 100.0) * (long) qty;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return totalCents;
    }

    private void updateCartTotal() {
        buildCartItems();
        long total = calculateCartTotal(cartItems);
        String formatted = getString(R.string.cart_total_value, total / 100.0);
        cartTotal.setText(formatted);
        setCheckoutEnabled(total > 0);
    }

    private void showAlertDialog(String messageString, String descriptionString) {
        showPaymentDetailsDialog(messageString, descriptionString, null, false);
    }

    private void showPaymentDetailsDialog(@Nullable String messageString,
                                          @Nullable String descriptionString,
                                          @Nullable String resultStatus,
                                          boolean clearCartOnClose) {
        String safeMessage = messageString != null ? messageString : "-";
        String safeDescription = descriptionString != null ? descriptionString : "-";
        String normalized = resolveNormalizedStatus(resultStatus, messageString, descriptionString);
        String safeStatus = mapNormalizedStatusToLabel(normalized);

        View content = getLayoutInflater().inflate(R.layout.dialog_payment_details, null);
        TextView messageView = content.findViewById(R.id.dialog_payment_message);
        TextView descriptionView = content.findViewById(R.id.dialog_payment_description);
        TextView statusView = content.findViewById(R.id.dialog_payment_status);
        messageView.setText(safeMessage);
        descriptionView.setText(safeDescription);
        statusView.setText(safeStatus);
        statusView.setTextColor(ContextCompat.getColor(this, resolveStatusColorRes(normalized)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.payment_details))
                .setView(content)
                .setPositiveButton(getString(R.string.ok), (d, which) -> {
                    d.dismiss();
                    if (clearCartOnClose) {
                        clearCart();
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
        styleDialogButtons(dialog);
    }

    private void styleDialogButtons(AlertDialog dialog) {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.msp_navy));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.msp_text_secondary));
        }
    }

    /**
     * SoftPOS returns MSPStatus.RESULT names (COMPLETED / CANCELLED / DECLINED / EXCEPTION).
     * Keep legacy lowercase values for older builds.
     */
    private String resolveDisplayStatus(@Nullable String resultStatus,
                                        @Nullable String messageString,
                                        @Nullable String descriptionString) {
        return mapNormalizedStatusToLabel(
                resolveNormalizedStatus(resultStatus, messageString, descriptionString)
        );
    }

    @Nullable
    private String resolveNormalizedStatus(@Nullable String resultStatus,
                                           @Nullable String messageString,
                                           @Nullable String descriptionString) {
        String normalized = normalizeStatus(resultStatus);
        if (normalized == null) {
            normalized = normalizeStatus(messageString);
        }
        if (normalized == null) {
            normalized = normalizeStatus(descriptionString);
        }
        return normalized;
    }

    private String mapNormalizedStatusToLabel(@Nullable String normalized) {
        if ("COMPLETED".equals(normalized) || "SUCCESS".equals(normalized)) {
            return getString(R.string.status_label_completed);
        }
        if ("CANCELLED".equals(normalized) || "CANCELED".equals(normalized)) {
            return getString(R.string.status_label_cancelled);
        }
        if ("DECLINED".equals(normalized)) {
            return getString(R.string.status_label_declined);
        }
        if ("EXCEPTION".equals(normalized)) {
            return getString(R.string.status_label_exception);
        }
        return getString(R.string.status_label_unknown);
    }

    private int resolveStatusColorRes(@Nullable String normalized) {
        if ("COMPLETED".equals(normalized) || "SUCCESS".equals(normalized)) {
            return R.color.status_completed;
        }
        if ("CANCELLED".equals(normalized) || "CANCELED".equals(normalized)) {
            return R.color.status_cancelled;
        }
        if ("DECLINED".equals(normalized)) {
            return R.color.status_declined;
        }
        if ("EXCEPTION".equals(normalized)) {
            return R.color.status_exception;
        }
        return R.color.status_unknown;
    }

    @Nullable
    private String normalizeStatus(@Nullable String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;

        // SmartPOS historically prefixes message with the status token.
        int space = value.indexOf(' ');
        if (space > 0) {
            value = value.substring(0, space);
        }
        return value.toUpperCase(Locale.US);
    }

    @Nullable
    private String mapMiddlewareStatusCode(int statusCode) {
        switch (statusCode) {
            case 471:
                return "COMPLETED";
            case 17:
                return "CANCELLED";
            case 88:
                return "DECLINED";
            case 875:
                return "EXCEPTION";
            default:
                return null;
        }
    }

    private boolean shouldClearCartForStatus(@Nullable String resultStatus) {
        String normalized = normalizeStatus(resultStatus);
        return "COMPLETED".equals(normalized)
                || "SUCCESS".equals(normalized)
                || "CANCELLED".equals(normalized)
                || "CANCELED".equals(normalized)
                || "DECLINED".equals(normalized);
    }

    private void sendRefundIntent(long amountInCents) {
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(PKG_SUNMI);
        if (intent != null) {
            String packageName = intent.getPackage();
            intent.setClassName(packageName, TARGET_ACTIVITY);
            intent.putExtra("order_id", "REFUND" + System.currentTimeMillis());
            intent.putExtra("amount", amountInCents);
            intent.putExtra("refund", true);
            intent.putExtra("currency", "EUR");
            intent.putExtra("package_name", this.getPackageName());
            this.startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.sunmi_app_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void callMSPPayApp(@Nullable JSONArray basket, long amount) {
        Log.d("DEBUG_AMOUNT", "Calling MSP Pay App with amount: " + amount);
        if (!validateAmount(amount)) return;

        boolean isSunmiDevice = Build.MANUFACTURER.equalsIgnoreCase("sunmi");
        if (isSunmiDevice && isPackageInstalled(PKG_SUNMI)) {
            Intent sunmiIntent = getPackageManager().getLaunchIntentForPackage(PKG_SUNMI);
            if (sunmiIntent != null) {
                sendIntentSunmi(sunmiIntent, basket, amount);
                return;
            }
        }

        Intent softpos = new Intent(SOFTPOS_ACTION_MANUAL_PAYMENT);
        softpos.setClassName(SOFTPOS_PACKAGE, SOFTPOS_COMPONENT_1848);
        putSoftPosCompatExtras(softpos, amount, getOrderId(), basket, true);

        try {
            if (getPackageManager().resolveActivity(softpos, 0) != null) {
                Log.d("POSCLIENT", "Launching SoftPOS via startActivityForResult");
                startActivityForResult(softpos, REQUEST_CODE_SOFTPOS);
                return;
            } else {
                Toast.makeText(this, getString(R.string.softpos_not_found), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("DEBUG_SOFTPOS", "Error launching SoftPOS", e);
            Toast.makeText(this, getString(R.string.softpos_launch_error), Toast.LENGTH_SHORT).show();
        }

        if (isPackageInstalled(PKG_SUNMI)) {
            Intent sunmiIntent = getPackageManager().getLaunchIntentForPackage(PKG_SUNMI);
            if (sunmiIntent != null) {
                sendIntentSunmi(sunmiIntent, basket, amount);
            }
        }
    }

    private void putSoftPosCompatExtras(Intent i,
                                        long amountCents,
                                        String orderId,
                                        @Nullable JSONArray basket,
                                        boolean skipManualInput) {
        i.putExtra("amount", String.format(Locale.US, "%.2f", amountCents / 100.0));
        i.putExtra("order_id", orderId);
        i.putExtra("skip_manual_input", skipManualInput);
        i.putExtra("package_name", getPackageName());
        // SoftPOS accepts both keys; keep aligned with SmartPOS merchant panel description.
        i.putExtra("description", getString(R.string.order_info));
        i.putExtra("order_description", getString(R.string.order_info));

        if (basket != null) {
            i.putExtra("items", basket.toString());
        }

        i.putExtra("callback_activity", getClass().getName());
    }


    private void askSubscriptionDataAndLaunch() {
        if (lastEnteredAmountCents != null && validateAmount(lastEnteredAmountCents)) {
            promptShopperReferenceAndLaunch(lastEnteredAmountCents);
        } else {
            promptAmountAndReferenceTogether();
        }
    }

    private void promptShopperReferenceAndLaunch(long amountCents) {
        final EditText inputRef = new EditText(this);
        inputRef.setHint(R.string.shopper_reference_hint);
        inputRef.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle(R.string.first_subscription_title)
                .setMessage(R.string.enter_shopper_reference)
                .setView(inputRef)
                .setPositiveButton(R.string.continue_button, (d, w) -> {
                    String reference = inputRef.getText() == null ? "" : inputRef.getText().toString().trim();
                    if (reference.isEmpty()) {
                        Toast.makeText(this, R.string.shopper_reference_required, Toast.LENGTH_LONG).show();
                        return;
                    }
                    launchFirstSubscription(amountCents, reference);
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void promptAmountAndReferenceTogether() {
        final EditText inputAmount = new EditText(this);
        inputAmount.setHint(R.string.amount_hint);
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        final EditText inputRef = new EditText(this);
        inputRef.setHint(R.string.shopper_reference_hint);
        inputRef.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(inputAmount);
        layout.addView(inputRef);

        new AlertDialog.Builder(this)
                .setTitle(R.string.first_subscription_title)
                .setMessage(R.string.enter_amount_and_reference)
                .setView(layout)
                .setPositiveButton(R.string.continue_button, (d, w) -> {
                    String amountStr = inputAmount.getText() == null ? "" : inputAmount.getText().toString().trim();
                    String reference = inputRef.getText() == null ? "" : inputRef.getText().toString().trim();

                    if (amountStr.isEmpty() || reference.isEmpty()) {
                        Toast.makeText(this, R.string.amount_and_reference_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    long amountCents;
                    try {
                        double amountDouble = Double.parseDouble(amountStr);
                        amountCents = (long) (amountDouble * 100);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!validateAmount(amountCents)) return;

                    lastEnteredAmountCents = amountCents;
                    launchFirstSubscription(amountCents, reference);
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void launchFirstSubscription(long amountCents, String shopperReference) {
        String currency = "EUR";
        String orderId = getOrderId();
        String description = "First subscription payment";

        Intent intent = buildSubscriptionIntent(amountCents, currency, orderId, description, shopperReference);
        if (intent == null) {
            Toast.makeText(this, "MSP Pay App not found", Toast.LENGTH_LONG).show();
            return;
        }

        String pkg = intent.getPackage();
        String componentPkg = intent.getComponent() != null ? intent.getComponent().getPackageName() : null;
        boolean isSoftposTarget = PKG_SOFTPOS.equals(pkg) || PKG_SOFTPOS.equals(componentPkg);

        try {
            if (isSoftposTarget) {
                Log.d("POSCLIENT", "Launching SoftPOS recurring via startActivityForResult");
                startActivityForResult(intent, REQUEST_CODE_SOFTPOS);
            } else {
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e("RECURRING", "Error launching recurring", e);
            Toast.makeText(this, getString(R.string.softpos_launch_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private Intent buildSubscriptionIntent(long amountCents,
                                           String currency,
                                           String orderId,
                                           String description,
                                           String shopperReference) {

        boolean isSunmiDevice = Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase("sunmi");
        if (isSunmiDevice && isPackageInstalled(PKG_SUNMI)) {
            Intent sunmi = getPackageManager().getLaunchIntentForPackage(PKG_SUNMI);
            if (sunmi != null) {
                sunmi.setClassName(PKG_SUNMI, TARGET_ACTIVITY);
                sunmi.putExtra("amount", amountCents);
                sunmi.putExtra("currency", currency);
                sunmi.putExtra("order_id", orderId);
                sunmi.putExtra("description", description);
                sunmi.putExtra("package_name", getPackageName());
                sunmi.putExtra("recurring_model", "cardOnFile");
                sunmi.putExtra("reference", shopperReference);
                Log.d("RECURRING", "Launch Sunmi recurring | cents=" + amountCents + " | orderId=" + orderId);
                return sunmi;
            }
        }

        if (isPackageInstalled(PKG_SOFTPOS)) {
            Intent softpos = new Intent(SOFTPOS_ACTION_MANUAL_PAYMENT);
            softpos.setClassName(SOFTPOS_PACKAGE, SOFTPOS_COMPONENT_1848);

            softpos.putExtra("amount", String.format(Locale.US, "%.2f", amountCents / 100.0));
            softpos.putExtra("currency", currency);
            softpos.putExtra("order_id", orderId);
            softpos.putExtra("description", description);

            softpos.putExtra("skip_manual_input", true);
            softpos.putExtra("package_name", getPackageName());
            softpos.putExtra("callback_package", getPackageName());
            softpos.putExtra("callback_activity", getClass().getName());

            softpos.putExtra("recurring_model", "cardOnFile");
            softpos.putExtra("reference", shopperReference);

            Log.d("RECURRING", "Launch SoftPOS recurring | amount=" +
                    String.format(Locale.US, "%.2f", amountCents / 100.0) + " | orderId=" + orderId);
            return softpos;
        }

        return null;
    }

    @Override
    public void callMSPPayAppECommerce(JSONArray basket, long amount) {
        Log.d("DEBUG_AMOUNT", "Calling MSP Pay App E-Commerce with amount: " + amount);
        if (!validateAmount(amount)) return;
        Intent intent = getPackageManager().getLaunchIntentForPackage(PKG_SUNMI);
        if (intent != null) sendECommerceIntent(intent, basket, amount);
        else Toast.makeText(this, getString(R.string.sunmi_app_not_found), Toast.LENGTH_SHORT).show();
    }

    private void sendIntentSunmi(Intent intent, @Nullable JSONArray basket, long amount) {
        intent.setClassName(PKG_SUNMI, TARGET_ACTIVITY);
        if (basket != null) intent.putExtra("items", basket.toString());
        intent.putExtra("amount", amount);
        intent.putExtra("order_id", getOrderId());
        intent.putExtra("order_description", getString(R.string.order_info));
        intent.putExtra("currency", "EUR");
        intent.putExtra("package_name", getPackageName());
        intent.putExtra("reference", getString(R.string.reference_prefix) + System.currentTimeMillis());
        intent.putExtra("auto_close", true);
        startActivity(intent);
    }

    private void sendECommerceIntent(Intent intent, JSONArray basket, long amount) {
        intent.setClassName(intent.getPackage(), TARGET_ACTIVITY);
        setCheckoutOptions(intent);
        intent.putExtra("items", basket.toString());
        intent.putExtra("order_id", getOrderId());
        intent.putExtra("description", getString(R.string.order_info));
        intent.putExtra("currency", "EUR");
        intent.putExtra("amount", amount);
        intent.putExtra("reference", getString(R.string.reference_example));
        intent.putExtra("auto_close", false);
        intent.putExtra("package_name", getPackageName());
        startActivity(intent);
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

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
            JSONObject alternateTaxTable = new JSONObject();
            alternateTaxTable.put("name", "21_percent");
            alternateTaxTable.put("rules", new JSONArray().put(
                    new JSONObject().put("rate", 0.21).put("country", "NL")
            ));
            alternateTaxTables.put(alternateTaxTable);
            taxTables.put("alternate", alternateTaxTables);

            intent.putExtra("checkout_options", checkoutOptions.toString());
        } catch (JSONException e) {
            Log.e("DEBUGGING_INTENT", "Options JSONException: " + e.getMessage());
        }
    }

    private String getOrderId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) {
            int index = (int) (rnd.nextFloat() * chars.length());
            salt.append(chars.charAt(index));
        }
        return salt.toString();
    }

    private boolean validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            Toast.makeText(this, getString(R.string.amount_invalid), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SOFTPOS && resultCode == RESULT_OK && data != null) {
            String status = data.getStringExtra("result_status");
            String messageString = data.getStringExtra("message");
            String descriptionString = data.getStringExtra("description");

            Log.d("POSCLIENT", "onActivityResult FROM SOFTPOS rc=" + requestCode +
                    " result=" + resultCode + " status=" + status);

            showPaymentDetailsDialog(
                    messageString,
                    descriptionString,
                    status,
                    shouldClearCartForStatus(status)
            );
        }
    }

    private void clearCart() {
        cartItems = new JSONArray();
        productQuantities.clear();
        productListLayout.removeAllViews();
        for (String productName : productPrices.keySet()) {
            TextView qtyView = productButtonsContainer.findViewWithTag("qty_" + productName);
            if (qtyView != null) qtyView.setText("0");
        }
        updateCartTotal();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
