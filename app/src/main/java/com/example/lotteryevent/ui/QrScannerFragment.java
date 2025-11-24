package com.example.lotteryevent.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lotteryevent.R;
import com.example.lotteryevent.viewmodels.QrScannerViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A fragment for scanning QR codes using the device's camera.
 * This class uses CameraX for camera operations and Google's ML Kit for barcode detection.
 * Upon successfully scanning a QR code containing an event ID, it navigates to the
 * {@link EventDetailsFragment} for that event.
 */
public class QrScannerFragment extends Fragment {
    /**
     * Factory for creating the ViewModel.
     * This is nullable and primarily used for dependency injection during unit testing.
     */
    private androidx.lifecycle.ViewModelProvider.Factory viewModelFactory;
    /** Executor for background camera operations. */
    private ExecutorService cameraExecutor;
    /** The view that displays the camera feed. */
    private PreviewView previewView;
    /** The ML Kit barcode scanner instance. */
    private BarcodeScanner scanner;
    /** The ViewModel for this fragment. */
    private QrScannerViewModel viewModel;


    /**
     * Required empty public constructor.
     */
    public QrScannerFragment() {
        // Default behavior
    }

    /**
     * Constructor used for testing to allow injection of a custom ViewModel factory.
     *
     * @param viewModelFactory The factory to use when creating the ViewModel (e.g., a fake for UI tests).
     */
    public QrScannerFragment(androidx.lifecycle.ViewModelProvider.Factory viewModelFactory) {
        this.viewModelFactory = viewModelFactory;
    }

    /**
     * Inflates the layout for this fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false);
    }

    /**
     * Called after the view has been created. This is where view initialization,
     * permission checks, and camera setup are triggered.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.camera_preview);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize ViewModel (use the custom factory if provided by the test constructor)
        if (viewModelFactory != null) {
            viewModel = new ViewModelProvider(this, viewModelFactory).get(QrScannerViewModel.class);
        }
        else {
            viewModel = new ViewModelProvider(this).get(QrScannerViewModel.class);
        }

        // Initialize the barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);

        // Observe ViewModel for successful scans
        viewModel.getScannedEventId().observe(getViewLifecycleOwner(), eventId -> {
            if (eventId != null) {
                navigateToEventDetails(eventId);
            }
        });

        // Check for camera permission and start the camera
        if (isCameraPermissionGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Initializes and starts the camera session.
     * This method sets up the CameraX provider, configures the Preview and ImageAnalysis use cases,
     * and binds them to the fragment's lifecycle.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Pass the imageProxy to the processing method
                imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("QrScannerFragment", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Processes a single frame from the camera's ImageAnalysis use case.
     * Converts the frame to an InputImage and passes it to the ML Kit BarcodeScanner.
     * @param imageProxy The image frame from the camera.
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    // Check if we found a barcode and we are not already processing one
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        // Pass the raw data to ViewModel for processing
                        viewModel.processScannedContent(barcode.getRawValue());
                    }
                })
                .addOnFailureListener(e -> Log.e("QrScannerFragment", "Barcode scanning failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }


    /**
     * Navigates to the Event Details screen for the successfully scanned event.
     * This method creates a Bundle containing the event ID, displays a brief "Event Found!"
     * confirmation toast to the user, and triggers the navigation action using the NavController.
     *
     * @param eventId The unique identifier of the event obtained from the QR code.
     */
    private void navigateToEventDetails(String eventId) {
        Toast.makeText(getContext(), "Event Found!", Toast.LENGTH_SHORT).show();

        Bundle args = new Bundle();
        args.putString("eventId", eventId);

        NavHostFragment.findNavController(this).navigate(R.id.eventDetailsFragment, args);
    }

    /**
     * Checks if the app has been granted camera permission.
     * @return true if permission is granted, false otherwise.
     */
    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * ActivityResultLauncher for handling the camera permission request.
     * If permission is granted, it starts the camera.
     * If denied, it shows a toast and navigates back.
     */
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack(); // Go back if denied
                }
            });

    /**
     * Called when the fragment's view is destroyed.
     * This is where resources like the camera executor and barcode scanner are cleaned up
     * to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        if (scanner != null) {
            scanner.close();
        }
        // Reset ViewModel processing state in case the user navigates back to this fragment instance
        viewModel.resetScanner();
    }
}
