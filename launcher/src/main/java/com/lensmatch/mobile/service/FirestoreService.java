package com.lensmatch.mobile.service;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.lensmatch.mobile.data.AnnouncementModel;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ClinicModel;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.data.ReservationModel;
import com.lensmatch.mobile.data.ScanModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreService {
    private static final String TAG = "FirestoreService";
    public static final String COLLECTION_FRAME_CATALOG = "FRAME_CATALOG";
    public static final String COLLECTION_RESERVATIONS = "RESERVATIONS";
    public static final String COLLECTION_CLINIC_INFORMATION = "CLINIC_INFORMATION";
    public static final String COLLECTION_CUSTOMERS = "CUSTOMERS";
    public static final String COLLECTION_SCAN_HISTORY = "SCAN_HISTORY";
    public static final String COLLECTION_ANNOUNCEMENTS = "ANNOUNCEMENTS";
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
        return "PENDING".equals(s) || "APPROVED".equals(s);
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
                    boolean hasActiveForFrame = false;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            String fId = data.containsKey("frameId") ? String.valueOf(data.get("frameId")) : "";
                            String status = data.containsKey("status") ? String.valueOf(data.get("status")) : "Pending";

                            if (fId.equals(frame.getId()) && isActiveStatus(status)) {
                                hasActiveForFrame = true;
                            }

                            if (isActiveStatus(status)) {
                                activeCount++;
                            }
                        }
                    }

                    if (hasActiveForFrame) {
                        if (callback != null) callback.onError("You already have an active reservation for this frame.");
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

    public static void cancelReservation(String reservationId, String cancellationReason, Callback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be logged in to cancel a reservation.");
            return;
        }

        if (reservationId == null || reservationId.trim().isEmpty()) {
            if (callback != null) callback.onError("Invalid reservation ID.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Cancelled");
        updates.put("statusUpdatedAt", FieldValue.serverTimestamp());
        if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
            updates.put("cancellationReason", cancellationReason.trim());
        }

        getDb().collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to cancel reservation: " + e.getMessage(), e);
                    if (callback != null) callback.onError("Failed to cancel reservation: " + e.getMessage());
                });
    }

    public static void cancelReservation(String reservationId, Callback<Void> callback) {
        cancelReservation(reservationId, null, callback);
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
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        com.google.firebase.auth.UserProfileChangeRequest profileUpdates =
                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName.trim())
                                        .build();
                        user.updateProfile(profileUpdates);
                    }
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating customer profile: " + e.getMessage(), e);
                    if (callback != null) callback.onError("Failed to update profile: " + e.getMessage());
                });
    }

    public static void loadCustomerProfile(Callback<Map<String, Object>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("Not authenticated");
            return;
        }

        getDb().collection(COLLECTION_CUSTOMERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> data = documentSnapshot.getData();
                        String fullName = data.containsKey("fullName") && data.get("fullName") != null
                                ? String.valueOf(data.get("fullName")).trim() : "";
                        String phone = data.containsKey("phoneNumber") && data.get("phoneNumber") != null
                                ? String.valueOf(data.get("phoneNumber")).trim() : "";
                        String address = data.containsKey("address") && data.get("address") != null
                                ? String.valueOf(data.get("address")).trim() : "";
                        String email = data.containsKey("email") && data.get("email") != null
                                ? String.valueOf(data.get("email")).trim() : (user.getEmail() != null ? user.getEmail() : "");

                        if (!fullName.isEmpty()) {
                            AppState.getInstance().updateAccountDetails(fullName, phone, address, email);
                        }
                        if (callback != null) callback.onSuccess(data);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching customer profile: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void createInitialCustomerProfile(String fullName, String phoneNumber, String email, Callback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be logged in to create a customer profile.");
            return;
        }

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("uid", user.getUid());
        initialData.put("fullName", fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : "Customer");
        initialData.put("email", email != null && !email.trim().isEmpty() ? email.trim() : (user.getEmail() != null ? user.getEmail() : ""));
        initialData.put("phoneNumber", phoneNumber != null ? phoneNumber.trim() : "");
        initialData.put("address", "");
        initialData.put("role", "customer");
        initialData.put("isActive", true);
        initialData.put("createdAt", Timestamp.now());

        getDb().collection(COLLECTION_CUSTOMERS)
                .document(user.getUid())
                .set(initialData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating initial customer profile: " + e.getMessage(), e);
                    if (callback != null) callback.onError("Failed to create customer profile: " + e.getMessage());
                });
    }

    public static void ensureCustomerProfileExists(String fullName, String email, Callback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("You must be logged in to verify your profile.");
            return;
        }

        getDb().collection(COLLECTION_CUSTOMERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            String name = data.containsKey("fullName") && data.get("fullName") != null
                                    ? String.valueOf(data.get("fullName")).trim() : "";
                            String phone = data.containsKey("phoneNumber") && data.get("phoneNumber") != null
                                    ? String.valueOf(data.get("phoneNumber")).trim() : "";
                            String address = data.containsKey("address") && data.get("address") != null
                                    ? String.valueOf(data.get("address")).trim() : "";
                            String mail = data.containsKey("email") && data.get("email") != null
                                    ? String.valueOf(data.get("email")).trim() : (user.getEmail() != null ? user.getEmail() : "");
                            if (!name.isEmpty()) {
                                AppState.getInstance().updateAccountDetails(name, phone, address, mail);
                            }
                        }
                        if (callback != null) callback.onSuccess(null);
                    } else {
                        Map<String, Object> initialData = new HashMap<>();
                        initialData.put("uid", user.getUid());
                        initialData.put("fullName", fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : "Customer");
                        initialData.put("email", email != null ? email.trim() : (user.getEmail() != null ? user.getEmail() : ""));
                        initialData.put("phoneNumber", "");
                        initialData.put("address", "");
                        initialData.put("role", "customer");
                        initialData.put("isActive", true);
                        initialData.put("createdAt", Timestamp.now());

                        getDb().collection(COLLECTION_CUSTOMERS)
                                .document(user.getUid())
                                .set(initialData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating initial Google customer profile: " + e.getMessage(), e);
                                    if (callback != null) callback.onError("Failed to create customer profile: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking customer profile existence: " + e.getMessage(), e);
                    if (callback != null) callback.onError("Failed to check customer profile: " + e.getMessage());
                });
    }

    public static void checkHasActiveReservation(String frameId, Callback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onSuccess(false);
            return;
        }

        getDb().collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("customerId", user.getUid())
                .whereEqualTo("frameId", frameId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasActive = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            String status = data.containsKey("status") ? String.valueOf(data.get("status")) : "Pending";
                            if (isActiveStatus(status)) {
                                hasActive = true;
                                break;
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(hasActive);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking active reservation for frame: " + e.getMessage(), e);
                    if (callback != null) callback.onSuccess(false);
                });
    }

    public static void saveScanResult(ScanModel scan, Callback<String> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) {
            if (callback != null) callback.onError("User is not signed in to Firebase");
            return;
        }

        String customerId = user.getUid();
        String customerName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : AppState.getInstance().getUserName();
        String customerEmail = user.getEmail() != null ? user.getEmail() : "";

        scan.setCustomerId(customerId);
        scan.setCustomerName(customerName != null ? customerName : "Customer");

        Map<String, Object> map = scan.toMap();
        map.put("customerId", customerId);
        map.put("userId", customerId);
        map.put("customerName", scan.getCustomerName());
        if (!customerEmail.isEmpty()) {
            map.put("customerEmail", customerEmail);
        }

        getDb().collection(COLLECTION_SCAN_HISTORY)
                .add(map)
                .addOnSuccessListener(documentReference -> {
                    String resultId = documentReference.getId();
                    scan.setId(resultId);
                    if (callback != null) callback.onSuccess(resultId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save scan history: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void getUserScanHistory(Callback<List<ScanModel>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) {
            if (callback != null) callback.onSuccess(new ArrayList<>());
            return;
        }

        final String customerId = user.getUid();

        getDb().collection(COLLECTION_SCAN_HISTORY)
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ScanModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            list.add(ScanModel.fromMap(data, doc.getId()));
                        }
                    }

                    Collections.sort(list, (a, b) -> {
                        if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                        if (a.getTimestamp() == null) return 1;
                        if (b.getTimestamp() == null) return -1;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });

                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user scan history: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void syncPendingScans(Callback<Integer> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) {
            if (callback != null) callback.onSuccess(0);
            return;
        }

        String uid = user.getUid();
        List<ScanModel> allScans = AppState.getInstance().getAllRawScans();
        List<ScanModel> pendingScans = new ArrayList<>();

        for (ScanModel scan : allScans) {
            if (scan == null) continue;
            // Identify scans that have not yet been confirmed in Firestore
            String id = scan.getId();
            String cId = scan.getCustomerId();
            if (id != null && (id.startsWith("scan_") || cId == null || cId.isEmpty() || "local".equalsIgnoreCase(cId) || "guest".equalsIgnoreCase(cId))) {
                pendingScans.add(scan);
            }
        }

        if (pendingScans.isEmpty()) {
            if (callback != null) callback.onSuccess(0);
            return;
        }

        final int total = pendingScans.size();
        final java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger uploaded = new java.util.concurrent.atomic.AtomicInteger(0);

        for (ScanModel scan : pendingScans) {
            final String oldId = scan.getId();
            scan.setCustomerId(uid);
            if (scan.getCustomerName() == null || scan.getCustomerName().isEmpty() || "User".equals(scan.getCustomerName())) {
                scan.setCustomerName(AppState.getInstance().getUserName());
            }

            saveScanResult(scan, new Callback<String>() {
                @Override
                public void onSuccess(String resultId) {
                    if (resultId != null) {
                        AppState.getInstance().updateScanId(oldId, resultId);
                        uploaded.incrementAndGet();
                    }
                    if (completed.incrementAndGet() == total) {
                        if (callback != null) callback.onSuccess(uploaded.get());
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (completed.incrementAndGet() == total) {
                        if (callback != null) callback.onSuccess(uploaded.get());
                    }
                }
            });
        }
    }

    public static void deleteScanHistoryItem(String scanId, Callback<Void> callback) {
        if (scanId == null || scanId.trim().isEmpty() || scanId.startsWith("scan_")) {
            if (callback != null) callback.onSuccess(null);
            return;
        }

        getDb().collection(COLLECTION_SCAN_HISTORY)
                .document(scanId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete scan: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void getAnnouncements(Callback<List<AnnouncementModel>> callback) {
        getDb().collection(COLLECTION_ANNOUNCEMENTS)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AnnouncementModel> activeList = new ArrayList<>();
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            AnnouncementModel model = AnnouncementModel.fromMap(data, doc.getId());
                            String start = model.getStartDate();
                            String end = model.getEndDate();

                            boolean isStarted = start.isEmpty() || start.compareTo(today) <= 0;
                            boolean isNotEnded = end.isEmpty() || end.compareTo(today) >= 0;

                            if (isStarted && isNotEnded) {
                                activeList.add(model);
                            }
                        }
                    }

                    Collections.sort(activeList, (a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    if (callback != null) callback.onSuccess(activeList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching announcements: " + e.getMessage(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }
}
