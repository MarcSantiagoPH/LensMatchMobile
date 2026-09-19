package com.lensmatch.mobile.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lensmatch.mobile.utils.FaceShapeDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {
    private static final String TAG = "GeminiService";
    // API Key injected via local.properties and BuildConfig
    private static String GEMINI_API_KEY = com.lensmatch.mobile.BuildConfig.GEMINI_API_KEY;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private static final List<String> SUPPORTED_FRAMES = Arrays.asList(
            "Round", "Cat-Eye", "Rectangle", "Wayfarer", 
            "Square", "Aviator", "Geometric", "Browline", "Oval"
    );

    public interface Callback {
        void onSuccess(FaceShapeDetector.ShapeRecommendation recommendation);
        void onError(String message);
    }

    public static void setApiKey(String apiKey) {
        GEMINI_API_KEY = apiKey;
    }

    public static void fetchRecommendations(String shape, Callback callback) {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            Log.w(TAG, "Gemini API key is missing or invalid. Falling back to local logic.");
            FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendation(shape);
            handler.post(() -> callback.onSuccess(fallback));
            return;
        }

        executor.execute(() -> {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + GEMINI_API_KEY;

                String prompt = "You are an expert optical stylist. Given the face shape \"" + shape + "\", "
                    + "return ONLY a single valid JSON object with exactly these keys:\n"
                    + "\"primary\": array of up to 3 strings\n"
                    + "\"secondary\": array of up to 2 strings\n"
                    + "\"explanation\": string, 1-2 sentences (max 200 characters)\n\n"
                    + "Both arrays must choose only from this exact list: "
                    + "[\"Round\", \"Cat-Eye\", \"Rectangle\", \"Wayfarer\", \"Square\", \"Aviator\", \"Geometric\", \"Browline\", \"Oval\"]. "
                    + "No frame shape may appear in both arrays.\n\n"
                    + "If \"" + shape + "\" is not a recognizable face shape, still return valid JSON using your best "
                    + "guess at the closest matching shape, and mention the assumption briefly in \"explanation\".";

                // Build Gemini Request JSON
                JsonObject requestJson = new JsonObject();
                
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", prompt);
                
                JsonArray partsArray = new JsonArray();
                partsArray.add(textPart);
                
                JsonObject contentObj = new JsonObject();
                contentObj.add("parts", partsArray);
                
                JsonArray contentsArray = new JsonArray();
                contentsArray.add(contentObj);
                
                requestJson.add("contents", contentsArray);
                
                JsonObject genConfig = new JsonObject();
                genConfig.addProperty("responseMimeType", "application/json");
                requestJson.add("generationConfig", genConfig);

                RequestBody body = RequestBody.create(
                        requestJson.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonStr = response.body().string();
                        Log.d(TAG, "Gemini API Response: " + jsonStr);
                        JsonObject resObj = gson.fromJson(jsonStr, JsonObject.class);

                        String rawText = resObj.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();

                        int jsonStart = rawText.indexOf("{");
                        int jsonEnd = rawText.lastIndexOf("}");
                        if (jsonStart != -1 && jsonEnd != -1) {
                            String cleanJson = rawText.substring(jsonStart, jsonEnd + 1);
                            JsonObject parsed = gson.fromJson(cleanJson, JsonObject.class);

                            String[] primaryArr = gson.fromJson(parsed.getAsJsonArray("primary"), String[].class);
                            String[] secondaryArr = gson.fromJson(parsed.getAsJsonArray("secondary"), String[].class);
                            String explanation = parsed.has("explanation") ? parsed.get("explanation").getAsString() : "";

                            List<String> rawPrimary = primaryArr != null ? Arrays.asList(primaryArr) : new ArrayList<>();
                            List<String> rawSecondary = secondaryArr != null ? Arrays.asList(secondaryArr) : new ArrayList<>();
                            
                            // Apply robust mapping and validation
                            List<String> validPrimary = new ArrayList<>();
                            for (String frame : rawPrimary) {
                                String mapped = mapToSupportedFrame(frame);
                                if (mapped != null && !validPrimary.contains(mapped)) {
                                    validPrimary.add(mapped);
                                }
                            }

                            List<String> validSecondary = new ArrayList<>();
                            for (String frame : rawSecondary) {
                                String mapped = mapToSupportedFrame(frame);
                                // Ensure secondary doesn't overlap with primary
                                if (mapped != null && !validSecondary.contains(mapped) && !validPrimary.contains(mapped)) {
                                    validSecondary.add(mapped);
                                }
                            }
                            
                            // Fallback if AI entirely failed to produce valid frames
                            if (validPrimary.isEmpty()) {
                                Log.w(TAG, "Gemini produced zero valid primary frames. Falling back.");
                                FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendation(shape);
                                handler.post(() -> callback.onSuccess(fallback));
                                return;
                            }

                            FaceShapeDetector.ShapeRecommendation rec = new FaceShapeDetector.ShapeRecommendation(validPrimary, validSecondary, explanation);
                            handler.post(() -> callback.onSuccess(rec));
                        } else {
                            handler.post(() -> callback.onError("Model did not return JSON:\n" + rawText));
                        }
                    } else {
                        String errBody = (response.body() != null) ? response.body().string() : "No response body";
                        Log.e(TAG, "Gemini HTTP " + response.code() + ": " + errBody);
                        FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendation(shape);
                        handler.post(() -> callback.onSuccess(fallback));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini Request Exception: " + e.getMessage(), e);
                FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendation(shape);
                handler.post(() -> callback.onSuccess(fallback));
            }
        });
    }

    /**
     * Robustly maps AI frame string outputs to the strictly supported 9 categories.
     * @param rawFrame The raw string from the LLM
     * @return A valid supported frame string, or null if it cannot be safely mapped.
     */
    private static String mapToSupportedFrame(String rawFrame) {
        if (rawFrame == null) return null;
        String clean = rawFrame.trim();
        
        // 1. Direct match (case-insensitive)
        for (String supported : SUPPORTED_FRAMES) {
            if (supported.equalsIgnoreCase(clean)) {
                return supported;
            }
        }
        
        // 2. Map known synonyms and LLM hallucinations
        String lower = clean.toLowerCase();
        if (lower.contains("hexagonal")) return "Geometric";
        if (lower.contains("clubmaster") || lower.contains("horn-rimmed")) return "Browline";
        if (lower.contains("pilot") || lower.contains("tear-drop")) return "Aviator";
        if (lower.contains("rimless") || lower.contains("half-rim")) return "Oval"; // Fallback mapping
        if (lower.contains("butterfly") || lower.contains("oversized")) return "Cat-Eye";
        if (lower.contains("shield") || lower.contains("wraparound")) return "Square";
        
        // 3. Unrecognized -> safely drop
        Log.w(TAG, "Dropped unsupported/unmapped frame style from AI: " + rawFrame);
        return null;
    }
}
