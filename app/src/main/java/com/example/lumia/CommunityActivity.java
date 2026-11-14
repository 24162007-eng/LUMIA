package com.example.lumia;

import android.os.Bundle;

public class CommunityActivity extends FeatureActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFeature(R.layout.activity_community);
        showPendingMessage(R.id.btnSendReport);
    }
}
