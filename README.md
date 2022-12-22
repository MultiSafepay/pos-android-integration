<p align="center">
  <img src="https://www.multisafepay.com/img/multisafepaylogo.svg" width="400px" position="center">
</p>

# pos-android-integration 

## How to send request to Multisafepay Pay App from 3rd party App via "intent" ##

### a. Multisafepay Pay App packages: ### 

[Click here](https://developer.android.com/reference/android/content/pm/PackageManager) for PackageManager reference.

"com.multisafepay.pos.nokernels" - nonkernel (non-POS devices)
"com.multisafepay.pos.sunmi" - sunmi (POS devices w/ kernel)

### a.1 Setting up Manifest file ### 

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

### a.2 Calling Multisafepay Pay App from 3rd party App ###

``` 

         //Intent intent = this.context.getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.nokernels");
           Intent intent = this.context.getPackageManager().getLaunchIntentForPackage("com.multisafepay.pos.sunmi");
 
            if (intent != null) {
               intent.putExtra("items", jsonArray.toString()); // Order items
               intent.putExtra("order_id", "POSPayApp: " + Math.random()); //Math.random for testing ONLY
               intent.putExtra("order_description", "info about the order");
               intent.putExtra("currency", "EUR");
               intent.putExtra("package_name", this.context.getPackageName()); // Callback packagename
               this.context.startActivity(intent);
           }

``` 

### a.3 Calling Multisafepay Pay App via deep-link - Using Webhook ###

``` 

        
    msp://?amount=123&order_id=123xyz&callback=http://192.168.xx.xx/notification_url=https://www.example.com/paymentnotification/?order_id={$order_id}&status={$status}


``` 

### b. Order items: ###

``` 
try {
            jsonArray = new JSONArray();
            JSONObject jsonObject;
 
            {
                jsonObject = new JSONObject();
                jsonObject.put("name", "Product 1");
                jsonObject.put("unit_price", "28.40");
                jsonObject.put("quantity", "1");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "3.90");
                jsonArray.put(jsonObject);
            }
 
            {
                jsonObject = new JSONObject();
                jsonObject.put("name", "Product 2");
                jsonObject.put("unit_price", "20");
                jsonObject.put("quantity", "10");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "1.40");
                jsonArray.put(jsonObject);
            }
 
            {
                jsonObject = new JSONObject();
                jsonObject.put("name", "Product 3");
                jsonObject.put("unit_price", "10");
                jsonObject.put("quantity", "4");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "0.40");
                jsonArray.put(jsonObject);
            }
 
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
}

``` 


### c. Process callback ###

[Click here](https://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)) for onNewIntent reference.

``` 

 @Override
   protected void onNewIntent(Intent intent) {
       super.onNewIntent(intent);
            
       //on waking up from callback process results
       //this intent is only called if target App (Pay App) is properly finalized.
        
           if (intent.hasExtra("status")) {
           int status = intent.getIntExtra("status", 0);
           String message = intent.getStringExtra("message");
           this.handleMiddlewareCallback(status, message);
       }
   }
 
    
   private void handleMiddlewareCallback(int status, String message) {
       switch (status) {
           case 875: {
               this.showDialog("MSP Middleware EXCEPTION");
           }
           break;
           case 471: {
               this.showDialog("MSP Middleware OK");
           }
           break;
           case 17: {
               this.showDialog("MSP Middleware CANCELLED");
           }
           break;
           case 88: {
               this.showDialog("MSP Middleware DECLINED");
           }
           break;
       }
   }

``` 