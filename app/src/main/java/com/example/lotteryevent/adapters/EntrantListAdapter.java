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

public class EntrantListAdapter extends RecyclerView.Adapter<EntrantListAdapter.EntrantViewHolder> {

    private final List<Entrant> entrantList;

    public EntrantListAdapter(List<Entrant> entrantList) {
        this.entrantList = entrantList;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrantList.get(position);
        holder.bind(entrant);
    }

    @Override
    public int getItemCount() {
        return entrantList.size();
    }

    /**
     * Replaces the data in the adapter with a new list and refreshes the RecyclerView.
     * @param newEntrants The new list of entrants to display.
     */
    public void updateEntrants(List<Entrant> newEntrants) {
        this.entrantList.clear();
        this.entrantList.addAll(newEntrants);
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class that describes an item view and its metadata within the RecyclerView.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.entrant_name_text_view);
        }

        public void bind(Entrant entrant) {
            nameTextView.setText(entrant.getUserName());
        }
    }
}