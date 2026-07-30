<p align="center">
  <img src="https://www.multisafepay.com/img/multisafepaylogo.svg" width="400px">
</p>

# POS Android Integration

## Overview

This repository describes how to integrate a third-party Android application with the MultiSafepay Pay App using Android Intents (App-to-App communication).

---

## Integration Types

The Pay App supports two execution environments:

* **SmartPOS** → Devices with a payment kernel (most Sunmi terminals)
* **SoftPOS (Tap to Pay)** → Devices without a payment kernel, using the device's own NFC reader — this covers pure Android/COTS (commercial off-the-shelf) hardware, but also certain Sunmi models that ship without kernel support (e.g. the L3)

Both use App-to-App communication via Android Intents, but they are launched and called back differently — see [Selecting the Pay App](#selecting-the-pay-app), [Callback Handling](#callback-handling), and [SoftPOS Integration](#softpos-integration).

> **Note:** "Sunmi" is not synonymous with "has a kernel." Some Sunmi models (e.g. the L3) don't have payment kernel support and must use SoftPOS instead of SmartPOS.

---

## Pay App Packages

* `com.multisafepay.pos.sunmi` → SmartPOS (kernel devices)
* `com.phonepos.mspsoftposapp` → SoftPOS / Tap to Pay (non-kernel devices)

---

## Selecting the Pay App

The manufacturer alone doesn't tell you whether a device has a payment kernel — some Sunmi models (e.g. the L3) don't. Use manufacturer as a coarse pre-filter, but gate the actual decision on whether the SmartPOS Pay App is installed:

```java
private boolean isSunmiDevice() {
    return Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase("sunmi");
}

private boolean isPackageInstalled(String packageName) {
    try {
        getPackageManager().getPackageInfo(packageName, 0);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

* If the device is Sunmi **and** `com.multisafepay.pos.sunmi` is installed → launch SmartPOS directly (see [Payment Flows](#payment-flows)).
* Otherwise (non-Sunmi hardware, or a Sunmi model without kernel support / without the SmartPOS Pay App installed) → launch SoftPOS (see [SoftPOS Integration](#softpos-integration)).

The two Pay Apps are not interchangeable drop-in replacements for each other: SmartPOS is launched with `getLaunchIntentForPackage` + `setClassName`, while SoftPOS uses an explicit action and component. Treat them as two distinct integrations sharing the same general App-to-App model.

---

## Manifest Configuration

```xml
<manifest>

    <queries>
        <package android:name="com.multisafepay.pos.sunmi" />
        <package android:name="com.phonepos.mspsoftposapp" />
    </queries>

</manifest>
```

---

## Callback Handling

SmartPOS reports the result back to your app through `onNewIntent`. SoftPOS reports it through `onActivityResult` instead — see below.

### SmartPOS Callback

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

### SoftPOS Callback

SoftPOS is launched with `startActivityForResult` and never calls `onNewIntent` — it returns its result directly to `onActivityResult`:

```java
private static final int REQUEST_CODE_SOFTPOS = 1001;

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CODE_SOFTPOS && resultCode == RESULT_OK && data != null) {
        String status = data.getStringExtra("result_status"); // "success" | "cancelled" | "declined"
        String message = data.getStringExtra("message");
        String description = data.getStringExtra("description");

        // update UI / clear cart based on status
    }
}
```

---

## Payment Flows

> The flows below use the **SmartPOS (Sunmi)** launch pattern — `getLaunchIntentForPackage` + `setClassName` + `startActivity`. For SoftPOS, see [SoftPOS Integration](#softpos-integration), which uses a different Intent action/component and `startActivityForResult`.

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
String packageName = "com.multisafepay.pos.sunmi";
Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

if (intent != null) {

    intent.setClassName(packageName,
        "com.multisafepay.pos.middleware.IntentActivity");

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
String packageName = "com.multisafepay.pos.sunmi";
Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

if (intent != null) {

    intent.setClassName(packageName,
        "com.multisafepay.pos.middleware.IntentActivity");

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
private void sendRefundIntent(long amountInCents) {
    String packageName = "com.multisafepay.pos.sunmi";
    Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

    if (intent != null) {
        intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

        intent.putExtra("order_id", "REFUND_" + System.currentTimeMillis());
        intent.putExtra("amount", amountInCents);
        intent.putExtra("refund", true);
        intent.putExtra("package_name", getPackageName());

        startActivity(intent);
    }
}
```

