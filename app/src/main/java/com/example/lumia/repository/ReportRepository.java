package com.example.lumia.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.lumia.models.ObstacleReport;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository handling Obstacle Report persistence and queries with Cloud Firestore
 * for the COMMUNITY module.
 */
public class ReportRepository {

    private static final String TAG = "ReportRepository";
    private static final String COLLECTION_REPORTS = "reports";
    private static ReportRepository instance;

    private final FirebaseFirestore firebaseFirestore;

    public interface ReportCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    public interface NearbyReportsCallback {
        void onSuccess(List<ObstacleReport> reports);
        void onFailure(Exception e);
    }

    public ReportRepository() {
        this.firebaseFirestore = FirebaseFirestore.getInstance();
    }

    public static synchronized ReportRepository getInstance() {
        if (instance == null) {
            instance = new ReportRepository();
        }
        return instance;
    }

    /**
     * Create and save a new obstacle report to Cloud Firestore.
     *
     * @param report   ObstacleReport object containing report details.
     * @param callback Callback to receive creation status.
     */
    public void createReport(ObstacleReport report, final ReportCallback callback) {
        FirebaseUser currentUser = UserRepository.getInstance().getCurrentUser();
        String currentUserId = (currentUser != null) ? currentUser.getUid() : "anonymous_user";

        if (report.getUserId() == null || report.getUserId().isEmpty()) {
            report.setUserId(currentUserId);
        }

        if (report.getTimestamp() <= 0) {
            report.setTimestamp(System.currentTimeMillis());
        }

        DocumentReference docRef = firebaseFirestore.collection(COLLECTION_REPORTS).document();
        report.setId(docRef.getId());

        docRef.set(report)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "createReport:success, docId: " + docRef.getId());
                        if (callback != null) {
                            callback.onSuccess(docRef.getId());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "createReport:failure", e);
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
    }

    /**
     * Module contract method required for cross-module integration (e.g. GO module).
     * Temporarily returns mock data near the specified coordinates and radius.
     *
     * @param lat      Latitude of the target center location.
     * @param lng      Longitude of the target center location.
     * @param radius   Radius in kilometers.
     * @param callback Callback returning nearby reports.
     */
    public void getNearbyReports(double lat, double lng, double radius, NearbyReportsCallback callback) {
        List<ObstacleReport> mockReports = new ArrayList<>();

        long now = System.currentTimeMillis();
        mockReports.add(new ObstacleReport(
                "mock_report_1",
                "Ổ gà",
                "Ổ gà lớn giữa đường nguy hiểm",
                lat + 0.001,
                lng + 0.001,
                now - 3600000,
                "user_mock_1",
                "active"
        ));

        mockReports.add(new ObstacleReport(
                "mock_report_2",
                "Công trình",
                "Rào chắn công trình thi công lề đường",
                lat - 0.002,
                lng - 0.0015,
                now - 7200000,
                "user_mock_2",
                "active"
        ));

        mockReports.add(new ObstacleReport(
                "mock_report_3",
                "Cầu thang",
                "Cầu thang hẹp thiếu tay vịn",
                lat + 0.0005,
                lng - 0.0008,
                now - 1800000,
                "user_mock_3",
                "active"
        ));

        Log.d(TAG, "getNearbyReports: returning " + mockReports.size() + " mock reports for lat=" + lat + ", lng=" + lng);

        if (callback != null) {
            callback.onSuccess(mockReports);
        }
    }
}
