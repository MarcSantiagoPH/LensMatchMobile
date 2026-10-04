package com.lensmatch.mobile.service;

import android.os.Handler; 
import android.os.Looper; 
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth; 
import com.google.firebase.auth.FirebaseUser; 
import com.google.gson.Gson; 
import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors; 
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType; 
import okhttp3.OkHttpClient; 
import okhttp3.Request; 
import okhttp3.RequestBody; 
import okhttp3.Response;

public class AuthApiService { 
    private static final String TAG = "AuthApiService"; 
    public static final String BACKEND_BASE_URL = "https://lensmatchstaffportal.onrender.com"; 
    private static final String ENDPOINT_SEND_OTP = BACKEND_BASE_URL + "/api/mobile/send-otp"; 
    private static final String ENDPOINT_RESEND_OTP = BACKEND_BASE_URL + "/api/mobile/resend-otp"; 
    private static final String ENDPOINT_VERIFY_OTP = BACKEND_BASE_URL + "/api/mobile/verify-otp";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(); 
    private static final Handler handler = new Handler(Looper.getMainLooper()); 
    private static final OkHttpClient client = new OkHttpClient.Builder() 
            .connectTimeout(60, TimeUnit.SECONDS) 
            .readTimeout(60, TimeUnit.SECONDS) 
            .writeTimeout(60, TimeUnit.SECONDS) 
            .build(); 
    private static final Gson gson = new Gson(); 
    
    public interface Callback<T> { 
        void onSuccess(T result); 
        void onError(String errorMessage); 
    } 
    
    public static void sendOtp(Callback<Void> callback) { 
        executeOtpRequest(ENDPOINT_SEND_OTP, "{}", new Callback<String>() { 
            @Override public void onSuccess(String message) { 
                if (callback != null) callback.onSuccess(null); 
            } 
            @Override public void onError(String errorMessage) { 
                if (callback != null) callback.onError(errorMessage); 
            } 
        }); 
    } 
    
    public static void resendOtp(Callback<Void> callback) { 
        executeOtpRequest(ENDPOINT_RESEND_OTP, "{}", new Callback<String>() { 
            @Override public void onSuccess(String message) { 
                if (callback != null) callback.onSuccess(null); 
            } 
            @Override public void onError(String errorMessage) { 
                if (callback != null) callback.onError(errorMessage); 
            } 
        }); 
    } 
    
    public static void verifyOtp(String otpCode, Callback<Boolean> callback) { 
        if (otpCode == null || otpCode.trim().length() != 6) { 
            if (callback != null) callback.onError("Please enter a valid 6-digit verification code."); 
            return; 
        } 
        JsonObject jsonObj = new JsonObject(); 
        jsonObj.addProperty("otp", otpCode.trim()); 
        executeOtpRequest(ENDPOINT_VERIFY_OTP, jsonObj.toString(), new Callback<String>() { 
            @Override public void onSuccess(String message) { 
                if (callback != null) callback.onSuccess(true); 
            } 
            @Override public void onError(String errorMessage) { 
                if (callback != null) callback.onError(errorMessage); 
            } 
        }); 
    } 
    
    private static void executeOtpRequest(String url, String jsonBody, Callback<String> callback) { 
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); 
        if (user == null) { 
            if (callback != null) callback.onError("Your session has expired. Please log in again."); 
            return; 
        } 
        user.getIdToken(false).addOnCompleteListener(task -> { 
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getToken() == null) { 
                String err = task.getException() != null ? task.getException().getMessage() : "Failed to retrieve authentication token."; 
                Log.e(TAG, "Error fetching Firebase ID Token: " + err); 
                if (callback != null) callback.onError("Authentication failed. Please log in again."); 
                return; 
            } 
            String idToken = task.getResult().getToken(); 
            executor.execute(() -> { 
                try { 
                    RequestBody body = RequestBody.create( 
                            jsonBody, MediaType.parse("application/json; charset=utf-8") 
                    ); 
                    Request request = new Request.Builder() 
                            .url(url) 
                            .addHeader("Authorization", "Bearer " + idToken) 
                            .post(body) 
                            .build(); 
                    try (Response response = client.newCall(request).execute()) { 
                        String respStr = response.body() != null ? response.body().string() : ""; 
                        Log.d(TAG, "OTP API Response (" + response.code() + "): " + respStr); 
                        JsonObject respJson = null; 
                        try { 
                            if (!respStr.isEmpty()) { 
                                respJson = gson.fromJson(respStr, JsonObject.class); 
                            } 
                        } catch (Exception ignored) {} 
                        
                        if (response.isSuccessful()) { 
                            String msg = (respJson != null && respJson.has("message") && !respJson.get("message").isJsonNull()) ? respJson.get("message").getAsString() : "Success"; 
                            handler.post(() -> callback.onSuccess(msg)); 
                        } else { 
                            String errorMsg = (respJson != null && respJson.has("error") && !respJson.get("error").isJsonNull()) ? respJson.get("error").getAsString() : ((respJson != null && respJson.has("message") && !respJson.get("message").isJsonNull()) ? respJson.get("message").getAsString() : parseDefaultHttpError(response.code())); 
                            handler.post(() -> callback.onError(errorMsg)); 
                        } 
                    } 
                } catch (Exception e) { 
                    Log.e(TAG, "Network exception during OTP call: " + e.getMessage(), e); 
                    handler.post(() -> callback.onError("Network error. Please check your internet connection.")); 
                } 
            }); 
        }); 
    } 
    
    private static String parseDefaultHttpError(int statusCode) { 
        switch (statusCode) { 
            case 400: return "Invalid or expired verification code. Please check and try again."; 
            case 401: return "Your session has expired. Please log in again."; 
            case 404: return "Verification code has expired or is missing. Please request a new code."; 
            case 429: return "Too many requests. Please wait a moment before trying again."; 
            case 500: 
            default: return "Server error (" + statusCode + "). Please try again later."; 
        } 
    }
}
