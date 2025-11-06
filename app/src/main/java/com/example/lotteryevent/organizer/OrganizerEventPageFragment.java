package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDeepLink;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.example.lotteryevent.organizer.OrganizerEventPageFragmentArgs;
import com.example.lotteryevent.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


/**
 * Displays info about a single event for the organizer to view.
 * <p>
 *     This fragment allows the organizer to view event information
 *     and run the participant draw
 * </p>
 */
public class OrganizerEventPageFragment extends Fragment {

    private static final String TAG = "OrganizerEventPage";

    private FirebaseFirestore db;
    private String eventId;
    private Button manageSelectedBtn;

    public OrganizerEventPageFragment() { }

    /**
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Retrieve the eventId from navigation arguments
        if (getArguments() != null) {
            eventId = OrganizerEventPageFragmentArgs.fromBundle(getArguments()).getEventId();
        }
    }

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
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_organizer_event_page, container, false);
    }

    /**
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Fetch and display event data if eventId is available
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        } else {
            Log.e(TAG, "Event ID is null or empty.");
            Toast.makeText(getContext(), "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
        }

        Button runLotteryButton = view.findViewById(R.id.btnRunLottery);
        runLotteryButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);

            Navigation.findNavController(view)
                    .navigate(R.id.action_organizerEventPageFragment_to_runDrawFragment, bundle);
        });

        this.manageSelectedBtn = view.findViewById(R.id.btnManageSelected);
        this.manageSelectedBtn.setVisibility(View.GONE); // hide until we confirm data
        this.manageSelectedBtn.setOnClickListener(v -> {
            OrganizerEventPageFragmentDirections
                    .ActionOrganizerEventPageFragmentToManageSelectedFragment action =
                    OrganizerEventPageFragmentDirections
                            .actionOrganizerEventPageFragmentToManageSelectedFragment(eventId);
            Navigation.findNavController(v).navigate(action);
        });

        refreshManageSelectedVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshManageSelectedVisibility();
    }

    private void refreshManageSelectedVisibility() {
        if (eventId == null) return;
        db.collection("events").document(eventId)
                .collection("selected").limit(1).get()
                .addOnSuccessListener(q -> {
                    boolean hasSelected = !q.isEmpty();
                    if (manageSelectedBtn != null) {
                        manageSelectedBtn.setVisibility(hasSelected ? View.VISIBLE : View.GONE);
                    }
                });
    }


    private void fetchEventDetails() {
        db.collection("events").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            // TODO: Grace is probably gonna expand out the Event class, that can be integrated later
                            String name = document.getString("name");

                            updateUi(name);
                        } else {
                            Log.d(TAG, "No such document");
                            Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "get failed with ", task.getException());
                        Toast.makeText(getContext(), "Failed to load event details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUi(String title) {
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(title);
    }
}