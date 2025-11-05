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
import androidx.navigation.Navigation;

import com.example.lotteryevent.organizer.OrganizerEventPageFragmentArgs;
import com.example.lotteryevent.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrganizerEventPageFragment extends Fragment {

    private static final String TAG = "OrganizerEventPage";

    private FirebaseFirestore db;
    private String eventId;

    public OrganizerEventPageFragment() { }

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_organizer_event_page, container, false);
    }

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