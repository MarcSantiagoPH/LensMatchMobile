# LensMatch System Architecture & Core Functions
*(Compiled notes for Thesis Paper)*

## 1. Abstract / System Overview
LensMatch is an intelligent mobile application that combines Computer Vision for facial biometric analysis, Generative AI for personalized styling recommendations, and Augmented Reality (AR) to provide a seamless virtual try-on experience for eyewear.

## 2. Real-Time Biometric Facial Analysis (Computer Vision)
The application leverages computer vision to analyze the user's face in real-time using the device's camera.
* **Technology:** Google ML Kit Vision
* **Implementation (`FaceMeshOverlayView.java`, `FaceShapeDetector.java`):** The system dynamically maps facial contours to create a biometric mesh over the user's face. This tracks landmarks (jawline, cheekbones, etc.) and accurately extracts geometric metrics (e.g., face length-to-width ratio, jaw-to-cheek ratio, forehead-to-cheek ratio).

## 3. Convolutional Neural Network (CNN) Architecture
For the local facial shape classification, the system employs a custom-trained Convolutional Neural Network (CNN) exported to TensorFlow Lite format (`face_shape_model.tflite`). 
* **Architecture (`TFLiteFaceDetector.java`):** The underlying architecture is based on the **MobileNet** family (e.g., MobileNetV2), which is specifically optimized for mobile vision applications.
* **Pipeline:** The system first uses ML Kit to detect the face and perform alignment (leveling the roll angle). The face is then cropped, padded, and proportionally resized to a `224x224` pixel tensor (the standard input size for MobileNet architectures). This tensor is fed into the MobileNet-based TFLite model, which acts as a lightweight feature extractor to predict a probability distribution across 7 distinct face shapes (Diamond, Heart, Oblong, Oval, Round, Square, Triangle) in real-time.

## 4. Generative AI Optical Stylist (LLM Integration)
Once the face shape is detected, the app uses a Large Language Model (LLM) to generate personalized styling advice.
* **Model Selection (`GeminiService.java`):** The system integrates the **Gemini 3.6 Flash** model via the Google Generative Language API. The 'Flash' tier is optimized for low latency, which is critical for providing real-time feedback on a mobile device.
* **Prompt Engineering:** The system employs a "Role-Prompting" strategy where the AI acts as an "expert optical stylist." The prompt is heavily constrained to return *only* a single valid JSON object containing specific keys.
* **Domain Constraint Validation:** The AI is given a strict set of 9 supported frame shapes to choose from. The prompt explicitly prevents the model from hallucinating unsupported frame types.
* **Robust Parsing Mechanism:** The system parses the AI's response by extracting the JSON string using structural bounds. This error-handling mechanism ensures the JSON is correctly extracted even if the LLM hallucinates markdown wrapping.
* **Data Sanitization:** Extracted frame shapes are passed through a validation function to normalize them, ensuring they map safely to the UI components.

## 5. Biometric Fallback & Heuristic Logic
The system is not solely reliant on the cloud AI; it features a sophisticated rule-based fallback system.
* **Hybrid Architecture (`FaceShapeDetector.java`):** If the Gemini API is unavailable (due to network drops), the system seamlessly falls back to a local, rule-based recommendation engine, ensuring constant app functionality.
* **Dynamic Personalization:** The local fallback system uses precise biometric ratios to adjust recommendations dynamically. For example:
    * If the face is `Oval` but the `faceLengthToWidthRatio > 1.50f` (narrow face), it dynamically injects "Round" frames to add width.
    * If the face is `Square` and the `jawAngle < 120.0f` (sharp jaw), it prioritizes "Round" and "Oval" frames to provide visual softening.
* **Explainable AI (XAI):** Even in local fallback mode, the system generates dynamic explanations for the user, ensuring they understand *why* a specific recommendation was made.

## 6. Augmented Reality (AR) Virtual Try-On
The app allows users to visually test the AI's recommendations.
* **Implementation (`ARTryOnActivity.java`, `ARFrameOverlayView.java`, Unity Library):** An Augmented Reality module enables the "virtual try-on" experience. It overlays digital 3D models or 2D assets of the recommended eyeglasses onto the user's live camera feed, tracking their head movements to provide a realistic preview.

## 7. Cloud-Based Catalog and User Management
The app relies on a backend database to store frames and user data.
* **Implementation (`FirestoreService.java`):** A cloud infrastructure via Firebase Firestore is implemented to securely manage the product catalog of available frames and handle user profiles or historical match results.
