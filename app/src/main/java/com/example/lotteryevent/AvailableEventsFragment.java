package com.example.lotteryevent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AvailableEventsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth; // not highlighted for some reason, but it is being used

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private ListenerRegistration registration;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_available_events, container, false);
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

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Use ONE RecyclerView reference
        recyclerView = view.findViewById(R.id.events_recycler_view);
        if (recyclerView == null) {
            Toast.makeText(requireContext(),
                    "RecyclerView with id 'events_recycler_view' not found in fragment_available_events.xml",
                    Toast.LENGTH_LONG).show();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Use the adapter constructor that takes the row layout you want here
        adapter = new EventAdapter(R.layout.item_event);
        recyclerView.setAdapter(adapter);

//         click handling
         adapter.setOnItemClickListener(event ->
                 Toast.makeText(requireContext(), "Clicked: " + event.getName(), Toast.LENGTH_SHORT).show()
         );

        // Firestore listener
        registration = db.collection("events")
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded()) return;
                    if (err != null || snap == null) {
                        Toast.makeText(requireContext(), "Load failed: " + (err != null ? err.getMessage() : ""), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Event e = new Event();

                        String id = doc.getId();
                        e.setEventId(id);

                        String title = doc.getString("name");
                        if (title == null || title.trim().isEmpty()) {
                            title = id;
                        }
                        e.setName(title);

                        e.setLocation(doc.getString("location"));
                        e.setDescription(doc.getString("description"));

                        list.add(e);
                    }
                    adapter.setEvents(list);
                });

        adapter.setOnItemClickListener(event -> {
            String id = event.getEventId();
            if (id == null || id.trim().isEmpty() || "null".equals(id)) {
                Toast.makeText(requireContext(), "Missing/invalid event id", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("eventId", id);

            androidx.navigation.NavController nav =
                    androidx.navigation.fragment.NavHostFragment.findNavController(AvailableEventsFragment.this);
            nav.navigate(R.id.eventDetailsFragment, args);
        });

    }

    /**
     * Destroys view
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
