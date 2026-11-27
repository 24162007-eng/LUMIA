package com.example.lumia.repository;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lumia.models.ObstacleReport;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public interface ReportsListCallback {
        void onSuccess(List<ObstacleReport> reports);
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
     * Fetch all obstacle reports realtime from Cloud Firestore ordered by timestamp descending.
     *
     * @param callback Callback returning list of reports.
     */
    public void getAllReports(final ReportsListCallback callback) {
        firebaseFirestore.collection(COLLECTION_REPORTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "getAllReports:onEvent failure", error);
                            if (callback != null) {
                                callback.onFailure(error);
                            }
                            return;
                        }

                        List<ObstacleReport> reports = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                ObstacleReport report = doc.toObject(ObstacleReport.class);
                                if (report.getId() == null || report.getId().isEmpty()) {
                                    report.setId(doc.getId());
                                }
                                reports.add(report);
                            }
                        }

                        if (callback != null) {
                            callback.onSuccess(reports);
                        }
                    }
                });
    }

    /**
     * Update status field of an obstacle report on Cloud Firestore.
     *
     * @param reportId  Target report document ID.
     * @param newStatus New status (e.g. "active" or "resolved").
     * @param callback  Callback to receive status update result.
     */
    public void updateReportStatus(String reportId, String newStatus, final ReportCallback callback) {
        if (reportId == null || reportId.isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Report ID cannot be empty"));
            }
            return;
        }

        firebaseFirestore.collection(COLLECTION_REPORTS)
                .document(reportId)
                .update("status", newStatus)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "updateReportStatus:success, docId: " + reportId);
                        if (callback != null) {
                            callback.onSuccess(reportId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "updateReportStatus:failure, docId: " + reportId, e);
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
    }

    /**
     * Module contract method required for cross-module integration (e.g. GO module).
     * Fetches active obstacle reports from Cloud Firestore, calculates distance from
     * target (lat, lng), filters reports within radius, and sorts from nearest to farthest.
     *
     * @param lat      Latitude of target center location.
     * @param lng      Longitude of target center location.
     * @param radius   Radius in kilometers (or meters if > 100).
     * @param callback Callback returning nearby reports sorted by distance.
     */
    public void getNearbyReports(final double lat, final double lng, final double radius, final NearbyReportsCallback callback) {
        firebaseFirestore.collection(COLLECTION_REPORTS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot value) {
                        final List<ObstacleReport> nearbyReports = new ArrayList<>();
                        final double maxDistanceMeters = (radius <= 100.0) ? radius * 1000.0 : radius;

                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                ObstacleReport report = doc.toObject(ObstacleReport.class);
                                if (report.getId() == null || report.getId().isEmpty()) {
                                    report.setId(doc.getId());
                                }

                                float[] results = new float[1];
                                Location.distanceBetween(
                                        lat, lng,
                                        report.getLatitude(), report.getLongitude(),
                                        results
                                );
                                float distanceInMeters = results[0];

                                if (distanceInMeters <= maxDistanceMeters) {
                                    nearbyReports.add(report);
                                }
                            }

                            Collections.sort(nearbyReports, new Comparator<ObstacleReport>() {
                                @Override
                                public int compare(ObstacleReport o1, ObstacleReport o2) {
                                    float[] res1 = new float[1];
                                    float[] res2 = new float[1];
                                    Location.distanceBetween(lat, lng, o1.getLatitude(), o1.getLongitude(), res1);
                                    Location.distanceBetween(lat, lng, o2.getLatitude(), o2.getLongitude(), res2);
                                    return Float.compare(res1[0], res2[0]);
                                }
                            });
                        }

                        Log.d(TAG, "getNearbyReports: returning " + nearbyReports.size() + " active reports within radius " + maxDistanceMeters + "m");
                        if (callback != null) {
                            callback.onSuccess(nearbyReports);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "getNearbyReports:failure", e);
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
    }
}
