package com.msp.posclientapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProductECommerce {

    private JSONArray jsonArray;

    void setProduct(IProduct product, long amount) { // Ensure amount is passed as a parameter
        try {
            jsonArray = new JSONArray();
            JSONObject jsonObject;

            {
                jsonObject = new JSONObject();
                jsonObject.put("name", "E-Commerce Product 1");
                jsonObject.put("unit_price", "0.10");
                jsonObject.put("quantity", "1");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "0.10");
                jsonArray.put(jsonObject);
            }

            {
                jsonObject = new JSONObject();
                jsonObject.put("name", "E-Commerce Product 2");
                jsonObject.put("unit_price", "0.20");
                jsonObject.put("quantity", "1");
                jsonObject.put("merchant_item_id", "749857");
                jsonObject.put("tax", "0.21");
                jsonArray.put(jsonObject);
            }

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

        // Ensure the user-entered amount is passed here
        product.callMSPPayAppECommerce(jsonArray, amount);
    }
}
