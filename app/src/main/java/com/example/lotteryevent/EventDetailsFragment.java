package com.example.lotteryevent;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event; // <-- ADD THIS IMPORT
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {

    private static final String TAG = "EventDetailsFragment";

    // --- UI Components ---
    private ListView detailsList;
    private FrameLayout dynamicBottomContainer;
    private Button btnAction;
    private TextView textInfoMessage;
    private ProgressBar bottomProgressBar;

    // --- Data ---
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> dataList;
    private String eventId;
    private Event event; // CHANGED: Replaced DocumentSnapshot with our Event POJO
    private Entrant currentUserEntrant;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        initializeViews(v);
        dataList = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dataList);
        detailsList.setAdapter(listAdapter);
        eventId = (getArguments() != null) ? getArguments().getString("eventId") : null;
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_missing_event_id, Toast.LENGTH_SHORT).show();
            return;
        }
        fetchEventDetails();
    }

    private void initializeViews(View v) {
        detailsList = v.findViewById(R.id.details_list);
        dynamicBottomContainer = v.findViewById(R.id.dynamic_bottom_container);
        btnAction = v.findViewById(R.id.btn_action);
        textInfoMessage = v.findViewById(R.id.text_info_message);
        bottomProgressBar = v.findViewById(R.id.bottom_progress_bar);
    }

    /**
     * Fetches the event document and converts it into an Event object.
     */
    private void fetchEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // CHANGED: Convert snapshot to Event object
                        this.event = documentSnapshot.toObject(Event.class);
                        if (this.event != null) {
                            bindEventDetails();
                            checkUserEntrantStatus();
                        } else {
                            Log.w(TAG, "Failed to parse Event object from document.");
                            Toast.makeText(requireContext(), R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    String message = getString(R.string.error_failed_to_load, e.getMessage());
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Binds details from the Event object to the ListView.
     */
    private void bindEventDetails() {
        // CHANGED: Simpler check now
        if (event == null) {
            Toast.makeText(requireContext(), R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        dataList.clear();
        // CHANGED: Use getters from the Event object
        addAny("Name", event.getName());
        addAny("Organizer", event.getOrganizerName());
        addAny("Location", event.getLocation());
        addAny("Date and Time", event.getEventStartDateTime());
        addAny("Price", event.getPrice());
        addAny("Description", event.getDescription());
        addAny("Lottery Guidelines", event.getLotteryGuidelines());
        addAny("Max Attendees", event.getCapacity());
        listAdapter.notifyDataSetChanged();
    }

    /**
     * Checks if the current user is in the event's 'entrants' subcollection.
     */
    private void checkUserEntrantStatus() {
        if (currentUser == null) {
            updateDynamicBottomUI();
            return;
        }
        bottomProgressBar.setVisibility(View.VISIBLE);
        btnAction.setVisibility(View.GONE);
        textInfoMessage.setVisibility(View.GONE);
        db.collection("events").document(eventId).collection("entrants")
                .document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    bottomProgressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            currentUserEntrant = doc.toObject(Entrant.class);
                        } else {
                            currentUserEntrant = null;
                        }
                        updateDynamicBottomUI();
                    } else {
                        Log.w(TAG, "Failed to check entrant status", task.getException());
                    }
                });
    }

    /**
     * Shows/hides the button or text message based on the user's status and event status.
     */
    private void updateDynamicBottomUI() {
        if (event == null) return; // Guard clause

        // CHANGED: Use getter from Event object
        String eventStatus = event.getStatus();
        if (eventStatus == null) return;

        boolean isEntrant = currentUserEntrant != null;

        if (!isEntrant && eventStatus.equals("open")) {
            textInfoMessage.setVisibility(View.GONE);
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText(R.string.join_waiting_list);
            btnAction.setOnClickListener(v -> joinWaitingList());
        }
        else if (isEntrant && (eventStatus.equals("open") || eventStatus.equals("closed"))) {
            textInfoMessage.setVisibility(View.GONE);
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText(R.string.leave_waiting_list);
            btnAction.setOnClickListener(v -> leaveWaitingList());
        }
        else if (!isEntrant && eventStatus.equals("closed")) {
            btnAction.setVisibility(View.GONE);
            textInfoMessage.setVisibility(View.VISIBLE);
            textInfoMessage.setText(R.string.info_cannot_join_closed);
        }
        else {
            btnAction.setVisibility(View.GONE);
            textInfoMessage.setVisibility(View.GONE);
        }
    }

    /**
     * Creates a new Entrant object and saves it to Firestore to join the event.
     */
    private void joinWaitingList() {
        if (currentUser == null) return;
        bottomProgressBar.setVisibility(View.VISIBLE);
        btnAction.setEnabled(false);
        Entrant newEntrant = new Entrant();
        newEntrant.setUserName(currentUser.getDisplayName());
        newEntrant.setDateRegistered(Timestamp.now());
        newEntrant.setStatus("waiting");
        db.collection("events").document(eventId).collection("entrants")
                .document(currentUser.getUid())
                .set(newEntrant)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.success_joined_waiting_list, Toast.LENGTH_SHORT).show();
                    checkUserEntrantStatus();
                    btnAction.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    String message = getString(R.string.error_failed_to_join, e.getMessage());
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    bottomProgressBar.setVisibility(View.GONE);
                    btnAction.setEnabled(true);
                });
    }

    /**
     * Deletes the user's entrant document from Firestore to leave the event.
     */
    private void leaveWaitingList() {
        if (currentUser == null) return;
        bottomProgressBar.setVisibility(View.VISIBLE);
        btnAction.setEnabled(false);
        db.collection("events").document(eventId).collection("entrants")
                .document(currentUser.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.success_left_waiting_list, Toast.LENGTH_SHORT).show();
                    checkUserEntrantStatus();
                    btnAction.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    String message = getString(R.string.error_failed_to_leave, e.getMessage());
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    bottomProgressBar.setVisibility(View.GONE);
                    btnAction.setEnabled(true);
                });
    }

    private final DateFormat DF = new SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault());

    private void addAny(String label, @Nullable Object raw) {
        if (raw == null) return;
        String v;
        if (raw instanceof Timestamp) {
            v = DF.format(((Timestamp) raw).toDate());
        } else if (raw instanceof Date) {
            v = DF.format((Date) raw);
        } else if (raw instanceof Number && label.equals("Price")) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.CANADA);
            v = nf.format(((Number) raw).doubleValue());
        } else {
            v = raw.toString();
        }
        v = v.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) return;
        dataList.add(label + ": " + v);
    }
}