package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for admin view of event list
 * <p>
 *     Binds Event data to row layout from item_event.xml
 *     Displays event title
 * </p>
 */
public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.EventViewHolder> {

    public interface OnItemClickLister {
        void onClick(Event event);
    }

    private final OnItemClickLister listener;
    private List<Event> eventList = new ArrayList<>();

    public AdminEventsAdapter(OnItemClickLister listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.eventList = events;
        notifyDataSetChanged();
    }

    /**
     * Inflates row layout and creates new EventViewHolder
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(v);
    }

    /**
     * Binds event data to viewHolder at specific position
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    /**
     * Returns the number of events in current list
     * @return
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Viewholder class to represent a single event row in RecyclerView
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.event_title_text);
        }

        public void bind(Event event, OnItemClickLister listener) {
            title.setText(event.getName());

            itemView.setOnClickListener(v -> listener.onClick(event));
        }
    }
}