> **Note:** Unreferenced refunds are currently only supported through the SmartPOS (Sunmi) Pay App. If the device isn't a Sunmi terminal, or the Sunmi Pay App isn't installed, hide or disable the refund action — SoftPOS does not support refunds yet.

---

## SoftPOS Integration

`com.phonepos.mspsoftposapp` is MultiSafepay's **SoftPOS (Tap to Pay)** Pay App — used on any device that doesn't have a payment kernel, turning the device's own NFC reader into a card terminal (COTS). This includes pure Android/COTS hardware as well as certain Sunmi models without kernel support (e.g. the L3). It's a different integration from SmartPOS: launched with an explicit action/component instead of `getLaunchIntentForPackage`, and it returns its result via `onActivityResult` instead of `onNewIntent`.

### Launching SoftPOS

```java
private static final int REQUEST_CODE_SOFTPOS = 1001;
private static final String SOFTPOS_PACKAGE = "com.phonepos.mspsoftposapp";
private static final String SOFTPOS_ACTION_MANUAL_PAYMENT = "com.phonepos.mspsoftposapp.ACTION_MANUAL_PAYMENT";
private static final String SOFTPOS_COMPONENT = "com.phonepos.mspsoftposapp.ManualPayInputActivity";

Intent softpos = new Intent(SOFTPOS_ACTION_MANUAL_PAYMENT);
softpos.setClassName(SOFTPOS_PACKAGE, SOFTPOS_COMPONENT);

// amount is a decimal string here, NOT a long in minor units
softpos.putExtra("amount", String.format(Locale.US, "%.2f", amountInCents / 100.0));
softpos.putExtra("order_id", getOrderId());
softpos.putExtra("skip_manual_input", true);
softpos.putExtra("package_name", getPackageName());
softpos.putExtra("items", jsonArray.toString()); // optional
softpos.putExtra("callback_activity", getClass().getName());

if (getPackageManager().resolveActivity(softpos, 0) != null) {
    startActivityForResult(softpos, REQUEST_CODE_SOFTPOS);
}
```

Important:
* `amount` is sent as a **decimal string** (e.g. `"1.50"`) — unlike SmartPOS, where `amount` is a `long` in minor units (cents).
* SoftPOS must be launched with `startActivityForResult`, since it returns its result directly rather than calling back via a new `Intent` (see [SoftPOS Callback](#softpos-callback)).
* Always check `resolveActivity(...)` before launching — if it returns `null`, SoftPOS isn't installed.
* Unreferenced refunds are not currently supported on SoftPOS (see [Unreferenced Refund Flow](#unreferenced-refund-flow)).

---

## Recurring / Subscription Payments (Card on File)

To start a card-on-file subscription (the shopper's card is tokenized for future off-session charges), send the same target-app Intent as a normal payment, plus `recurring_model` and `reference` extras.

```java
// SmartPOS (Sunmi)
String packageName = "com.multisafepay.pos.sunmi";
Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");
intent.putExtra("amount", amountInCents);
intent.putExtra("currency", "EUR");
intent.putExtra("order_id", getOrderId());
intent.putExtra("description", "First subscription payment");
intent.putExtra("package_name", getPackageName());
intent.putExtra("recurring_model", "cardOnFile");
intent.putExtra("reference", shopperReference); // links future off-session charges to this payment

startActivity(intent);
```

```java
// SoftPOS
Intent softpos = new Intent(SOFTPOS_ACTION_MANUAL_PAYMENT);
softpos.setClassName(SOFTPOS_PACKAGE, SOFTPOS_COMPONENT);
softpos.putExtra("amount", String.format(Locale.US, "%.2f", amountInCents / 100.0));
softpos.putExtra("currency", "EUR");
softpos.putExtra("order_id", getOrderId());
softpos.putExtra("description", "First subscription payment");
softpos.putExtra("skip_manual_input", true);
softpos.putExtra("package_name", getPackageName());
softpos.putExtra("callback_activity", getClass().getName());
softpos.putExtra("recurring_model", "cardOnFile");
softpos.putExtra("reference", shopperReference);

startActivityForResult(softpos, REQUEST_CODE_SOFTPOS);
```

* `reference` is the shopper reference used to link future off-session charges to this first payment.
* The result of this first payment is delivered through the same callback mechanism as a normal payment — `onNewIntent` for SmartPOS, `onActivityResult` for SoftPOS.
