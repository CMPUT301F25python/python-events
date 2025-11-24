package com.example.lotteryevent.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.AdminImageItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying a grid or list of images in the Admin Dashboard.
 * <p>
 * This adapter handles the binding of {@link AdminImageItem} objects to the view.
 * It specifically handles the decoding of Base64 encoded image strings into {@link Bitmap}
 * objects for display in an {@link ImageView}.
 * </p>
 */
public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageHolder> {

    /**
     * Interface definition for a callback to be invoked when an image is clicked.
     */
    public interface OnItemClickListener {
        /**
         * Called when an item has been clicked.
         * @param item The {@link AdminImageItem} that was clicked.
         */
        void onClick(AdminImageItem item);
    }

    private List<AdminImageItem> images = new ArrayList<>();
    private final OnItemClickListener listener;

    /**
     * Constructs a new AdminImagesAdapter.
     *
     * @param listener The callback listener to handle item click events.
     */
    public AdminImagesAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of images displayed by the adapter and notifies the RecyclerView to refresh.
     *
     * @param list The new list of {@link AdminImageItem} objects to display.
     */
    public void setImages(List<AdminImageItem> list) {
        this.images = list;
        notifyDataSetChanged();
    }

    /**
     * Called when the RecyclerView needs a new {@link ImageHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ImageHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageHolder(v);
    }

    /**
     * Binds the data from the {@link AdminImageItem} at the given position to the ViewHolder.
     * <p>
     * This method decodes the Base64 string from the item into a {@link Bitmap}.
     * If decoding fails (e.g., invalid Base64 string), a default fallback report icon is displayed.
     * It also sets up the click listener for the item.
     * </p>
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
        AdminImageItem item = images.get(position);
        String base64Image = item.getBase64Image();

        try {
            // Decode Base64 string to Bitmap
            byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            // Fallback if decoding fails
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {

        return images.size();
    }

    /**
     * A ViewHolder that describes an item view and metadata about its place within the RecyclerView.
     */
    static class ImageHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;


        /**
         * Constructs the ImageHolder.
         * @param itemView The root view of the individual list item.
         */
        public ImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.admin_image_thumb);
        }
    }
}
