package com.example.lotteryevent.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.AdminImagesAdapter;
import com.example.lotteryevent.repository.AdminImagesRepositoryImpl;
import com.example.lotteryevent.viewmodels.AdminImagesViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

/**
 * Fragment allowing administrators to browse all uploaded images.
 * Displays thumbnails in a grid layout.
 */
public class AdminImagesFragment extends Fragment {

    private AdminImagesViewModel viewModel;
    private RecyclerView recycler;
    private AdminImagesAdapter adapter;
    private ProgressBar progress;

    public AdminImagesFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_images, container, false);
    }

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

        adapter = new AdminImagesAdapter(imageUrl ->
                Toast.makeText(requireContext(),
                        "Clicked image", Toast.LENGTH_SHORT).show());

        recycler.setAdapter(adapter);

        setupObservers();
        viewModel.fetchImages();
    }

    private void setupObservers() {
        viewModel.getImages().observe(getViewLifecycleOwner(), list -> {
            adapter.setImages(list);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
