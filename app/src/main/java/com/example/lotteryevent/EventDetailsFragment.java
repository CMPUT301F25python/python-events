package com.example.lotteryevent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EventDetailsFragment extends Fragment {

    private TextView title, location, description, status;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
//        title = v.findViewById(R.id.detail_title);
//        location = v.findViewById(R.id.detail_location);
//        description = v.findViewById(R.id.detail_description);
//        status = v.findViewById(R.id.detail_status);

        String eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        if (eventId == null) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            return;
        }

        db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(this::bind)
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bind(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }
        title.setText(nz(doc.getString("name")));
        location.setText(nz(doc.getString("location")));
        description.setText(nz(doc.getString("description")));
        status.setText(nz(doc.getString("status")));
    }

    private String nz(String s) { return s == null ? "" : s; }
}
