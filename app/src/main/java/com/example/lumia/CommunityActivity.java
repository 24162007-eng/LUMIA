package com.example.lumia;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lumia.adapters.ReportAdapter;
import com.example.lumia.models.ObstacleReport;
import com.example.lumia.repository.ReportRepository;
import com.example.lumia.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity handling obstacle report creation, viewing, real GPS integration,
 * image attachments, and status updates in COMMUNITY module.
 */
public class CommunityActivity extends AppCompatActivity {

    private Spinner spinnerObstacleType;
    private EditText edtDescription;
    private EditText edtLatitude;
    private EditText edtLongitude;
    private Button btnGetGpsLocation;
    private Button btnPickImage;
    private Button btnRemoveImage;
    private ImageView ivReportPreview;
    private TextView tvTimestamp;
    private Button btnSubmitReport;

    private Spinner spinnerFilterStatus;
    private RecyclerView rvReports;
    private ProgressBar pbLoadingReports;
    private TextView tvEmptyReports;
    private ReportAdapter reportAdapter;

    private List<ObstacleReport> masterReportsList = new ArrayList<>();
    private String selectedImageBase64 = null;
    private double currentLatitude = 10.8505; // Default HCMUTE mock
    private double currentLongitude = 106.7720;

    private long currentTimestamp;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        setupActivityLaunchers();
        initViews();
        setupRecyclerView();
        setupListeners();
        updateTimestamp();
        loadReportsList();
    }

    private void setupActivityLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                new ActivityResultCallback<Bitmap>() {
                    @Override
                    public void onActivityResult(Bitmap bitmap) {
                        if (bitmap != null) {
                            processAndSetImage(bitmap);
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            try {
                                InputStream is = getContentResolver().openInputStream(uri);
                                Bitmap bitmap = BitmapFactory.decodeStream(is);
                                if (is != null) is.close();
                                if (bitmap != null) {
                                    processAndSetImage(bitmap);
                                }
                            } catch (Exception e) {
                                Toast.makeText(CommunityActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<java.util.Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(java.util.Map<String, Boolean> result) {
                        Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                            fetchRealGpsLocation();
                        } else {
                            Toast.makeText(CommunityActivity.this, getString(R.string.gps_permission_denied), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void initViews() {
        spinnerObstacleType = findViewById(R.id.spinnerObstacleType);
        edtDescription = findViewById(R.id.edtDescription);
        edtLatitude = findViewById(R.id.edtLatitude);
        edtLongitude = findViewById(R.id.edtLongitude);
        btnGetGpsLocation = findViewById(R.id.btnGetGpsLocation);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        ivReportPreview = findViewById(R.id.ivReportPreview);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus);
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
        btnGetGpsLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestGpsLocation();
            }
        });

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog();
            }
        });

        btnRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelectedImage();
            }
        });

        btnSubmitReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });

        spinnerFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyReportFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.dialog_select_camera), getString(R.string.dialog_select_gallery)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_image_title))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            cameraLauncher.launch(null);
                        } else {
                            galleryLauncher.launch("image/*");
                        }
                    }
                })
                .show();
    }

    private void processAndSetImage(Bitmap originalBitmap) {
        if (originalBitmap == null) return;

        // Resize bitmap to max side 600px
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int maxSide = Math.max(width, height);
        Bitmap resizedBitmap = originalBitmap;
        if (maxSide > 600) {
            float scale = 600f / maxSide;
            resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, Math.round(width * scale), Math.round(height * scale), true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        ivReportPreview.setImageBitmap(resizedBitmap);
        ivReportPreview.setVisibility(View.VISIBLE);
        btnRemoveImage.setVisibility(View.VISIBLE);
    }

    private void clearSelectedImage() {
        selectedImageBase64 = null;
        ivReportPreview.setImageDrawable(null);
        ivReportPreview.setVisibility(View.GONE);
        btnRemoveImage.setVisibility(View.GONE);
    }

    private void requestGpsLocation() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            fetchRealGpsLocation();
        }
    }

    private void fetchRealGpsLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                Toast.makeText(this, getString(R.string.gps_error), Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location bestLocation = null;

            if (isGpsEnabled && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (bestLocation == null && isNetworkEnabled && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (bestLocation != null) {
                currentLatitude = bestLocation.getLatitude();
                currentLongitude = bestLocation.getLongitude();
                edtLatitude.setText(String.format(Locale.US, "%.6f", currentLatitude));
                edtLongitude.setText(String.format(Locale.US, "%.6f", currentLongitude));
                updateTimestamp();
                Toast.makeText(this, getString(R.string.gps_success), Toast.LENGTH_SHORT).show();
            } else {
                // Request a single fresh update if last known is null
                String provider = isGpsEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        edtLatitude.setText(String.format(Locale.US, "%.6f", currentLatitude));
                        edtLongitude.setText(String.format(Locale.US, "%.6f", currentLongitude));
                        updateTimestamp();
                        Toast.makeText(CommunityActivity.this, getString(R.string.gps_success), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                }, null);
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.gps_error), Toast.LENGTH_SHORT).show();
        }
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
                masterReportsList = (reports != null) ? reports : new ArrayList<>();
                applyReportFilter(spinnerFilterStatus.getSelectedItemPosition());
            }

            @Override
            public void onFailure(Exception e) {
                pbLoadingReports.setVisibility(View.GONE);
                String errorMsg = e != null ? e.getMessage() : "";
                Toast.makeText(CommunityActivity.this, getString(R.string.toast_report_failure, errorMsg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyReportFilter(int filterPosition) {
        if (masterReportsList == null || masterReportsList.isEmpty()) {
            tvEmptyReports.setVisibility(View.VISIBLE);
            rvReports.setVisibility(View.GONE);
            return;
        }

        List<ObstacleReport> filteredList = new ArrayList<>();

        if (filterPosition == 1) { // Only Active
            for (ObstacleReport r : masterReportsList) {
                if ("active".equalsIgnoreCase(r.getStatus())) {
                    filteredList.add(r);
                }
            }
        } else if (filterPosition == 2) { // Only Resolved
            for (ObstacleReport r : masterReportsList) {
                if ("resolved".equalsIgnoreCase(r.getStatus())) {
                    filteredList.add(r);
                }
            }
        } else if (filterPosition == 3) { // Nearby active (< 5km)
            double userLat = currentLatitude;
            double userLng = currentLongitude;
            try {
                if (!TextUtils.isEmpty(edtLatitude.getText())) {
                    userLat = Double.parseDouble(edtLatitude.getText().toString().trim());
                }
                if (!TextUtils.isEmpty(edtLongitude.getText())) {
                    userLng = Double.parseDouble(edtLongitude.getText().toString().trim());
                }
            } catch (Exception ignored) {}

            final double centerLat = userLat;
            final double centerLng = userLng;

            for (ObstacleReport r : masterReportsList) {
                if ("active".equalsIgnoreCase(r.getStatus())) {
                    float[] results = new float[1];
                    Location.distanceBetween(centerLat, centerLng, r.getLatitude(), r.getLongitude(), results);
                    if (results[0] <= 5000) { // 5km
                        filteredList.add(r);
                    }
                }
            }

            Collections.sort(filteredList, new Comparator<ObstacleReport>() {
                @Override
                public int compare(ObstacleReport o1, ObstacleReport o2) {
                    float[] r1 = new float[1];
                    float[] r2 = new float[1];
                    Location.distanceBetween(centerLat, centerLng, o1.getLatitude(), o1.getLongitude(), r1);
                    Location.distanceBetween(centerLat, centerLng, o2.getLatitude(), o2.getLongitude(), r2);
                    return Float.compare(r1[0], r2[0]);
                }
            });
        } else { // All
            filteredList.addAll(masterReportsList);
        }

        if (filteredList.isEmpty()) {
            tvEmptyReports.setVisibility(View.VISIBLE);
            rvReports.setVisibility(View.GONE);
        } else {
            tvEmptyReports.setVisibility(View.GONE);
            rvReports.setVisibility(View.VISIBLE);
            reportAdapter.updateData(filteredList);
        }
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
                "active",
                selectedImageBase64
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
        clearSelectedImage();
        updateTimestamp();
    }

    private void showReportDetailDialog(final ObstacleReport report) {
        if (report == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_detail, null);

        TextView tvDetailType = dialogView.findViewById(R.id.tvDetailType);
        TextView tvDetailStatus = dialogView.findViewById(R.id.tvDetailStatus);
        ImageView ivDetailImage = dialogView.findViewById(R.id.ivDetailImage);
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

        String imageBase64 = report.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (bitmap != null) {
                    ivDetailImage.setImageBitmap(bitmap);
                    ivDetailImage.setVisibility(View.VISIBLE);
                } else {
                    ivDetailImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                ivDetailImage.setVisibility(View.GONE);
            }
        } else {
            ivDetailImage.setVisibility(View.GONE);
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
