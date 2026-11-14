package com.example.lumia;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

final class FeaturePermissions {
    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String[]> launcher;

    FeaturePermissions(AppCompatActivity activity) {
        this.activity = activity;
        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> showMessage(result.containsValue(true)
                        ? R.string.permission_granted_pending : R.string.permission_denied)
        );
    }

    void bindCamera(@IdRes int buttonId) {
        bind(buttonId, R.string.permission_camera_reason, Manifest.permission.CAMERA);
    }

    void bindMicrophone(@IdRes int buttonId) {
        bind(buttonId, R.string.permission_microphone_reason, Manifest.permission.RECORD_AUDIO);
    }

    void bindLocation(@IdRes int buttonId) {
        bind(buttonId, R.string.permission_location_reason,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void bind(@IdRes int buttonId, @StringRes int rationale, String... permissions) {
        activity.findViewById(buttonId).setOnClickListener(view -> request(rationale, permissions));
    }

    private void request(@StringRes int rationale, String[] permissions) {
        boolean explain = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                showMessage(R.string.feature_pending);
                return;
            }
            explain |= activity.shouldShowRequestPermissionRationale(permission);
        }

        if (explain) {
            new AlertDialog.Builder(activity)
                    .setMessage(rationale)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.permission_continue,
                            (dialog, which) -> launcher.launch(permissions))
                    .show();
        } else {
            launcher.launch(permissions);
        }
    }

    private void showMessage(@StringRes int message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
