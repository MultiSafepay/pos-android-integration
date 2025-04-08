<p align="center">
  <img src="https://www.multisafepay.com/img/multisafepaylogo.svg" width="400px" position="center">
</p>

# pos-android-integration

## How to send request to Multisafepay Pay App from 3rd party App via "intent" ##

### Multisafepay Pay App packages: ### 

[Click here](https://developer.android.com/reference/android/content/pm/PackageManager) for PackageManager reference.

"com.multisafepay.pos.nokernels" - nonkernel (non-POS devices)
"com.multisafepay.pos.sunmi" - sunmi (POS devices w/ kernel)

### a. Setting up Manifest file ### 

```    
<manifest
 
...
...
 
    <queries>
        <package android:name="com.multisafepay.pos.nokernels" />
        <package android:name="com.multisafepay.pos.sunmi" />
    </queries>  
 
...
...
 
</manifest>

``` 

### b. Process callback ###

[Click here](https://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)) for onNewIntent reference.

``` 

 @Override
    protected void onNewIntent(Intent intent) {
        //on waking up from callback process results
        //this intent is only called if target App (Pay App) is properly finalized.

        this.processMSPMiddlewareResponse(intent);
        super.onNewIntent(intent);
    }
    
    private void processMSPMiddlewareResponse(@NonNull Intent intent) {
        //retrieve intent extra data including message.
        if (intent.hasExtra("status")) {
            int status = intent.getIntExtra("status", 0);
            String message = intent.getStringExtra("message");
            this.handleMiddlewareCallback(status, message);
        }
    }
 
    private void handleMiddlewareCallback(int status, String message) {
        switch (status) {
            case 875: {
                this.receivedCallbackIntent(String.format("EXCEPTION", message));
            }
            break;
            case 471: {
                this.receivedCallbackIntent(String.format("COMPLETED", message));
            }
            break;
            case 17: {
                this.receivedCallbackIntent(String.format("CANCELLED", message));
            }
            break;
            case 88: {
                this.receivedCallbackIntent(String.format("DECLINED", message));
            }
            break;
        }
        
          private void receivedCallbackIntent(String message) {
             Intent intent = new Intent(this, PaymentActivity.class);
             intent.putExtra("message",message);
             intent.putExtra("description","pass data to this Activity");
              startActivity(intent);
        }
   }

   
``` 

## Legacy App-to-App flow (non-e-commerce) ##

### a. Order items: ###

``` 
try {
            jsonArray = new JSONArray();
            JSONObject jsonObject;
 
             {
                jsonObject = new JSONObject();
                jsonObject.put("name", "Product 1");
                jsonObject.put("unit_price", "0.10");
                jsonObject.put("quantity", "1");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "3.90");
                jsonArray.put(jsonObject);
            }

            {
                jsonObject = new JSONObject();
                jsonObject.put("name", "Product 2");
                jsonObject.put("unit_price", "0.20");
                jsonObject.put("quantity", "1");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "1.40");
                jsonArray.put(jsonObject);
            }
 
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
}

``` 

### b. Calling Multisafepay Pay App from 3rd party App ###

``` 

         
           Intent intent = this.context.getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");

           String packageName = intent.getPackage();
           intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

           // Note: The field 'Amount' is required to process the transaction. The data type for 'Amount' should be long (L).
 
            if (intent != null) {
               intent.putExtra("items", jsonArray.toString()); // Order items
               intent.putExtra("order_id", getOrderId()); //getOrderId() for testing ONLY, for PRODUCTION place here your order_id
               intent.putExtra("order_description", "info about the order");
               intent.putExtra("currency", "EUR");
               intent.putExtra("amount", amount);   // this is new from version v1.0.8
               intent.putExtra("package_name", this.context.getPackageName()); // Callback packagename
               this.context.startActivity(intent);
           }

``` 

## New e-commerce flow ##

### a. Order items: ###

``` 
 try {
            jsonArray = new JSONArray();

            // First Item: Socks
            JSONObject socks = new JSONObject();
            socks.put("name", "Socks");
            socks.put("description", "One pair of black socks");
            socks.put("merchant_item_id", "001-M");
            socks.put("unit_price", 0.105785124);
            socks.put("quantity", 3);

            //Note: If the shipping cost is taxed, add a tax_table_selector to the shipping item.
            socks.put("tax_table_selector", "21_percent");

            //TODO: Optional fields
            /* JSONObject weight = new JSONObject();
            weight.put("unit", "G");
            weight.put("value", "120");
            socks.put("weight", weight);*/

            jsonArray.put(socks);

            // Second Item: Shipping
            JSONObject shipping = new JSONObject();
            shipping.put("name", "Shipping");
            shipping.put("description", "Domestic shipping (zone 1)");
            shipping.put("merchant_item_id", "msp-shipping");
            shipping.put("unit_price", 0.15);
            shipping.put("quantity", 1);

            jsonArray.put(shipping);


        } catch (JSONException jsonException) {
            Log.e("DEBUGGING_INTENT", "JSONException occurred: " + jsonException.getMessage());
        }

``` 

### b. Calling Multisafepay Pay App from 3rd party App ###

``` 
           Intent intent = this.context.getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");

           String packageName = intent.getPackage();
           intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

           // Call the setCheckoutOptions method to set the checkout options
                setCheckoutOptions(intent);

            // Note: The field 'Amount' is required to process the transaction. The data type for 'Amount' should be long (L).
 
            if (intent != null) {
                intent.putExtra("items", basket.toString());
                intent.putExtra("order_id", getOrderId());
                intent.putExtra("description", "info about the order");
                intent.putExtra("currency", "EUR");
                intent.putExtra("amount", amount);
                intent.putExtra("reference", "Ref-intent-1234-dani");
                intent.putExtra("auto_close", false);
                intent.putExtra("package_name", getPackageName());
           }

``` 

### c. set checkout options method ###

``` 
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

``` 
# Unreferenced Refund flow (App-to-App)

**a. Setting up Manifest file**

In **your** app (third-party), declare the queries so you can find the Pay App:

```xml
<manifest>
  
    <queries>
        <package android:name="com.multisafepay.pos.nokernels" />
        <package android:name="com.multisafepay.pos.sunmi" />
    </queries>
   
</manifest>

```

> FYI: in the MultiSafepay Pay App there is an intent-filter listening to this action:
>

```xml

<activity android:name=".IntentActivity" android:exported="true">
    <intent-filter>
        <action android:name="com.multisafepay.ACTION_UNREFERENCED_REFUND" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>

```

---

**b. Sending Unreferenced Refund request**

```java


private void sendRefundIntent(long amountInCents) {

    Intent intent = this.getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
    if (intent != null) {

        String packageName = intent.getPackage();

        intent.setClassName(
                packageName, "com.multisafepay.pos.middleware.IntentActivity"
        );

        intent.putExtra("order_id", "REFUND" + System.currentTimeMillis());
        intent.putExtra("amount", amountInCents);        
        intent.putExtra("refund", true);                
        intent.putExtra("package_name", this.getPackageName()); 

        this.startActivity(intent);
    }
}


```

**Notes**

- `amount` **must** be a `long` in cents (minor units).
- `package_name` **must** be your app’s package so the Pay App can return the callback.
- The **Unreferenced refund** feature must be **enabled** in the Pay App settings on the terminal.

---

**c. Process callback**

Reuse the same callback flow used for payments:

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
        case 875: // EXCEPTION
            receivedCallbackIntent("EXCEPTION" + (message != null ? (" " + message) : ""));
            break;
        case 471: // COMPLETED
            receivedCallbackIntent("COMPLETED" + (message != null ? (" " + message) : ""));
            break;
        case 17:  // CANCELLED
            receivedCallbackIntent("CANCELLED" + (message != null ? (" " + message) : ""));
            break;
        case 88:  // DECLINED
            receivedCallbackIntent("DECLINED" + (message != null ? (" " + message) : ""));
            break;
    }
}

