package com.lensmatch.mobile.service;

import android.os.Handler;
import android.os.Looper;

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
    private static String GEMINI_API_KEY = "AQ.Ab8RN6IxJvAq03Jh6KEedjZA9wQRP8ptDjOG7VILgjHRvP3SPA";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(FaceShapeDetector.ShapeRecommendation recommendation);
        void onError(String message);
    }

    public static void fetchRecommendations(String shape, Callback callback) {
        executor.execute(() -> {
            // First attempt Gemini REST API request
            FaceShapeDetector.ShapeRecommendation aiResult = queryGeminiApi(shape);
            
            if (aiResult != null) {
                handler.post(() -> callback.onSuccess(aiResult));
            } else {
                // Fall back to rule-based engine
                FaceShapeDetector.ShapeRecommendation fallback = FaceShapeDetector.getRecommendationForShape(shape);
                handler.post(() -> callback.onSuccess(fallback));
            }
        });
    }

    private static FaceShapeDetector.ShapeRecommendation queryGeminiApi(String shape) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
            
            String prompt = "You are an expert optical stylist. Given a " + shape + " face shape, return a JSON object with exactly three keys: \"recommended\", \"avoided\", and \"description\". "
                    + "The \"recommended\" and \"avoided\" values must be arrays of strings choosing from: [\"Round\", \"Cat Eye\", \"Rectangle\", \"Wayfarer\", \"Square\", \"Aviator\", \"Geometric\", \"Browline\", \"Oval\"]. "
                    + "The \"description\" must be a short string explaining why. Output ONLY valid JSON.";

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
                    JsonObject resObj = gson.fromJson(jsonStr, JsonObject.class);
                    // Extract candidates text
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
                        
                        return new FaceShapeDetector.ShapeRecommendation(recs, avoids, desc);
                    }
                }
            }
        } catch (Exception e) {
            // API call failed or rate limited
        }
        return null;
    }
}
