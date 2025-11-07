package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Entrant;

import java.util.List;

/**
 * An adapter for displaying a list of {@link Entrant} objects in a RecyclerView.
 * <p>
 * This class is responsible for creating the view for each item, binding the data
 * from an {@code Entrant} object to that view, and managing the overall list.
 */
public class EntrantListAdapter extends RecyclerView.Adapter<EntrantListAdapter.EntrantViewHolder> {

    /**
     * The list of Entrant objects that the adapter will display.
     */
    private final List<Entrant> entrantList;

    /**
     * Constructs a new {@code EntrantListAdapter}.
     *
     * @param entrantList The initial list of entrants to be displayed. This list will be
     *                    managed by the adapter.
     */
    public EntrantListAdapter(List<Entrant> entrantList) {
        this.entrantList = entrantList;
    }

    /**
     * Called when the RecyclerView needs a new {@link EntrantViewHolder} of the given type to represent
     * an item. This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EntrantViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * updates the contents of the {@link EntrantViewHolder#itemView} to reflect the item at the
     * given position.
     *
     * @param holder   The EntrantViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrantList.get(position);
        holder.bind(entrant);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return entrantList.size();
    }

    /**
     * Replaces the data in the adapter with a new list and refreshes the RecyclerView.
     * This method clears the existing list and adds all items from the new list.
     *
     * @param newEntrants The new list of entrants to display.
     */
    public void updateEntrants(List<Entrant> newEntrants) {
        this.entrantList.clear();
        this.entrantList.addAll(newEntrants);
        notifyDataSetChanged(); // Notifies the attached observers that the data set has changed.
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     * It holds the references to the UI components for a single list item, which improves
     * performance by avoiding repeated {@code findViewById()} calls.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTextView;

        /**
         * Constructs a new {@code EntrantViewHolder}.
         *
         * @param itemView The view that represents a single item in the list.
         */
        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.entrant_name_text_view);
        }

        /**
         * Binds an {@link Entrant} object to the ViewHolder's views. This method takes the
         * data from the entrant and populates the UI components.
         *
         * @param entrant The entrant object containing the data to display.
         */
        public void bind(Entrant entrant) {
            nameTextView.setText(entrant.getUserName());
        }
    }
}