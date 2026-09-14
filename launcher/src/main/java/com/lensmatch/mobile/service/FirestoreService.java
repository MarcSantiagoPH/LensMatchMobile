package com.lensmatch.mobile.service;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ClinicModel;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.data.ReservationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreService {
    private static final String TAG = "FirestoreService";
    public static final String COLLECTION_FRAME_CATALOG = "FRAME_CATALOG";
    public static final String COLLECTION_RESERVATIONS = "RESERVATIONS";
    public static final String COLLECTION_CLINIC_INFORMATION = "CLINIC_INFORMATION";
    public static final String COLLECTION_CUSTOMERS = "CUSTOMERS";
    public static final String DOC_CLINIC_GENERAL = "general";

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

    public static boolean isActiveStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return "PENDING".equals(s) || "APPROVED".equals(s) || "CONFIRMED".equals(s);
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

        // Query user's existing reservations to check duplicates & 5-active-reservation limit
        getDb().collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeCount = 0;
                    boolean hasPendingForFrame = false;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            String fId = data.containsKey("frameId") ? String.valueOf(data.get("frameId")) : "";
                            String status = data.containsKey("status") ? String.valueOf(data.get("status")) : "Pending";

                            if (fId.equals(frame.getId()) && "Pending".equalsIgnoreCase(status.trim())) {
                                hasPendingForFrame = true;
                            }

                            if (isActiveStatus(status)) {
                                activeCount++;
                            }
                        }
                    }

                    if (hasPendingForFrame) {
                        if (callback != null) callback.onError("You already have a pending reservation for this frame.");
                        return;
                    }

                    if (activeCount >= 5) {
                        if (callback != null) {
                            callback.onError("You can have a maximum of 5 active reservations at a time. Please wait for an existing reservation to be completed or cancelled before making another reservation.");
                        }
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
                    Log.e(TAG, "Error checking user reservations: " + e.getMessage(), e);
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

    public static void getClinicInformation(Callback<ClinicModel> callback) {
        getDb().collection(COLLECTION_CLINIC_INFORMATION)
                .document(DOC_CLINIC_GENERAL)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        ClinicModel model = ClinicModel.fromMap(documentSnapshot.getData());
                        if (callback != null) callback.onSuccess(model);
                    } else {
                        if (callback != null) callback.onSuccess(new ClinicModel("Franselle Optical Clinic", "", "", "", ""));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching clinic information: " + e.getMessage(), e);
                    if (callback != null) callback.onSuccess(new ClinicModel("Franselle Optical Clinic", "", "", "", ""));
                });
    }

    public static void updateCustomerProfile(String fullName, String phoneNumber, String address, Callback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be logged in to update your profile.");
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        if (fullName != null) updateData.put("fullName", fullName.trim());
        if (phoneNumber != null) updateData.put("phoneNumber", phoneNumber.trim());
        if (address != null) updateData.put("address", address.trim());

        getDb().collection(COLLECTION_CUSTOMERS)
                .document(user.getUid())
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating customer profile: " + e.getMessage(), e);
                    if (callback != null) callback.onError("Failed to update profile: " + e.getMessage());
                });
    }
}
