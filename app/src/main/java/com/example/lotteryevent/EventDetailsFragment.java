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

/**
 * Fragment responsible for displaying detailed information about a specific event
 * to the user. This fragment also handles all user action related to joining, leaving,
 * accepting, or declining invites for the event.
 *
 * <p>
 *     This screen is used by participants that then switch to organizers when the event status changes to open
 *     It shows event details, dynamic UI updates and controls depending on the user's status in the event:
 *     waiting,invited, accepted, declined, cancelled.
 *
 *     Interacts with firestore to manage entrant participation and invites
 * </p>
 *
 * <p>Behaviour</p>
 * <ul>
 *     <li>Fetching event detials and rendering them in a list</li>
 *     <li>Check and display current entrant's status</li>
 *     <li>Allow users to join or leave waitlist</li>
 *     <li>Allow user to accept or decline participation in event</li>
 * </ul>
 */
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
    private NotificationCustomManager notificationCustomManager;
    private String eventId;
    private Event event;
    private Entrant currentUserEntrant;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    /**
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     *
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    /**
     *
     * @param v The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
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

        if (getArguments() != null) {
            // Assign to the class member variable
            this.eventId = getArguments().getString("eventId");
            String notificationId = getArguments().getString("notificationId");
            if (notificationId != null) {
                notificationCustomManager.markNotificationAsSeen(notificationId);
            }
        }

        if (this.eventId == null || this.eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_missing_event_id, Toast.LENGTH_SHORT).show();
            return;
        }
        fetchEventDetails();
    }

    /**
     * Initializes and binds UI components from the layout
     * @param v
     */
    private void initializeViews(View v) {
        detailsList = v.findViewById(R.id.details_list);
        buttonActionsContainer = v.findViewById(R.id.button_actions_container);
        btnActionPositive = v.findViewById(R.id.btn_action_positive);
        btnActionNegative = v.findViewById(R.id.btn_action_negative);
        textInfoMessage = v.findViewById(R.id.text_info_message);
        bottomProgressBar = v.findViewById(R.id.bottom_progress_bar);
    }

    /**
     * Fetches event data from firestore using the event ID passed using nav args.
     * Populates UI and checks the current user's entrant status
     */
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

    /**
     * Populates the event details ListView with formatted event attributes
     * Display is non-null
     */
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

    /**
     * Retrieve current user's entrant status and updates UI.
     * Executes callback after completion
     * @param onComplete
     */
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

    /**
     * Adds the current user to the event waitlist in firestore
     * sets entrant status to "waiting" and refreshes UI
     */
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

    /**
     * Allows a user to leave the waitlist. If registration is closed
     * shows confirmation dialog before removing entrant record
     */
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

    /**
     * Removes the current user's entrant record from Firestore and refreshes UI
     * Called directly or after confirmation
     */
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

    /**
     * Update the user's invitation status to "accepted" or "declined" in firestore
     * @param newStatus
     * The new status value ("accepted" or "declined")
     */
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

    /**
     * Utility method to hide all bottom UI actions
     */
    // --- Helper Methods ---
    private void hideAllBottomActions() {
        buttonActionsContainer.setVisibility(View.GONE);
        textInfoMessage.setVisibility(View.GONE);
    }

    /**
     * Show info text message to user
     * @param message
     */
    private void showInfoText(String message) {
        hideAllBottomActions();
        textInfoMessage.setText(message);
        textInfoMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Show single button to join or leave
     * @param text
     * @param listener
     */
    private void showOneButton(String text, View.OnClickListener listener) {
        hideAllBottomActions();
        btnActionNegative.setVisibility(View.GONE);
        btnActionPositive.setText(text);
        btnActionPositive.setOnClickListener(listener);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Show accept or decline button
     * @param positiveText
     * @param negativeText
     * @param positiveListener
     * @param negativeListener
     */
    private void showTwoButtons(String positiveText, String negativeText, View.OnClickListener positiveListener, View.OnClickListener negativeListener) {
        hideAllBottomActions();
        btnActionPositive.setText(positiveText);
        btnActionPositive.setOnClickListener(positiveListener);
        btnActionNegative.setText(negativeText);
        btnActionNegative.setOnClickListener(negativeListener);
        btnActionNegative.setVisibility(View.VISIBLE);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Enable or disable action buttons
     * @param enabled
     * whether user action buttons should be enabled
     */
    private void setActionsEnabled(boolean enabled) {
        bottomProgressBar.setVisibility(enabled ? View.GONE : View.VISIBLE);
        btnActionPositive.setEnabled(enabled);
        btnActionNegative.setEnabled(enabled);
    }

    /**
     *
     * @return
     * reference for the current user's entrant doc
     */
    private DocumentReference getEntrantDocRef() {
        return db.collection("events").document(eventId).collection("entrants").document(currentUser.getUid());
    }

    /**
     * Logs firestore error and re-enables UI actions
     * @param e
     * Exception thrown during firestore operation
     */
    private void handleFailure(@NonNull Exception e) {
        Log.w(TAG, "Firestore operation failed", e);
        setActionsEnabled(true);
    }


    private final DateFormat DF = new SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault());

    /**
     * Helper to safely append formatted event details into the list adapter
     * @param label
     * Display label
     * @param raw
     * raw value returned from firestore
     */
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