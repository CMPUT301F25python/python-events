package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for handling QR code scanning logic
 */
public class QrScannerViewModel extends ViewModel {

    private final MutableLiveData<String> scannedEventId = new MutableLiveData<>();
    /**
     * An atomic boolean to ensure that the QR code is processed only once.
     * This prevents multiple navigation events from being fired from a single continuous scan.
     */
    private boolean isProcessing = false;

    /**
     * Returns the LiveData that emits the event ID when a QR code is successfully scanned.
     */
    public LiveData<String> getScannedEventId() {
        return scannedEventId;
    }

    /**
     * Processes the raw string value from the QR code scanner.
     * Checks if a scan is already in progress to prevent duplicate navigations.
     *
     * @param rawValue The raw string content of the QR code.
     */
    public void processScannedContent(String rawValue) {
        if (isProcessing) {
            return;
        }

        if (rawValue != null && !rawValue.isEmpty()) {
            isProcessing = true;
            scannedEventId.postValue(rawValue);
        }
    }

    /**
     * Resets the scanner state. Is called when the view is destroyed or
     * when navigation is complete so the user can scan again later.
     */
    public void resetScanner() {
        isProcessing = false;
        scannedEventId.setValue(null);
    }
}
