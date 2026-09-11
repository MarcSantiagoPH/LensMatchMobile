package com.lensmatch.mobile.service;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.data.ReservationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FirestoreService {
    private static final String TAG = "FirestoreService";
    public static final String COLLECTION_FRAME_CATALOG = "FRAME_CATALOG";
    public static final String COLLECTION_RESERVATIONS = "RESERVATIONS";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    private static FirebaseFirestore getDb() {
        return FirebaseFirestore.getInstance();
    }

    public static void getFrames(Callback<List<FrameModel>> callback) {
        getDb().collection(COLLECTION_FRAME_CATALOG)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FrameModel> frames = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            frames.add(FrameModel.fromMap(data, doc.getId()));
                        }
                    }
                    if (callback != null) callback.onSuccess(frames);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching frames: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void getFramesByStyle(String styleFilter, Callback<List<FrameModel>> callback) {
        if (styleFilter == null || styleFilter.isEmpty() || "All".equalsIgnoreCase(styleFilter)) {
            getFrames(callback);
            return;
        }

        getFrames(new Callback<List<FrameModel>>() {
            @Override
            public void onSuccess(List<FrameModel> allFrames) {
                List<FrameModel> filtered = new ArrayList<>();
                for (FrameModel f : allFrames) {
                    if (f.getFrameStyle() != null && f.getFrameStyle().equalsIgnoreCase(styleFilter.trim())) {
                        filtered.add(f);
                    }
                }
                if (callback != null) callback.onSuccess(filtered);
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) callback.onError(errorMessage);
            }
        });
    }

    public static void createReservation(FrameModel frame, Callback<String> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be logged in to reserve a frame.");
            return;
        }

        if (!frame.isAvailable()) {
            if (callback != null) callback.onError("This frame is currently unavailable for reservation.");
            return;
        }

        final String customerId = user.getUid();
        String name = user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = AppState.getInstance().getUserName();
        }
        final String customerName = (name != null && !name.trim().isEmpty()) ? name : "Customer";

        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = AppState.getInstance().getUserEmail();
        }
        final String customerEmail = email != null ? email : "";

        // Check for existing pending reservation for this authenticated user
        getDb().collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("frameId", frame.getId())
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        if (callback != null) callback.onError("You already have a pending reservation for this frame.");
                        return;
                    }

                    ReservationModel reservation = new ReservationModel(
                            null,
                            customerId,
                            customerName,
                            customerEmail,
                            frame.getId(),
                            frame.getFrameName(),
                            frame.getBrand(),
                            frame.getFrameStyle(),
                            frame.getPriceValue(),
                            frame.getImageUrl(),
                            "Pending",
                            null,
                            null
                    );

                    getDb().collection(COLLECTION_RESERVATIONS)
                            .add(reservation.toMap())
                            .addOnSuccessListener(documentReference -> {
                                AppState.getInstance().addReservation(frame);
                                if (callback != null) callback.onSuccess(documentReference.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to submit reservation: " + e.getMessage(), e);
                                if (callback != null) callback.onError("Failed to save reservation: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking duplicate reservation: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void getUserReservations(Callback<List<ReservationModel>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onSuccess(new ArrayList<>());
            return;
        }

        getDb().collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("customerId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ReservationModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            list.add(ReservationModel.fromMap(data, doc.getId()));
                        }
                    }

                    Collections.sort(list, (a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user reservations: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }
}
