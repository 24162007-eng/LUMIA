package com.example.lumia;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lumia.models.ObstacleReport;
import com.example.lumia.repository.ReportRepository;
import com.example.lumia.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity handling obstacle report creation in COMMUNITY module.
 */
public class CommunityActivity extends AppCompatActivity {

    private Spinner spinnerObstacleType;
    private EditText edtDescription;
    private EditText edtLatitude;
    private EditText edtLongitude;
    private Button btnGetMockLocation;
    private TextView tvTimestamp;
    private Button btnSubmitReport;

    private long currentTimestamp;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        initViews();
        setupListeners();
        updateTimestamp();
    }

    private void initViews() {
        spinnerObstacleType = findViewById(R.id.spinnerObstacleType);
        edtDescription = findViewById(R.id.edtDescription);
        edtLatitude = findViewById(R.id.edtLatitude);
        edtLongitude = findViewById(R.id.edtLongitude);
        btnGetMockLocation = findViewById(R.id.btnGetMockLocation);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
    }

    private void setupListeners() {
        btnGetMockLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Populate mock coordinates for HCMUTE test location
                edtLatitude.setText("10.8505");
                edtLongitude.setText("106.7720");
                updateTimestamp();
            }
        });

        btnSubmitReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });
    }

    private void updateTimestamp() {
        currentTimestamp = System.currentTimeMillis();
        String formattedDate = dateFormat.format(new Date(currentTimestamp));
        tvTimestamp.setText(getString(R.string.timestamp_label, formattedDate));
    }

    private void submitReport() {
        String description = edtDescription.getText().toString().trim();
        String latStr = edtLatitude.getText().toString().trim();
        String lngStr = edtLongitude.getText().toString().trim();

        if (TextUtils.isEmpty(description) || TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lngStr)) {
            Toast.makeText(this, getString(R.string.toast_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude;
        double longitude;

        try {
            latitude = Double.parseDouble(latStr);
            longitude = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.toast_invalid_coordinates), Toast.LENGTH_SHORT).show();
            return;
        }

        String obstacleType = spinnerObstacleType.getSelectedItem() != null
                ? spinnerObstacleType.getSelectedItem().toString()
                : getString(R.string.obstacle_type_other);

        FirebaseUser user = UserRepository.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "anonymous_user";

        updateTimestamp();

        ObstacleReport report = new ObstacleReport(
                null,
                obstacleType,
                description,
                latitude,
                longitude,
                currentTimestamp,
                userId,
                "active"
        );

        btnSubmitReport.setEnabled(false);

        ReportRepository.getInstance().createReport(report, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(String documentId) {
                btnSubmitReport.setEnabled(true);
                Toast.makeText(CommunityActivity.this, getString(R.string.toast_report_success), Toast.LENGTH_LONG).show();
                clearForm();
            }

            @Override
            public void onFailure(Exception e) {
                btnSubmitReport.setEnabled(true);
                String errorMsg = e != null ? e.getMessage() : "Unknown error";
                Toast.makeText(CommunityActivity.this, getString(R.string.toast_report_failure, errorMsg), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearForm() {
        edtDescription.setText("");
        edtLatitude.setText("");
        edtLongitude.setText("");
        spinnerObstacleType.setSelection(0);
        updateTimestamp();
    }
}
