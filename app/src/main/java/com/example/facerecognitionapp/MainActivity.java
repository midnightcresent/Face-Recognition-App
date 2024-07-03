package com.example.facerecognitionapp;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private TextView textView;
    private FaceBoundingBoxView faceBoundingBoxView;

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {

            }
    );

    private final FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .enableTracking()
            .build();

    private final com.google.mlkit.vision.face.FaceDetector detector = FaceDetection.getClient(faceDetectorOptions);

    private final ImageAnalysis.Analyzer faceDetectionAnalyzer = new ImageAnalysis.Analyzer() {
        @OptIn(markerClass = ExperimentalGetImage.class)
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            @androidx.camera.core.ExperimentalGetImage
            @NonNull android.media.Image mediaImage = Objects.requireNonNull(imageProxy.getImage());
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        textView.setText("");
                        for (int index = 0; index < faces.size(); index++) {
                            com.google.mlkit.vision.face.Face face = faces.get(index);
                            String faceInfoString = "Face " + index;
                            android.graphics.Rect bounds = face.getBoundingBox();
                            faceBoundingBoxView.setBoundingBox(bounds, imageProxy.getWidth(), imageProxy.getHeight());

                            float rotX = face.getHeadEulerAngleX();
                            faceInfoString += "\nRotation X: " + rotX + " (" + (rotX >= 0 ? "Facing Upward" : "Facing Down") + ")";

                            float rotY = face.getHeadEulerAngleY();
                            faceInfoString += "\nRotation Y: " + rotY + " (" + (rotY >= 0 ? "Facing Right" : "Facing Left") + ")";

                            float rotZ = face.getHeadEulerAngleZ();
                            faceInfoString += "\nRotation Z: " + rotZ + " (" + (rotZ >= 0 ? "Rotation Counter-Clockwise" : "Facing Rotation Clockwise") + ")";

                            face.getLandmark(FaceLandmark.LEFT_EAR);

                            face.getContour(FaceContour.FACE);

                            if (face.getSmilingProbability() != null) {
                                float smileProb = face.getSmilingProbability();
                                faceInfoString += "\nSmiling Probability: " + smileProb;
                            }

                            if (face.getLeftEyeOpenProbability() != null) {
                                float leftEyeOpenProb = face.getLeftEyeOpenProbability();
                                faceInfoString += "\nLeft Eye Open Probability: " + leftEyeOpenProb;
                            }

                            if (face.getRightEyeOpenProbability() != null) {
                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                faceInfoString += "\nRight Eye Open Probability: " + rightEyeOpenProb;
                            }

                            if (face.getTrackingId() != null) {
                                int id = face.getTrackingId();
                            }

                            textView.setText(faceInfoString);
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        textView = findViewById(R.id.textView);
        faceBoundingBoxView = findViewById(R.id.face_bounding_box_view);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setResolutionSelector(
                        new ResolutionSelector.Builder()
                                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                                .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, faceDetectionAnalyzer);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }
}