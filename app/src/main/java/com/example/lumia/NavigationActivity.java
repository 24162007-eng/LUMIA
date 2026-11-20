package com.example.lumia;

import android.os.Bundle;

public class NavigationActivity extends FeatureActivity {
    private final FeaturePermissions permissions = new FeaturePermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFeature(R.layout.activity_navigation);
        permissions.bindMicrophone(R.id.btnDestinationVoice);
        permissions.bindLocation(R.id.btnCurrentLocation);
        showPendingMessage(R.id.btnRepeatDirections);
        showPendingMessage(R.id.btnStopNavigation);
    }
}
