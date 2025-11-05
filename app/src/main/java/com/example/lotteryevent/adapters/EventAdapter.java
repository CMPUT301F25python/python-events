package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;
import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Event;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RecyclerView.Adapter} that is responsible for displaying a list of {@link Event}
 * objects in a grid format. It handles the creation and binding of views for each event tile.
 * This adapter also provides a mechanism for handling click events on individual tiles.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList = new ArrayList<>();
    private OnItemClickListener listener;

    /**
     * A listener interface for receiving callbacks when an item in the RecyclerView is clicked.
     * The hosting Fragment or Activity must implement this interface to handle navigation
     * or other actions.
     */
    public interface OnItemClickListener {
        /**
         * Called when an event tile in the RecyclerView is clicked.
         *
         * @param event The {@link Event} object corresponding to the clicked item.
         */
        void onItemClick(Event event);
    }

    /**
     * Registers a callback to be invoked when an item in this adapter has been clicked.
     *
     * @param listener The callback that will be executed.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Called when the RecyclerView needs a new {@link EventViewHolder} to represent an item.
     * <p>
     * This new ViewHolder is constructed with a new View that is inflated from the
     * {@code R.layout.tile_event} XML layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new {@link EventViewHolder} that holds a View for an event tile.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tile_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * updates the contents of the {@link EventViewHolder#itemView} to reflect the item at the
     * given position.
     *
     * @param holder   The {@link EventViewHolder} which should be updated to represent the
     *                 contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in the list.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Updates the list of events displayed by the adapter. This method clears the current
     * list and replaces it with the new list, then notifies the RecyclerView that the
     * data set has changed.
     *
     * @param newEvents The new list of {@link Event} objects to be displayed.
     */
    public void setEvents(List<Event> newEvents) {
        this.eventList.clear();
        this.eventList.addAll(newEvents);
        notifyDataSetChanged(); // In a more complex app, consider using DiffUtil for better performance.
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an event tile view and its metadata
     * within the RecyclerView. It holds references to the individual views within the tile
     * (e.g., TextView, ImageView) and handles click events.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final ImageView posterImageView;

        /**
         * Constructs a new {@link EventViewHolder}.
         *
         * @param itemView The view that represents a single event tile, inflated from a layout file.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.event_title_text);
            posterImageView = itemView.findViewById(R.id.event_poster_image);

            // Set a click listener on the entire tile view.
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                // Ensure the position is valid and a listener is registered before triggering the callback.
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(eventList.get(position));
                }
            });
        }

        /**
         * Binds an {@link Event} object's data to the views within the ViewHolder.
         * For this version, it only sets the event's name on a TextView.
         *
         * @param event The {@link Event} object containing the data to display.
         */
        public void bind(Event event) {
            titleTextView.setText(event.getName());
            // TODO: Future logic to load an image into posterImageView can be added here.
        }
    }
}
