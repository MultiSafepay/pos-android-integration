package com.msp.posclientapp;

import org.json.JSONArray;

public interface IProduct {
    void callMSPPayApp(JSONArray jsonArray, long amount);
    void callMSPPayAppECommerce(JSONArray jsonArray, long amount);
}
