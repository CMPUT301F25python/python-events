package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying a list of {@link Entrant} objects.
 * <p>
 * This adapter manages the display of entrant details (specifically the name)
 * within a list. It includes logic to dynamically show or hide a "Cancel" button
 * based on the entrant's status. If an entrant has the status "invited", the
 * button is visible, allowing the organizer to revoke the invitation.
 * </p>
 */
public class EntrantListAdapter extends RecyclerView.Adapter<EntrantListAdapter.ViewHolder> {

    /**
     * Interface definition for a callback to be invoked when an action is performed
     * on an entrant item.
     */
    public interface OnEntrantActionListener {
        /**
         * Called when the "Cancel Invitation" button is clicked for a specific entrant.
         *
         * @param userId The unique identifier of the entrant whose invitation is being cancelled.
         */
        void onCancelInvite(String userId);
    }

    private List<Entrant> entrants;
    private final OnEntrantActionListener actionListener;

    /**
     * Constructs a new EntrantListAdapter.
     *
     * @param entrants The initial list of entrants to display. If null, an empty list is created.
     * @param listener The listener to handle actions (e.g., cancelling an invite).
     */
    public EntrantListAdapter(List<Entrant> entrants, OnEntrantActionListener listener) {
        this.entrants = entrants != null ? entrants : new ArrayList<>();
        this.actionListener = listener;
    }

    /**
     * Updates the data set used by the adapter and refreshes the RecyclerView.
     * Use this method when the underlying list of entrants changes (e.g., after a filter update).
     *
     * @param newEntrants The new list of entrants to display.
     */
    public void updateEntrants(List<Entrant> newEntrants) {
        this.entrants = newEntrants != null ? newEntrants : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Called when the RecyclerView needs a new {@link ViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     * <p>
     * This method handles the logic for the "Cancel" button:
     * <ul>
     *     <li>If the entrant's status is <b>"invited"</b>, the Cancel button is set to VISIBLE
     *     and a click listener is attached to trigger {@link OnEntrantActionListener#onCancelInvite}.</li>
     *     <li>For all other statuses, the Cancel button is set to GONE.</li>
     * </ul>
     * </p>
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);

        // Set Name
        String name = entrant.getUserName();
        holder.nameText.setText((name != null && !name.isEmpty()) ? name : "Unknown User");

        // LOGIC: Only show Cancel button if status is "invited"
        if ("invited".equalsIgnoreCase(entrant.getStatus())) {
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCancelInvite(entrant.getUserId());
                }
            });
        } else {
            holder.cancelButton.setVisibility(View.GONE);
            holder.cancelButton.setOnClickListener(null);
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of entrants.
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView cancelButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.entrant_name_text);
            cancelButton = itemView.findViewById(R.id.cancel_invitation_button);
        }
    }
}