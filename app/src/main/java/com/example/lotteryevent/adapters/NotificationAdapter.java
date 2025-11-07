package com.example.lotteryevent.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.data.Notification;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private final List<Notification> notificationsList = new ArrayList<>();
    private NotificationAdapter.OnItemClickListener listener;
    private final int rowLayoutResId;

    public NotificationAdapter(@LayoutRes int rowLayoutResId) {
        this.rowLayoutResId = rowLayoutResId;
    }

    /**
     * A listener interface for receiving callbacks when an item in the RecyclerView is clicked.
     * The hosting Fragment or Activity must implement this interface to handle navigation
     * or other actions.
     */
    public interface OnItemClickListener {
        /**
         * Called when an event tile in the RecyclerView is clicked.
         *
         * @param notification The {@link Notification} object corresponding to the clicked item.
         */
        void onItemClick(Notification notification);
    }

    /**
     * Registers a callback to be invoked when an item in this adapter has been clicked.
     *
     * @param listener The callback that will be executed.
     */
    public void setOnItemClickListener(NotificationAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Called when the RecyclerView needs a new {@link EventAdapter.EventViewHolder} to represent an item.
     * <p>
     * This new ViewHolder is constructed with a new View that is inflated from the
     * {@code R.layout.tile_event} XML layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new {@link EventAdapter.EventViewHolder} that holds a View for an event tile.
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(rowLayoutResId, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * updates the contents of the {@link EventAdapter.EventViewHolder#itemView} to reflect the item at the
     * given position.
     *
     * @param holder   The {@link EventAdapter.EventViewHolder} which should be updated to represent the
     *                 contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationsList.get(position);
        holder.bind(notification);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in the list.
     */
    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    /**
     * Updates the list of events displayed by the adapter. This method clears the current
     * list and replaces it with the new list, then notifies the RecyclerView that the
     * data set has changed.
     *
     * @param newNotifications The new list of {@link Event} objects to be displayed.
     */
    public void setNotifications(List<Notification> newNotifications) {
        this.notificationsList.clear();
        this.notificationsList.addAll(newNotifications);
        notifyDataSetChanged(); // In a more complex app, consider using DiffUtil for better performance.
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an event tile view and its metadata
     * within the RecyclerView. It holds references to the individual views within the tile
     * (e.g., TextView, ImageView) and handles click events.
     */
    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView message;
        private final TextView timestamp;
        private final MaterialCardView notifications_material_card;

        /**
         * Constructs a new {@link EventAdapter.EventViewHolder}.
         *
         * @param itemView The view that represents a single event tile, inflated from a layout file.
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_message);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
            notifications_material_card = itemView.findViewById(R.id.notifications_material_card);

            // Set a click listener on the entire tile view.
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                v.setBackgroundColor(Color.rgb(230, 232, 230));
                // Ensure the position is valid and a listener is registered before triggering the callback.
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(notificationsList.get(position));
                }
            });
        }

        void bind(Notification notification) {
            String message = null;

            if (Objects.equals(notification.getType(), "lottery_win")) {
                message = "\uD83C\uDF89 You've been selected for " + notification.getEventName() + "! Tap to accept or decline.";
            } else if (Objects.equals(notification.getType(), "lottery_loss")) {
                message = "❌ You weren't selected for " + notification.getEventName() + ".";
            } else if (Objects.equals(notification.getType(), "event_update")) {
                message = "\uD83D\uDD01 A spot just opened for " + notification.getEventName() + "! Join again for another chance.";
            } else {
                message = "\uD83D\uDCAC Message from the organizer of " + notification.getEventName() + ": " + notification.getMessage();
            }

            Timestamp timestampRaw = notification.getTimestamp();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String timestamp = dateFormat.format(timestampRaw.toDate());

            if (notification.getSeen() == true) {
                notifications_material_card.setBackgroundColor(Color.rgb(230, 232, 230));
            }

            this.message.setText(message);
            this.timestamp.setText(timestamp);
        }
    }
}