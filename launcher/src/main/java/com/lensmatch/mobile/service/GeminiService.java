package com.lensmatch.mobile.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lensmatch.mobile.utils.FaceShapeDetector;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {
    private static final String TAG = "GeminiService";
    private static String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(FaceShapeDetector.ShapeRecommendation recommendation);
        void onError(String message);
    }

    public static void setApiKey(String apiKey) {
        GEMINI_API_KEY = apiKey;
    }

    public static void fetchRecommendations(String shape, Callback callback) {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendationForShape(shape);
            handler.post(() -> callback.onSuccess(fallback));
            return;
        }

        executor.execute(() -> {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + GEMINI_API_KEY;

                String prompt = "You are an expert optical stylist. Given the face shape \"" + shape + "\", "
                    + "return ONLY a single valid JSON object (no markdown fences, no commentary) with exactly these keys:\n"
                    + "\"recommended\": array of 2-4 strings\n"
                    + "\"avoided\": array of 2-4 strings\n"
                    + "\"description\": string, 1-2 sentences (max 200 characters)\n\n"
                    + "Both arrays must choose only from this exact list: "
                    + "[\"Round\", \"Cat Eye\", \"Rectangle\", \"Wayfarer\", \"Square\", \"Aviator\", \"Geometric\", \"Browline\", \"Oval\"]. "
                    + "No frame shape may appear in both \"recommended\" and \"avoided\".\n\n"
                    + "If \"" + shape + "\" is not a recognizable face shape, still return valid JSON using your best "
                    + "guess at the closest matching shape, and mention the assumption briefly in \"description\".";
                    
                JsonObject requestJson = new JsonObject();
                JsonObject content = new JsonObject();
                JsonObject part = new JsonObject();
                part.addProperty("text", prompt);

                content.add("parts", gson.toJsonTree(new JsonObject[]{part}));
                requestJson.add("contents", gson.toJsonTree(new JsonObject[]{content}));

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

                            String[] recs = gson.fromJson(parsed.getAsJsonArray("recommended"), String[].class);
                            String[] avoids = gson.fromJson(parsed.getAsJsonArray("avoided"), String[].class);
                            String desc = parsed.get("description").getAsString();

                            FaceShapeDetector.ShapeRecommendation rec = new FaceShapeDetector.ShapeRecommendation(recs, avoids, desc);
                            handler.post(() -> callback.onSuccess(rec));
                        } else {
                            handler.post(() -> callback.onError("Model did not return JSON:\n" + rawText));
                        }
                    } else {
                        String errBody = (response.body() != null) ? response.body().string() : "No response body";
                        Log.e(TAG, "Gemini HTTP " + response.code() + ": " + errBody);
                        FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendationForShape(shape);
                        handler.post(() -> callback.onSuccess(fallback));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini Request Exception: " + e.getMessage(), e);
                FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendationForShape(shape);
                handler.post(() -> callback.onSuccess(fallback));
            }
        });
    }
}
