package com.example.lotteryevent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.adapters.EventAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AvailableEventsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private ListenerRegistration registration;   // remove this in onDestroyView()

    // Minimal inline model (you can move to its own file later)
    public static class Event {
        public String id = "";
        public String name = "";
        public String location = "";
        public Event() {}
    }

    public AvailableEventsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Use a layout that contains a RecyclerView with id eventsRecyclerView
        return inflater.inflate(R.layout.fragment_home, container, false);
        // If you must keep your existing file, change to:
        // return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser(); // use if you need per-user logic

        recyclerView = view.findViewById(R.id.events_recycler_view);
        adapter = new EventAdapter(); // your existing adapter class
        recyclerView.setAdapter(adapter);

        // Realtime listener. If you only want a single fetch, use .get() instead.
        registration = db.collection("events")
                .orderBy("createdAt")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load events: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    if (snap == null) return;

                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Event e = new Event();
                        e.id = doc.getId();
                        e.name = doc.getString("name");
                        e.location = doc.getString("location");
                        list.add(e);
                    }

                    // If EventAdapter extends ListAdapter:
//                    adapter.submitList(list);
                    // If it's a plain RecyclerView.Adapter with a setter, use:
                     adapter.setData(list); adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
