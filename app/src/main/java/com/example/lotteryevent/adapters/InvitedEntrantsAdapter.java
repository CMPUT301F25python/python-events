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
 * This adapter displays invited lottery entrants.
 *
 * <p>
 *     Groups a list of {@link Entrant} objects that represent invited users
 *     and displays them as a scrollable list.
 * <p>
 *     Each item includes: entrant's name (displayed) as well as a cancel button allowing
 *     the organizer to revoke the invitation.
 * </p>
 */
public class InvitedEntrantsAdapter extends RecyclerView.Adapter<InvitedEntrantsAdapter.ViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClicked(String userId);
    }

    private List<Entrant> entrants;
    private final OnCancelClickListener cancelListener;

    /**
     * Constructor for InvitedEntrantsAdapter.
     *
     * @param entrants List of Entrant objects
     * @param cancelListener Listener for cancel button
     */
    public InvitedEntrantsAdapter(List<Entrant> entrants, OnCancelClickListener cancelListener) {
        // Null safety: verify list exists
        this.entrants = entrants != null ? entrants : new ArrayList<>();
        this.cancelListener = cancelListener;
    }

    /**
     * Updates the data source and refreshes the RecyclerView.
     * This allows the ViewModel to push new data without recreating the adapter.
     *
     * @param newEntrants The updated list of entrants
     */
    public void setEntrants(List<Entrant> newEntrants) {
        this.entrants = newEntrants != null ? newEntrants : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.invited_user, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant currentEntrant = this.entrants.get(position);

        // Get data using Entrant getters
        String name = currentEntrant.getUserName();
        String displayName = (name != null && !name.isEmpty()) ? name : "Unnamed Entrant";
        String userId = currentEntrant.getUserId();

        holder.userNameText.setText(displayName);

        // Handle Cancel Button Click
        holder.cancelButton.setOnClickListener(v -> {
            if (cancelListener != null && userId != null) {
                cancelListener.onCancelClicked(userId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.entrants.size();
    }

    /**
     * ViewHolder Class representing a single invited entrant row.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText;
        Button cancelButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.userNameText);
            cancelButton = itemView.findViewById(R.id.cancelButton);
        }
    }
}