package com.msp.posclientapp;

import org.json.JSONArray;

public interface IProduct {
    void callMSPPayApp(JSONArray jsonArray);
    void callMSPPayAppECommerce(JSONArray jsonArray);
}