private void receivedCallbackIntent(String message) {
    Intent i = new Intent(this, PaymentActivity.class);
    i.putExtra("message", message);
    i.putExtra("description", "pass data to this Activity");
    startActivity(i);
}

```
## **Subscribed / Recurring Payment**

**Purpose:** Start the **first** subscription transaction directly from the terminal (tokenization).

Subsequent charges are handled by your **backend via API** using the stored token.

---

### **Flow**

1. User selects “Recurring Payment” in your app.
2. Prompt for:
    - `Amount` (in EUR)
    - `Shopper reference` (stable customer ID in your system)
3. Build and send intent with:
    - Base payment fields
    - `recurring_model` = `"cardOnFile"`
    - `reference` = shopper reference

---



```java

private void launchFirstSubscription(long amountCents, String shopperReference) {
    Intent launch = getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
    if (launch == null) launch = getPackageManager().getLaunchIntentForPackage("com.phonepos.mspsoftposapp");
    if (launch == null) {
        Toast.makeText(this, "MSP Pay App not found (Sunmi/SoftPOS).", Toast.LENGTH_LONG).show();
        return;
    }

    String packageName = launch.getPackage();
    launch.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");

 
    launch.putExtra("amount", amountCents);
    launch.putExtra("currency", "EUR");
    launch.putExtra("order_id", getOrderId()); 
    launch.putExtra("description", "First subscription payment (App-to-App)");
    launch.putExtra("package_name", getPackageName());

  
    launch.putExtra("recurring_model", "cardOnFile");
    launch.putExtra("reference", shopperReference);

    startActivity(launch);
}

```

---

### **Notes**

- **amount** must be sent as `long` in **cents.**
- **shopperReference** must remain constant for the same customer to allow future recurring payments.
- If the “Recurring Payment” toggle in the Pay App is **off**, the payment may be processed as normal (no token) or rejected depending on version/policy.
- Callback handling is identical to normal payments — check `status` in `onNewIntent()`.
