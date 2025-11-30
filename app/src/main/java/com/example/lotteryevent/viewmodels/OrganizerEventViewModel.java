package com.example.lotteryevent.viewmodels;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IOrganizerEventRepository;
import com.google.firebase.Timestamp;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
    private final MediatorLiveData<String> message = new MediatorLiveData<>();

    // --- UI State LiveData ---
    private final MutableLiveData<UiState> _uiState = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> _qrCodeBitmap = new MutableLiveData<>();
    private final MutableLiveData<String> _csvContent = new MutableLiveData<>();

    // --- Exposed LiveData ---

    /**
     * Returns LiveData event for the organizer
     * @return event
     */
    public LiveData<Event> getEvent() { return event; }

    /**
     * Returns LiveData boolean of whether run draw button is enabled
     * @return draw button boolean
     */
    public LiveData<Boolean> isRunDrawButtonEnabled() { return isRunDrawButtonEnabled; }
    /**
     * Returns LiveData event for loading
     * @return loading boolean
     */
    public LiveData<Boolean> isLoading() { return isLoading; }
    /**
     * Returns LiveData event for message
     * @return message
     */
    public LiveData<String> getMessage() { return message; }
    /**
     * Returns LiveData UIState
     * @return UIState
     */
    public LiveData<UiState> getUiState() { return _uiState; }
    /**
     * Returns LiveData bitmap for QR code
     * @return bitmap
     */
    public LiveData<Bitmap> getQrCodeBitmap() { return _qrCodeBitmap; }
    /**
     * Returns LiveData CSV content
     * @return CSV string content
     */
    public LiveData<String> getCsvContent() { return _csvContent; }


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

        /**
         * Sets message based on server message updates (from repository)
         * @param serverMsg message to set
         */
        message.addSource(repository.getMessage(), serverMsg -> {
            message.setValue(serverMsg);
        });

        // Define the observer logic that will translate event data into a UI state.
        this.eventObserver = this::determineUiState;

        // This ensures we can react to data changes from the repository at any time.
        event.observeForever(eventObserver);
    }

    /**
     * Initiates the process of fetching the event details, capacity status, and entrants from the repository.
     *
     * @param eventId The unique identifier for the event to load.
     */
    public void loadEvent(String eventId) {
        repository.fetchEventAndCapacityStatus(eventId);
        repository.fetchEntrants(eventId);
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
     * Updates the event poster image (Base64) for the given event.
     * @param eventId The ID of the event to update.
     * @param posterImageUrl The Base64-encoded poster image data.
     */
    public void updateEventPoster(String eventId, String posterImageUrl) {
        repository.updateEventPoster(eventId, posterImageUrl);
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
     * Triggers the event finalization process in the repository.
     * The repository will handle the logic of checking if the user is the organizer.
     *
     * @param eventId The ID of the event to finalize.
     */
    public void finalizeEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) return;
        repository.finalizeEvent(eventId);
    }

    /**
     * Transforms the current list of entrants into a CSV string.
     * The result is posted to _csvContent.
     */
    public void generateEntrantCsv() {
        // Retrieve the data directly from the repository's LiveData current value
        List<Entrant> entrants = repository.getEntrants().getValue();

        if (entrants == null || entrants.isEmpty()) {
            message.setValue("No entrants found to export.");
            Log.w(TAG, "generateEntrantCsv: No entrants found to export.");
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        // CSV Header
        csvBuilder.append("User ID,Name,Status,Date Registered,Latitude,Longitude\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (Entrant entrant : entrants) {
            // 1. Handle Name (escape quotes/commas)
            String name = entrant.getUserName() != null ? entrant.getUserName() : "Unknown";
            if (name.contains(",")) {
                name = "\"" + name + "\"";
            }

            // 2. Handle Date
            String dateStr = "";
            if (entrant.getDateRegistered() != null) {
                dateStr = dateFormat.format(entrant.getDateRegistered().toDate());
            }

            // 3. Handle GeoLocation
            String lat = "";
            String lon = "";
            if (entrant.getGeoLocation() != null) {
                lat = String.valueOf(entrant.getGeoLocation().getLatitude());
                lon = String.valueOf(entrant.getGeoLocation().getLongitude());
            }

            // 4. Handle Status
            String status = entrant.getStatus() != null ? entrant.getStatus() : "unknown";

            // Append Line
            csvBuilder.append(entrant.getUserId()).append(",")
                    .append(name).append(",")
                    .append(status).append(",")
                    .append(dateStr).append(",")
                    .append(lat).append(",")
                    .append(lon).append("\n");
        }

        // Post the result
        _csvContent.setValue(csvBuilder.toString());
    }

    /**
     * Resets the CSV content LiveData.
     * Call this after the Fragment has successfully handled the file save
     * to prevent re-saving on configuration changes.
     */
    public void onCsvExported() {
        _csvContent.setValue(null);
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