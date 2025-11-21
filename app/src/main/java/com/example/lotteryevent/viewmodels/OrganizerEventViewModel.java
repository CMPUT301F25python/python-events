package com.example.lotteryevent.viewmodels;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IOrganizerEventRepository;
import com.google.firebase.Timestamp;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * ViewModel for the OrganizerEventFragment.
 * <p>
 * This class is responsible for preparing and managing the data for the organizer's
 * event detail screen. It retrieves data from the {@link IOrganizerEventRepository},
 * processes it to determine the UI state, and exposes it to the Fragment via LiveData.
 * It is designed to be testable by allowing repository injection.
 */
public class OrganizerEventViewModel extends ViewModel {

    private static final String TAG = "OrganizerEventVM";

    // The repository is injected via the constructor for testability.
    private final IOrganizerEventRepository repository;

    // --- LiveData from Repository ---
    private final LiveData<Event> event;
    private final LiveData<Boolean> isRunDrawButtonEnabled;
    private final LiveData<Boolean> isLoading;
    private final LiveData<String> message;

    // --- UI State LiveData ---
    private final MutableLiveData<UiState> _uiState = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> _qrCodeBitmap = new MutableLiveData<>();

    // --- Exposed LiveData ---
    public LiveData<Event> getEvent() { return event; }
    public LiveData<Boolean> getIsRunDrawButtonEnabled() { return isRunDrawButtonEnabled; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<UiState> getUiState() { return _uiState; }
    public LiveData<Bitmap> getQrCodeBitmap() { return _qrCodeBitmap; }

    private final Observer<Event> eventObserver;

    /**
     * Enum representing the possible states of the UI.
     * The ViewModel determines the state, and the Fragment reacts to it.
     */
    public enum UiState {
        UPCOMING,
        OPEN,
        FINALIZED,
        ERROR
    }

    /**
     * Constructs the ViewModel and injects the data repository.
     *
     * @param repository The repository responsible for fetching organizer-specific event data.
     */
    public OrganizerEventViewModel(IOrganizerEventRepository repository) {
        this.repository = repository;

        // Connect the ViewModel's LiveData directly to the Repository's LiveData.
        event = repository.getEvent();
        isRunDrawButtonEnabled = repository.isRunDrawButtonEnabled();
        isLoading = repository.isLoading();
        message = repository.getMessage();

        // Define the observer logic that will translate event data into a UI state.
        this.eventObserver = this::determineUiState;

        // This ensures we can react to data changes from the repository at any time.
        event.observeForever(eventObserver);
    }

    /**
     * Initiates the process of fetching the event details and capacity status from the repository.
     *
     * @param eventId The unique identifier for the event to load.
     */
    public void loadEvent(String eventId) {
        repository.fetchEventAndCapacityStatus(eventId);
    }

    /**
     * Analyzes the event data to determine the correct UI state and updates the _uiState LiveData.
     *
     * @param event The event object fetched from the repository.
     */
    private void determineUiState(Event event) {
        if (event == null) {
            return;
        }

        if ("finalized".equals(event.getStatus())) {
            _uiState.setValue(UiState.FINALIZED);
        } else if (event.getRegistrationStartDateTime() != null &&
                Timestamp.now().compareTo(event.getRegistrationStartDateTime()) < 0) {
            _uiState.setValue(UiState.UPCOMING);
        } else {
            _uiState.setValue(UiState.OPEN);
        }
    }

    /**
     * Generates a QR Code bitmap from the eventId and posts it to the _qrCodeBitmap LiveData.
     *
     * @param eventId The text content to encode in the QR code.
     */
    public void generateQrCode(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.w(TAG, "Cannot generate QR code with null or empty eventId.");
            return;
        }

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 600, 600);
            Bitmap bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565);
            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            _qrCodeBitmap.postValue(bmp);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR Code", e);
        }
    }

    /**
     * Should be called by the Fragment after the QR code has been displayed.
     */
    public void onQrCodeShown() {
        _qrCodeBitmap.setValue(null);
    }

    /**
     * This method is called when the ViewModel is no longer used and will be destroyed.
     * It's crucial to clean up observers registered with `observeForever` to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        event.removeObserver(eventObserver);
    }
}