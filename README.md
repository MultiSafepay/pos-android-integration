<p align="center">
  <img src="https://www.multisafepay.com/img/multisafepaylogo.svg" width="400px">
</p>

# POS Android Integration

## Overview

This repository describes how to integrate a third-party Android application with the MultiSafepay Pay App using Android Intents (App-to-App communication).

---

## Integration Types

The Pay App supports two execution environments:

* **SmartPOS** → Devices with payment kernel (e.g. Sunmi)
* **Tap to Pay** → Android devices without kernel, using NFC

Both use the same App-to-App integration model.

---

## Pay App Packages

* `com.multisafepay.pos.sunmi` → SmartPOS (kernel devices)
* `com.multisafepay.pos.nokernels` → Tap to Pay (non-kernel devices)

---

## Selecting the Pay App

Use the correct package depending on the device type.

```java
private String getMSPPackage(boolean isTapToPayDevice) {
    if (isTapToPayDevice) {
        return "com.multisafepay.pos.nokernels";
    } else {
        return "com.multisafepay.pos.sunmi";
    }
}
```

---

## Manifest Configuration

```xml
<manifest>

    <queries>
        <package android:name="com.multisafepay.pos.nokernels" />
        <package android:name="com.multisafepay.pos.sunmi" />
    </queries>

</manifest>
```

---

## Callback Handling

```java
@Override
protected void onNewIntent(Intent intent) {
    processMSPMiddlewareResponse(intent);
    super.onNewIntent(intent);
}

private void processMSPMiddlewareResponse(@NonNull Intent intent) {
    if (intent.hasExtra("status")) {
        int status = intent.getIntExtra("status", 0);
        String message = intent.getStringExtra("message");
        handleMiddlewareCallback(status, message);
    }
}

private void handleMiddlewareCallback(int status, String message) {
    switch (status) {
        case 875:
            receivedCallbackIntent("EXCEPTION " + message);
            break;
        case 471:
            receivedCallbackIntent("COMPLETED " + message);
            break;
        case 17:
            receivedCallbackIntent("CANCELLED " + message);
            break;
        case 88:
            receivedCallbackIntent("DECLINED " + message);
            break;
    }
}

private void receivedCallbackIntent(String message) {
    Intent intent = new Intent(this, PaymentActivity.class);
    intent.putExtra("message", message);
    intent.putExtra("description", "pass data to this Activity");
    startActivity(intent);
}
```

---

## Payment Flows

### 1. Standard (Legacy) Flow

#### Order Items

```java
JSONArray jsonArray = new JSONArray();

try {
    JSONObject item1 = new JSONObject();
    item1.put("name", "Product 1");
    item1.put("unit_price", "0.10");
    item1.put("quantity", "1");
    item1.put("merchant_item_id", "749857");
    item1.put("tax", "3.90");

    JSONObject item2 = new JSONObject();
    item2.put("name", "Product 2");
    item2.put("unit_price", "0.20");
    item2.put("quantity", "1");
    item2.put("merchant_item_id", "749857");
    item2.put("tax", "1.40");

    jsonArray.put(item1);
    jsonArray.put(item2);

} catch (JSONException e) {
    e.printStackTrace();
}
```

---

#### Sending Payment Intent

```java
boolean isTapToPayDevice = false; // Implement your own device detection
String packageName = getMSPPackage(isTapToPayDevice);

Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

if (intent != null) {

    if (!isTapToPayDevice) {
        intent.setClassName(packageName,
            "com.multisafepay.pos.middleware.IntentActivity");
    }

    // amount must be long and expressed in minor units (for example: cents)
    long amountInCents = amount;

    intent.putExtra("items", jsonArray.toString());
    intent.putExtra("order_id", getOrderId());
    intent.putExtra("order_description", "info about the order");
    intent.putExtra("currency", "EUR");
    intent.putExtra("amount", amountInCents);
    intent.putExtra("package_name", getPackageName());

    startActivity(intent);
}
```

---

### 2. E-commerce Flow

