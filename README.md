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

### b. Calling Multisafepay Pay App from 3rd party App ###


``` 

         
           Intent intent = this.context.getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");

           String packageName = intent.getPackage();
           intent.setClassName(packageName, "com.multisafepay.pos.middleware.IntentActivity");
 
            if (intent != null) {
               intent.putExtra("items", jsonArray.toString()); // Order items
               intent.putExtra("order_id", getOrderId()); //getOrderId() for testing ONLY, for PRODUCTION place here your order_id
               intent.putExtra("order_description", "info about the order");
               intent.putExtra("currency", "EUR");
               intent.putExtra("package_name", this.context.getPackageName()); // Callback packagename
               this.context.startActivity(intent);
           }

``` 

### c. Order items: ###


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


### d. Process callback ###

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