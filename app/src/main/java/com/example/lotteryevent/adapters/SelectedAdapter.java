package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;

import java.util.List;

/**
 * This adapter displayes selected lottery entrants
 *
 * <p>
 *     Groups a list of  user Ids that represent selected entrants
 *     and displays them as a scrollable list.
 * <p>
 *     Each item includes: user Id (displayed) as well as a cancel button allowing
 *     the organizer to remove the user from selected list
 * <p>
 * </p>
 */
public class SelectedAdapter extends RecyclerView.Adapter<SelectedAdapter.ViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClicked(String userId);
    }

    private List<String> selectedUsers;
    private OnCancelClickListener cancelListener;

    /**
     * Constructor for SelectedAdapter
     *
     * @param selectedUsers
     * @param cancelListener
     */
    public SelectedAdapter(List<String> selectedUsers, OnCancelClickListener cancelListener) {
        this.selectedUsers = selectedUsers;
        this.cancelListener = cancelListener;
    }

    /**
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     *
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.selected_user, parent, false);
        return new ViewHolder(view);
    }

    /**
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String userId = this.selectedUsers.get(position);
        holder.userIdText.setText(userId);

        holder.cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancelClicked(userId);
            }
        });
    }


    @Override
    public int getItemCount() {
        return this.selectedUsers.size();
    }

    /**
     * ViewHolder Class representing a single selected entrant row
     *
     * <p>
     *     References user Id and button to remove entrant
     * </p>
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userIdText;
        Button cancelButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userIdText = itemView.findViewById(R.id.userNameText);
            cancelButton = itemView.findViewById(R.id.cancelButton);
        }
    }
}
