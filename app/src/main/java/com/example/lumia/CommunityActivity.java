package com.example.lumia;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lumia.adapters.ReportAdapter;
import com.example.lumia.models.ObstacleReport;
import com.example.lumia.repository.ReportRepository;
import com.example.lumia.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity handling obstacle report creation, viewing, and status updates in COMMUNITY module.
 */
public class CommunityActivity extends AppCompatActivity {

    private Spinner spinnerObstacleType;
    private EditText edtDescription;
    private EditText edtLatitude;
    private EditText edtLongitude;
    private Button btnGetMockLocation;
    private TextView tvTimestamp;
    private Button btnSubmitReport;

    private RecyclerView rvReports;
    private ProgressBar pbLoadingReports;
    private TextView tvEmptyReports;
    private ReportAdapter reportAdapter;

    private long currentTimestamp;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateTimestamp();
        loadReportsList();
    }

    private void initViews() {
        spinnerObstacleType = findViewById(R.id.spinnerObstacleType);
        edtDescription = findViewById(R.id.edtDescription);
        edtLatitude = findViewById(R.id.edtLatitude);
        edtLongitude = findViewById(R.id.edtLongitude);
        btnGetMockLocation = findViewById(R.id.btnGetMockLocation);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        rvReports = findViewById(R.id.rvReports);
        pbLoadingReports = findViewById(R.id.pbLoadingReports);
        tvEmptyReports = findViewById(R.id.tvEmptyReports);
    }

    private void setupRecyclerView() {
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(new ArrayList<>(), new ReportAdapter.OnReportClickListener() {
            @Override
            public void onReportClick(ObstacleReport report) {
                showReportDetailDialog(report);
            }
        });
        rvReports.setAdapter(reportAdapter);
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

    private void loadReportsList() {
        pbLoadingReports.setVisibility(View.VISIBLE);
        tvEmptyReports.setVisibility(View.GONE);

        ReportRepository.getInstance().getAllReports(new ReportRepository.ReportsListCallback() {
            @Override
            public void onSuccess(List<ObstacleReport> reports) {
                pbLoadingReports.setVisibility(View.GONE);
                if (reports == null || reports.isEmpty()) {
                    tvEmptyReports.setVisibility(View.VISIBLE);
                    rvReports.setVisibility(View.GONE);
                } else {
                    tvEmptyReports.setVisibility(View.GONE);
                    rvReports.setVisibility(View.VISIBLE);
                    reportAdapter.updateData(reports);
                }
            }

            @Override
            public void onFailure(Exception e) {
                pbLoadingReports.setVisibility(View.GONE);
                String errorMsg = e != null ? e.getMessage() : "";
                Toast.makeText(CommunityActivity.this, getString(R.string.toast_report_failure, errorMsg), Toast.LENGTH_SHORT).show();
            }
        });
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
                String errorMsg = e != null ? e.getMessage() : "";
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

    private void showReportDetailDialog(final ObstacleReport report) {
        if (report == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_detail, null);

        TextView tvDetailType = dialogView.findViewById(R.id.tvDetailType);
        TextView tvDetailStatus = dialogView.findViewById(R.id.tvDetailStatus);
        TextView tvDetailDescription = dialogView.findViewById(R.id.tvDetailDescription);
        TextView tvDetailCoordinates = dialogView.findViewById(R.id.tvDetailCoordinates);
        TextView tvDetailTimestamp = dialogView.findViewById(R.id.tvDetailTimestamp);
        TextView tvDetailUserId = dialogView.findViewById(R.id.tvDetailUserId);
        final Button btnMarkResolved = dialogView.findViewById(R.id.btnMarkResolved);
        Button btnCloseDetail = dialogView.findViewById(R.id.btnCloseDetail);

        tvDetailType.setText(getString(R.string.report_detail_type, report.getType() != null ? report.getType() : ""));

        final boolean isResolved = "resolved".equalsIgnoreCase(report.getStatus());
        if (isResolved) {
            tvDetailStatus.setText(getString(R.string.report_status_resolved));
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_resolved);
            tvDetailStatus.setTextColor(Color.parseColor("#123B36"));
            btnMarkResolved.setEnabled(false);
            btnMarkResolved.setAlpha(0.6f);
        } else {
            tvDetailStatus.setText(getString(R.string.report_status_active));
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_active);
            tvDetailStatus.setTextColor(Color.WHITE);
            btnMarkResolved.setEnabled(true);
            btnMarkResolved.setAlpha(1.0f);
        }

        tvDetailDescription.setText(getString(R.string.report_detail_description, report.getDescription() != null ? report.getDescription() : ""));
        tvDetailCoordinates.setText(getString(R.string.report_detail_coordinates, String.valueOf(report.getLatitude()), String.valueOf(report.getLongitude())));

        if (report.getTimestamp() > 0) {
            tvDetailTimestamp.setText(getString(R.string.report_detail_timestamp, dateFormat.format(new Date(report.getTimestamp()))));
        } else {
            tvDetailTimestamp.setText(getString(R.string.report_detail_timestamp, ""));
        }

        tvDetailUserId.setText(getString(R.string.report_detail_user_id, report.getUserId() != null ? report.getUserId() : ""));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCloseDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnMarkResolved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResolved) {
                    Toast.makeText(CommunityActivity.this, getString(R.string.already_resolved), Toast.LENGTH_SHORT).show();
                    return;
                }
                btnMarkResolved.setEnabled(false);
                ReportRepository.getInstance().updateReportStatus(report.getId(), "resolved", new ReportRepository.ReportCallback() {
                    @Override
                    public void onSuccess(String documentId) {
                        Toast.makeText(CommunityActivity.this, getString(R.string.toast_status_update_success), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        btnMarkResolved.setEnabled(true);
                        String errorMsg = e != null ? e.getMessage() : "";
                        Toast.makeText(CommunityActivity.this, getString(R.string.toast_status_update_failure, errorMsg), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
    }
}
