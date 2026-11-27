package com.example.lumia;

import android.os.Bundle;

public class AlertActivity extends FeatureActivity {
    private final FeaturePermissions permissions = new FeaturePermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFeature(R.layout.activity_alert);
        permissions.bindCamera(R.id.btnStartAlert);
        showPendingMessage(R.id.btnStopAlert);
    }
}
