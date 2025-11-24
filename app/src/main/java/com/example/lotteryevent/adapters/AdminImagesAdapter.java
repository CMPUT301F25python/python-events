package com.example.lotteryevent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lotteryevent.R;

import java.util.ArrayList;
import java.util.List;

public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageHolder> {

    public interface OnItemClickListener {
        void onClick(String imageUrl);
    }

    private List<String> images = new ArrayList<>();
    private final OnItemClickListener listener;

    public AdminImagesAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setImages(List<String> list) {
        this.images = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
        String url = images.get(position);
        holder.bind(url, listener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;

        public ImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.admin_image_thumb);
        }

        public void bind(String url, OnItemClickListener listener) {
            Glide.with(imageView.getContext())
                    .load(url)
                    .placeholder(R.drawable.photo_library_24px)
                    .into(imageView);

            itemView.setOnClickListener(v -> listener.onClick(url));
        }
    }
}