#### Order Items

```java
JSONArray jsonArray = new JSONArray();

try {
    JSONObject socks = new JSONObject();
    socks.put("name", "Socks");
    socks.put("description", "One pair of black socks");
    socks.put("merchant_item_id", "001-M");
    socks.put("unit_price", 0.105785124);
    socks.put("quantity", 3);
    socks.put("tax_table_selector", "21_percent");

    JSONObject shipping = new JSONObject();
    shipping.put("name", "Shipping");
    shipping.put("description", "Domestic shipping (zone 1)");
    shipping.put("merchant_item_id", "msp-shipping");
    shipping.put("unit_price", 0.15);
    shipping.put("quantity", 1);

    jsonArray.put(socks);
    jsonArray.put(shipping);

} catch (JSONException e) {
    e.printStackTrace();
}
```

---

#### Sending Payment Intent

```java
boolean isTapToPayDevice = false; // Implement your own device detection
String packageName = getMSPPackage(isTapToPayDevice);

Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

if (intent != null) {

    if (!isTapToPayDevice) {
        intent.setClassName(packageName,
            "com.multisafepay.pos.middleware.IntentActivity");
    }

    setCheckoutOptions(intent);

    // amount must be long and expressed in minor units (for example: cents)
    long amountInCents = amount;

    intent.putExtra("items", jsonArray.toString());
    intent.putExtra("order_id", getOrderId());
    intent.putExtra("description", "info about the order");
    intent.putExtra("currency", "EUR");
    intent.putExtra("amount", amountInCents);
    intent.putExtra("reference", "Ref-" + System.currentTimeMillis());
    intent.putExtra("auto_close", false);
    intent.putExtra("package_name", getPackageName());

    startActivity(intent);
}
```

---

### Checkout Options

```java
private void setCheckoutOptions(Intent intent) {
    try {
        JSONObject checkoutOptions = new JSONObject();

        checkoutOptions.put("validate_cart", true);

        JSONObject taxTables = new JSONObject();
        checkoutOptions.put("tax_tables", taxTables);

        JSONObject defaultTaxTable = new JSONObject();
        defaultTaxTable.put("rate", 0);
        taxTables.put("default", defaultTaxTable);

        intent.putExtra("checkout_options", checkoutOptions.toString());

    } catch (JSONException e) {
        e.printStackTrace();
    }
}
```

---

## Unreferenced Refund Flow

```java
private void sendRefundIntent(long amountInCents, boolean isTapToPayDevice) {

    String packageName = getMSPPackage(isTapToPayDevice);

    Intent intent = getPackageManager()
            .getLaunchIntentForPackage(packageName);

    if (intent != null) {

        if (!isTapToPayDevice) {
            intent.setClassName(
                    packageName,
                    "com.multisafepay.pos.middleware.IntentActivity"
            );
        }

        intent.putExtra("order_id", "REFUND_" + System.currentTimeMillis());
        intent.putExtra("amount", amountInCents);
        intent.putExtra("refund", true);
        intent.putExtra("package_name", getPackageName());

        startActivity(intent);
    }
}
```

---

## Tap to Pay Notes

Tap to Pay uses the same App-to-App integration model as SmartPOS, but the entry point is different.

* Same Intent structure
* Same parameters
* Same callback handling

For **SmartPOS** (`com.multisafepay.pos.sunmi`), the payment flow is started directly via:

`com.multisafepay.pos.middleware.IntentActivity`

For **Tap to Pay** (`com.multisafepay.pos.nokernels`), the app should be launched using the package launcher intent:

`getLaunchIntentForPackage("com.multisafepay.pos.nokernels")`

In this case, the app handles the incoming intent through its launcher flow before navigating to the payment screen.

Important:
* Use `setClassName(..., "com.multisafepay.pos.middleware.IntentActivity")` only for SmartPOS
* Do not force `IntentActivity` for Tap to Pay
* `amount` must be sent as `long` in minor units
* `package_name` must be the package name of the third-party app that should receive the callback intent