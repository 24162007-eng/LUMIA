package com.example.lumia;

import android.os.Bundle;

public class SeeActivity extends FeatureActivity {
    private final FeaturePermissions permissions = new FeaturePermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFeature(R.layout.activity_see);
        permissions.bindCamera(R.id.btnCapture);
        showPendingMessage(R.id.btnReadText);
        showPendingMessage(R.id.btnRepeatRecognition);
    }
}
