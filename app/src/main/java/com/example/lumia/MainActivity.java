package com.example.lumia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private final FeaturePermissions permissions = new FeaturePermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        findViewById(R.id.btnSettings).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class))
        );

        findViewById(R.id.btnSee).setOnClickListener(view ->
                startActivity(new Intent(this, SeeActivity.class))
        );
        showPendingMessage(R.id.btnOcr);
        findViewById(R.id.btnAlert).setOnClickListener(view ->
                startActivity(new Intent(this, AlertActivity.class))
        );
        findViewById(R.id.btnGo).setOnClickListener(view ->
                startActivity(new Intent(this, NavigationActivity.class))
        );
        showPendingMessage(R.id.btnReport);
        findViewById(R.id.btnCommunity).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class))
        );
        permissions.bindMicrophone(R.id.btnVoice);
    }

    private void showPendingMessage(int buttonId) {
        findViewById(buttonId).setOnClickListener(view ->
                Toast.makeText(this, R.string.feature_pending, Toast.LENGTH_SHORT).show()
        );
    }
}
