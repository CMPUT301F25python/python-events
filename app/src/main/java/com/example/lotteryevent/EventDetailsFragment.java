package com.example.lotteryevent;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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
    private Button btnActionPositive, btnActionNegative;
    private TextView textInfoMessage;
    private ProgressBar bottomProgressBar;
    private LinearLayout buttonActionsContainer;

    // --- Data ---
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> dataList;
    private ListView detailsList;
    private NotificationCustomManager notificationCustomManager;
    private String eventId;
    private Event event;
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

        notificationCustomManager = new NotificationCustomManager(getContext());

        String eventId = (getArguments() != null) ? getArguments().getString("eventId") : null;
        String notificationId = (getArguments() != null) ? getArguments().getString("notificationId") : null;
        if (notificationId != null) {
            notificationCustomManager.markNotificationAsSeen(notificationId);
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_missing_event_id, Toast.LENGTH_SHORT).show();
            return;
        }
        fetchEventDetails();
    }

    private void initializeViews(View v) {
        detailsList = v.findViewById(R.id.details_list);
        buttonActionsContainer = v.findViewById(R.id.button_actions_container);
        btnActionPositive = v.findViewById(R.id.btn_action_positive);
        btnActionNegative = v.findViewById(R.id.btn_action_negative);
        textInfoMessage = v.findViewById(R.id.text_info_message);
        bottomProgressBar = v.findViewById(R.id.bottom_progress_bar);
    }

    private void fetchEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        this.event = documentSnapshot.toObject(Event.class);
                        if (this.event != null) {
                            bindEventDetails();
                            checkUserEntrantStatus(null);
                        } else {
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

    private void bindEventDetails() {
        if (event == null) return;
        dataList.clear();
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

    private void checkUserEntrantStatus(@Nullable Runnable onComplete) {
        if (currentUser == null) {
            updateDynamicBottomUI();
            if (onComplete != null) onComplete.run();
            return;
        }

        bottomProgressBar.setVisibility(View.VISIBLE);
        hideAllBottomActions();

        db.collection("events").document(eventId).collection("entrants")
                .document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    bottomProgressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        currentUserEntrant = (doc != null && doc.exists()) ? doc.toObject(Entrant.class) : null;
                        updateDynamicBottomUI();
                    } else {
                        Log.w(TAG, "Failed to check entrant status", task.getException());
                    }

                    // After the UI is updated (or on failure), run the completion action.
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    /**
     * Overhauled logic to display the correct button(s) or text message.
     * The order of these checks is critical.
     */
    private void updateDynamicBottomUI() {
        if (event == null) return;
        hideAllBottomActions();

        boolean isEntrant = currentUserEntrant != null;
        String entrantStatus = isEntrant ? currentUserEntrant.getStatus() : null;
        String eventStatus = event.getStatus();
        Timestamp regEnd = event.getRegistrationEndDateTime();
        Timestamp now = Timestamp.now();

        Log.d(TAG, "Entrant Status: " + entrantStatus);
        Log.d(TAG, "Event Status: " + eventStatus);

        // Rule 1 (Highest Priority): User's invitation was cancelled.
        if (isEntrant && "cancelled".equals(entrantStatus)) {
            showInfoText(getString(R.string.info_invitation_cancelled));
            return;
        }

        if (isEntrant && "declined".equals(entrantStatus)) {
            showInfoText(getString(R.string.info_declined));
            return;
        }

        if (isEntrant && "accepted".equals(entrantStatus)) {
            showInfoText(getString(R.string.info_accepted));
            return;
        }

        // Rule 2: Event is permanently finalized.
        if ("finalized".equals(eventStatus)) {
            if (!isEntrant) {
                showInfoText(getString(R.string.info_cannot_join_closed));
            } else { // "waiting" or "invited"
                showInfoText(getString(R.string.info_finalized_organizer));
            }
            return;
        }

        // Rule 3: Event is "open".
        if ("open".equals(eventStatus)) {
            if (isEntrant) {
                if ("invited".equals(entrantStatus)) {
                    // Show Accept and Decline buttons
                    showTwoButtons(getString(R.string.accept_invitation), getString(R.string.decline_invitation),
                            v -> updateInvitationStatus("accepted"),
                            v -> updateInvitationStatus("declined")
                    );
                } else { // "waiting"
                    // Show Leave Waiting List button
                    showOneButton(getString(R.string.leave_waiting_list), v -> leaveWaitingList());
                }
            } else { // Not an entrant
                if (regEnd != null && now.compareTo(regEnd) < 0) {
                    // Pre-registration deadline: Show Join button
                    showOneButton(getString(R.string.join_waiting_list), v -> joinWaitingList());
                } else {
                    // Post-registration deadline
                    showInfoText(getString(R.string.info_cannot_join_closed));
                }
            }
        }
    }

    private void joinWaitingList() {
        if (currentUser == null) return;
        setActionsEnabled(false);
        Entrant newEntrant = new Entrant();
        newEntrant.setUserName(currentUser.getDisplayName());
        newEntrant.setDateRegistered(Timestamp.now());
        newEntrant.setStatus("waiting");
        getEntrantDocRef().set(newEntrant)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.success_joined_waiting_list, Toast.LENGTH_SHORT).show();
                    checkUserEntrantStatus(() -> setActionsEnabled(true));
                })
                .addOnFailureListener(this::handleFailure);
    }

    private void leaveWaitingList() {
        Timestamp regEnd = event.getRegistrationEndDateTime();
        Timestamp now = Timestamp.now();
        // If registration is over, show a warning dialog first.
        if (regEnd != null && now.compareTo(regEnd) > 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_leave_warning_title)
                    .setMessage(R.string.dialog_leave_warning_message)
                    .setPositiveButton(R.string.dialog_leave_confirm, (dialog, which) -> performLeave())
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        } else {
            performLeave(); // If registration is still open, leave immediately.
        }
    }

    private void performLeave() {
        setActionsEnabled(false);
        getEntrantDocRef().delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.success_left_waiting_list, Toast.LENGTH_SHORT).show();
                    // Pass the "re-enable" logic as a callback
                    checkUserEntrantStatus(() -> setActionsEnabled(true));
                })
                .addOnFailureListener(this::handleFailure);
    }

    private void updateInvitationStatus(String newStatus) {
        setActionsEnabled(false);
        getEntrantDocRef().update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    int messageResId = "accepted".equals(newStatus) ? R.string.info_accepted : R.string.success_invitation_declined;
                    Toast.makeText(getContext(), messageResId, Toast.LENGTH_SHORT).show();
                    // Pass the "re-enable" logic as a callback
                    checkUserEntrantStatus(() -> setActionsEnabled(true));
                })
                .addOnFailureListener(this::handleFailure);
    }

    // --- Helper Methods ---
    private void hideAllBottomActions() {
        buttonActionsContainer.setVisibility(View.GONE);
        textInfoMessage.setVisibility(View.GONE);
    }

    private void showInfoText(String message) {
        hideAllBottomActions();
        textInfoMessage.setText(message);
        textInfoMessage.setVisibility(View.VISIBLE);
    }

    private void showOneButton(String text, View.OnClickListener listener) {
        hideAllBottomActions();
        btnActionNegative.setVisibility(View.GONE);
        btnActionPositive.setText(text);
        btnActionPositive.setOnClickListener(listener);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

    private void showTwoButtons(String positiveText, String negativeText, View.OnClickListener positiveListener, View.OnClickListener negativeListener) {
        hideAllBottomActions();
        btnActionPositive.setText(positiveText);
        btnActionPositive.setOnClickListener(positiveListener);
        btnActionNegative.setText(negativeText);
        btnActionNegative.setOnClickListener(negativeListener);
        btnActionNegative.setVisibility(View.VISIBLE);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

    private void setActionsEnabled(boolean enabled) {
        bottomProgressBar.setVisibility(enabled ? View.GONE : View.VISIBLE);
        btnActionPositive.setEnabled(enabled);
        btnActionNegative.setEnabled(enabled);
    }

    private DocumentReference getEntrantDocRef() {
        return db.collection("events").document(eventId).collection("entrants").document(currentUser.getUid());
    }

    private void handleFailure(@NonNull Exception e) {
        Log.w(TAG, "Firestore operation failed", e);
        setActionsEnabled(true);
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