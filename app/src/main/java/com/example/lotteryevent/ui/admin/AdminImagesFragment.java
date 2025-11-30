package com.example.lotteryevent.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.AdminImagesAdapter;
import com.example.lotteryevent.data.AdminImageItem;
import com.example.lotteryevent.repository.AdminImagesRepositoryImpl;
import com.example.lotteryevent.viewmodels.AdminImagesViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import android.util.Base64;

/**
 * Fragment allowing administrators to browse all uploaded images.
 * Displays thumbnails in a grid layout.
 */
public class AdminImagesFragment extends Fragment {

    private AdminImagesViewModel viewModel;
    private RecyclerView recycler;
    private AdminImagesAdapter adapter;
    private ProgressBar progress;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Default empty constructor required for Fragment instantiation.
     */
    public AdminImagesFragment() {}

    /**
     * Constructor used for testing purposes to inject a mock ViewModelFactory.
     *
     * @param factory The ViewModelProvider.Factory to use.
     */
    public AdminImagesFragment(ViewModelProvider.Factory factory) {
        this.viewModelFactory = factory;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_images, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * <p>
     * This method initializes the ViewModel, RecyclerView, Adapter, and Observers.
     * It also triggers the initial fetch of images.
     * </p>
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup ViewModel
        GenericViewModelFactory factory = new GenericViewModelFactory();
        factory.put(AdminImagesViewModel.class,
                () -> new AdminImagesViewModel(new AdminImagesRepositoryImpl()));

        viewModel = new ViewModelProvider(this, factory)
                .get(AdminImagesViewModel.class);

        progress = view.findViewById(R.id.admin_images_loading);
        recycler = view.findViewById(R.id.admin_images_recycler);

        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new AdminImagesAdapter(this::showImageDialog);

        recycler.setAdapter(adapter);

        setupObservers();
        viewModel.fetchImages();
    }

    /**
     * Sets up observers for LiveData from the ViewModel.
     * Handles updates for the image list, loading state, and toast messages.
     */
    private void setupObservers() {
        /**
         * Observes list of images, updates adapter when changed
         * @param list contains list of images
         */
        viewModel.getImages().observe(getViewLifecycleOwner(), list -> {
            adapter.setImages(list);
        });
        /**
         * Observes loading boolean, shows progress conditionally based on loading
         * @param loading boolean of whether loading or not
         */
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
        /**
         * Observes message, if updates and contains one, make a toast
         * @param msg message to show
         */
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Creates and shows a dialog with the full-size image and a delete option.
     *
     * @param item The {@link AdminImageItem} to display and potentially delete.
     */
    private void showImageDialog(AdminImageItem item) {
        // 1. Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_admin_image_preview, null);

        ImageView imageView = dialogView.findViewById(R.id.dialog_image_view);
        Button deleteButton = dialogView.findViewById(R.id.dialog_delete_btn);

        // 2. Decode Base64 and set image
        try {
            byte[] decodedString = Base64.decode(item.getBase64Image(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImageResource(android.R.drawable.ic_menu_report_image); // Fallback
        }

        // 3. Create the Preview Dialog
        AlertDialog previewDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 4. Handle Delete Button Click -> Show Confirmation Dialog
        /**
         * Opens a confirmation dialog when the delete button is clicked.
         *
         * @param v The view that was clicked.
         */
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Image?")
                    .setMessage("Are you sure you want to permanently delete this image? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Perform the actual delete
                        viewModel.deleteImage(item.getEventId());
                        // Close both dialogs
                        dialog.dismiss();
                        previewDialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        previewDialog.show();
    }

}
