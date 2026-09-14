package com.msp.posclientapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    /** SmartPOS on Sunmi devices with payment kernels. */
    private static final String PKG_SMARTPOS = "com.multisafepay.pos.sunmi";

    private MaterialButton sunmiButton;
    private MaterialButton softposButton;
    private TextView subtitleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sunmiButton = findViewById(R.id.sunmipospayment);
        softposButton = findViewById(R.id.softpayment);
        subtitleView = findViewById(R.id.launcher_subtitle);


        boolean useSmartPos = isPackageInstalled(PKG_SMARTPOS);

        if (useSmartPos) {
            sunmiButton.setVisibility(View.VISIBLE);
            softposButton.setVisibility(View.GONE);
            sunmiButton.setText(R.string.pay_with_sunmi_label);
            if (subtitleView != null) {
                subtitleView.setText(R.string.launcher_subtitle_smartpos);
            }
        } else {
            sunmiButton.setVisibility(View.GONE);
            softposButton.setVisibility(View.VISIBLE);
            softposButton.setText(R.string.pay_with_softpos_label);
            if (subtitleView != null) {
                subtitleView.setText(R.string.launcher_subtitle_softpos);
            }
        }

        sunmiButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            startActivity(intent);
        });

        softposButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra("launchSoftPOS", true);
            startActivity(intent);
        });
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.processMSPMiddlewareResponse(intent);
        super.onNewIntent(intent);
    }

    private void processMSPMiddlewareResponse(@NonNull Intent intent) {
        if (intent.hasExtra("status")) {
            int status = intent.getIntExtra("status", 0);
            String message = intent.getStringExtra("message");
            this.handleMiddlewareCallback(status, message);
        }
    }

    private void receivedCallbackIntent(String resultStatus, String message, String description) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("result_status", resultStatus);
        intent.putExtra("message", message);
        intent.putExtra("description", description);
        startActivity(intent);
    }

    private void handleMiddlewareCallback(int status, String message) {
        if (message == null) message = "";
        switch (status) {
            case 875:
                receivedCallbackIntent("EXCEPTION", "EXCEPTION " + message, getString(R.string.exception));
                break;
            case 471:
                receivedCallbackIntent("COMPLETED", "COMPLETED " + message, getString(R.string.completed));
                break;
            case 17:
                receivedCallbackIntent("CANCELLED", "CANCELLED " + message, getString(R.string.cancel));
                break;
            case 88:
                receivedCallbackIntent("DECLINED", "DECLINED " + message, getString(R.string.decline));
                break;
        }
    }
}
