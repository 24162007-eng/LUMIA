package com.example.lumia;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

abstract class FeatureActivity extends AppCompatActivity {
    protected void showFeature(@LayoutRes int layout) {
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(layout);

        View toolbar = findViewById(R.id.featureToolbar);
        int toolbarTop = toolbar.getPaddingTop();
        int toolbarHeight = toolbar.getMinimumHeight();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.featureRoot), (view, insets) -> {
            Insets safeArea = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safeArea.left, 0, safeArea.right, safeArea.bottom);
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarTop + safeArea.top,
                    toolbar.getPaddingRight(), 0);
            toolbar.setMinimumHeight(toolbarHeight + safeArea.top);
            return insets;
        });
        findViewById(R.id.btnFeatureBack).setOnClickListener(view -> finish());
    }

    protected void showPendingMessage(@IdRes int buttonId) {
        findViewById(buttonId).setOnClickListener(view ->
                Toast.makeText(this, R.string.feature_pending, Toast.LENGTH_SHORT).show()
        );
    }
}
