package com.example.lotteryevent.organizer;

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

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.SelectedAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * This Fragment allows the organizer to view and manage entrants who were selected
 * through the lottery for a specific event
 *
 * <p>
 *      A selected entrant is represented by "invited" status in firestore
 * </p>
 *
 */
public class ManageSelectedFragment extends Fragment {
    private FirebaseFirestore db;
    private SelectedAdapter selectedAdapter;
    private String eventId;
    private List<String> selectedUsers = new ArrayList<>();

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
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        
        View view = inflater.inflate(R.layout.fragment_manage_selected, container, false);

        if (this.getArguments() != null) {
            this.eventId = this.getArguments().getString("eventId");
        }

        this.db = FirebaseFirestore.getInstance();

        RecyclerView recyclerView  =view.findViewById(R.id.recyclerViewSelected);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        // Cancelling user reverts their status back to "waiting"
        this.selectedAdapter = new SelectedAdapter(this.selectedUsers, userId -> this.cancelSelectedUser(userId));
        recyclerView.setAdapter(this.selectedAdapter);

        this.loadSelectedEntrants();
        return view;
    }

    /**
     *
     * Loads all selected entrants for the event from firestore and displays them
     *
     * <p>
     *     Entrants are stored as document IDs with "invited" status
     * </p>
     *
     */
    private void loadSelectedEntrants() {
        this.db.collection("events")
                .document(this.eventId)
                .collection("entrants")
                .whereEqualTo("status", "invited")
                .get()
                .addOnSuccessListener(query -> {
                    this.selectedUsers.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String uid = doc.getId();

                        this.selectedUsers.add(uid);

                    }
                    this.selectedAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this.getContext(), "Failed to load selected entrants", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cancels a selected entrant by setting their status back to "waiting"
     * and automatically selects the next waiting user if one exists
     *
     * @param userId
     * This is the user's ID stored as docId in firestore
     */
    private void cancelSelectedUser(String userId) {
        // Get waitlist before anything is modified
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(waitQuery -> {
                    List<DocumentSnapshot> waitlistDocs = waitQuery.getDocuments();

                    String replacementUserId = null;

                    // Select replacement entrant before removing user from invited list
                    if (!waitlistDocs.isEmpty()) {
                        int randomUser = new Random().nextInt(waitlistDocs.size());
                        replacementUserId = waitlistDocs.get(randomUser).getId();
                    }

                    String finalReplacementUserId = replacementUserId;

                    // Move replacement to invited list if they exist
                    if (finalReplacementUserId != null) {
                        db.collection("events")
                                .document(eventId)
                                .collection("entrants")
                                .document(finalReplacementUserId)
                                .update("status", "invited");
                    }

                    // After replacing return cancelled user to waiting list
                    db.collection("events")
                            .document(eventId)
                            .collection("entrants")
                            .document(userId)
                            .update("status", "waiting")
                            .addOnSuccessListener(aVoid -> {

                                selectedUsers.remove(userId);

                                if (finalReplacementUserId != null) {
                                    selectedUsers.add(finalReplacementUserId);
                                    Toast.makeText(getContext(), "Replacement selected from waitlist", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "Cancelled. No users left in waitlist.", Toast.LENGTH_SHORT).show();
                                }

                                selectedAdapter.notifyDataSetChanged();
                            })
                            .addOnSuccessListener(e ->
                                    Toast.makeText(getContext(),"Unable to update cancelled user", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading waitlist", Toast.LENGTH_SHORT).show()
                );
    }
}
