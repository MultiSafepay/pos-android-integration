package com.msp.posclientapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProductECommerce {

    private JSONArray jsonArray;
    private IProduct product;

    void setProduct(IProduct product) {
        this.product = product;

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

            System.out.println(jsonArray);


        } catch (JSONException jsonException) {
            Log.e("DEBUGGING_INTENT", "JSONException occurred: " + jsonException.getMessage());
        }

        product.callMSPPayAppECommerce(jsonArray);
    }

}
