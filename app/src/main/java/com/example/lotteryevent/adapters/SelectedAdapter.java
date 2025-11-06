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

public class SelectedAdapter extends RecyclerView.Adapter<SelectedAdapter.ViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClicked(String userId);
    }

    private List<String> selectedUsers;
    private OnCancelClickListener cancelListener;

    public SelectedAdapter(List<String> selectedUsers, OnCancelClickListener cancelListener) {
        this.selectedUsers = selectedUsers;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.selected_user, parent, false);
        return new ViewHolder(view);
    }

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
