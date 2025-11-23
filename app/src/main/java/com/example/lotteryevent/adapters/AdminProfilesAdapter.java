package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.User;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter binds user objects to UI elements in item_entrant.xml
 * Each list item shows the user's name and has a click listener for profile selection
 */
public class AdminProfilesAdapter extends RecyclerView.Adapter<AdminProfilesAdapter.ProfileViewHolder> {

    public interface OnItemClickListener {
        void onClick(User user);
    }

    private final OnItemClickListener listener;
    private List<User> _profileList = new ArrayList<>();
    public AdminProfilesAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

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

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.entrant_name_text_view);
        }

        public void bind(User user, OnItemClickListener listener) {
            name.setText(user.getName());
            itemView.setOnClickListener(v -> listener.onClick(user));
        }
    }
}
