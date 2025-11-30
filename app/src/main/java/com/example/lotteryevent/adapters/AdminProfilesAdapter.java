package com.example.lotteryevent.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.AdminImageItem;
import com.example.lotteryevent.data.User;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter binds user objects to UI elements in item_entrant.xml
 * Each list item shows the user's name and has a click listener for profile selection
 */
public class AdminProfilesAdapter extends RecyclerView.Adapter<AdminProfilesAdapter.ProfileViewHolder> {

    /**
     * Interface definition for a callback to be invoked when a profile is clicked.
     */
    public interface OnItemClickListener {
        /**
         * Called when an item has been clicked.
         * @param user The {@link User} that was clicked.
         */
        void onClick(User user);
    }

    private final OnItemClickListener listener;
    private List<User> _profileList = new ArrayList<>();

    /**
     * Adds listener to instance
     * @param listener listener to add
     */
    public AdminProfilesAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * sets profile list and notify of update
     * @param list profile list
     */
    public void setProfiles(List<User> list) {
        this._profileList = list;
        notifyDataSetChanged();
    }

    /**
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     */
    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
        return new ProfileViewHolder(v);
    }

    /**
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        User user = _profileList.get(position);
        holder.bind(user, listener);
    }

    /**
     * Gets number of users in profile list
     */
    @Override
    public int getItemCount() {
        return _profileList.size();
    }

    /**
     * ViewHolder class representing a single user in the list
     * Binds user's name to a textview and handles clicks
     */
    static class ProfileViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;

        /**
         * gets name of the view holder
         * @param itemView item being shown
         */
        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.entrant_name_text);
        }

        /**
         * Displays name of the user being shown
         * @param user user being shown in holder
         * @param listener sets behaviour on click
         */
        public void bind(User user, OnItemClickListener listener) {
            String displayName = user.getName();

            if(TextUtils.isEmpty(displayName)) {
                displayName = "Unnamed User";
            }

            name.setText(displayName);
            /**
             * Sets behaviour on item click
             * @param v view clickec
             */
            itemView.setOnClickListener(v -> listener.onClick(user));
        }
    }
}
