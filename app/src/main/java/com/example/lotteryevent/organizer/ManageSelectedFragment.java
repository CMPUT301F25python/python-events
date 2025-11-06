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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This Fragment allows the organizer to view and manage entrants who were selected
 * through the lottery for a specific event
 *
 * <p>
 *     The organizer may cancel a selected entrant from the waitlist. Cancelling removes the user from the selected list
 *     and into the the waitlist in the FireStore subcollection under the same event ID.
 *     This allows organizers to revoke selection from chosen participants
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
        View view = inflater.inflate(R.layout.fragment_manage_selected, container, false);

        if (this.getArguments() != null) {
            this.eventId = this.getArguments().getString("eventId");
        }

        this.db = FirebaseFirestore.getInstance();

        RecyclerView recyclerView  =view.findViewById(R.id.recyclerViewSelected);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

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
     *     Entrants are stored as document IDs under "selected" subcollection
     * </p>
     *
     */
    private void loadSelectedEntrants() {
        this.db.collection("events")
                .document(this.eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(query -> {
                    this.selectedUsers.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String uid = doc.getId();

                        this.selectedUsers.add(uid);

                    }
                    this.selectedAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this.getContext(), "Failed to load selected entrants", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cancels a selected entrant by removing them from selected list and returning them
     * to event waitlist
     *
     * @param userId
     */
    private void cancelSelectedUser(String userId) {
        // Remove entrant from selected list
        this.db.collection("events")
                .document(this.eventId)
                .collection("selected")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {

                    // Add user back to waitlist
                    this.db.collection("events")
                            .document(this.eventId)
                            .collection("waitlist")
                            .document(userId)
                            .set(new HashMap<>())
                            .addOnSuccessListener(unused -> {
                                this.selectedUsers.remove(userId);
                                this.selectedAdapter.notifyDataSetChanged();

                                Toast.makeText(this.getContext(), "User cancelled and returned to waitlist", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this.getContext(), "Failed to return user to waitlist", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this.getContext(), "Failed to remove selected user", Toast.LENGTH_SHORT).show());
    }

}
