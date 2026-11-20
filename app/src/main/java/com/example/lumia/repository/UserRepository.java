package com.example.lumia.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository handling Firebase Authentication and Cloud Firestore operations
 * for the COMMUNITY module.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static UserRepository instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firebaseFirestore;

    // Interface callback for Auth operations
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }

    // Interface callback for Firestore operations
    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    public UserRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseFirestore = FirebaseFirestore.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    /**
     * Get currently logged-in FirebaseUser.
     *
     * @return FirebaseUser or null if not authenticated.
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Sign in anonymously using Firebase Auth for testing foundation.
     *
     * @param callback AuthCallback listener to receive result.
     */
    public void signInAnonymously(AuthCallback callback) {
        firebaseAuth.signInAnonymously()
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Log.d(TAG, "signInAnonymously:success, uid: " + (user != null ? user.getUid() : "null"));
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "signInAnonymously:failure", e);
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
    }

    /**
     * Test Cloud Firestore connection by writing a sample document to "test" collection.
     *
     * @param callback FirestoreCallback listener to receive result.
     */
    public void testFirestoreConnection(FirestoreCallback callback) {
        FirebaseUser currentUser = getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "anonymous_user";

        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("message", "Testing Firebase Firestore connection from LUMIA COMMUNITY module");
        testData.put("userId", userId);
        testData.put("status", "connected");

        firebaseFirestore.collection("test")
                .add(testData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "testFirestoreConnection:success, docId: " + documentReference.getId());
                        if (callback != null) {
                            callback.onSuccess(documentReference.getId());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "testFirestoreConnection:failure", e);
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
    }
}
