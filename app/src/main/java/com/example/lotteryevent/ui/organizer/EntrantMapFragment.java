package com.example.lotteryevent.ui.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.EntrantListRepositoryImpl;
import com.example.lotteryevent.viewmodels.EntrantMapViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.repository.IEntrantListRepository;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class EntrantMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "EntrantMapFragment";
    private String eventId;
    private GoogleMap map;
    private ProgressBar loadingProgressBar;

    // --- ViewModel ---
    private EntrantMapViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Required empty public constructor
     */
    public EntrantMapFragment() {}

    /**
     * Constructor used for testing, allowing injection of a custom ViewModelProvider.Factory instance.
     * @param factory a custom ViewModel factory to be used when instantiating the fragment's ViewModel
     */
    public EntrantMapFragment(GenericViewModelFactory factory) {
        this.viewModelFactory = factory;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = EntrantMapFragmentArgs.fromBundle(getArguments()).getEventId();
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_map, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view. This is where we finalize the
     * fragment's UI by initializing views and fetching data by attaching LiveData observers.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingProgressBar = view.findViewById(R.id.map_loading_bar);

        if (viewModelFactory == null) {
            IEntrantListRepository entrantListRepo = new EntrantListRepositoryImpl(getContext());
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(EntrantMapViewModel.class, () -> new EntrantMapViewModel(entrantListRepo, eventId));
            viewModelFactory = factory;
        }

        viewModel = new ViewModelProvider(this, viewModelFactory).get(EntrantMapViewModel.class);

        setupObservers();

        // MANUALLY ADD THE MAP FRAGMENT
        FragmentManager fm = getChildFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map_container);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.map_container, mapFragment);
            ft.commit();
        }

        // Request the map asynchronously
        mapFragment.getMapAsync(this);
    }

    /**
     * Called when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * @param googleMap The Google Map object
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;

        // Hide the progress bar now that the map is ready
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }

        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);

        if (viewModel.getEntrants().getValue() != null) {
            updateMapMarkers(viewModel.getEntrants().getValue());
        } else {
            LatLng defaultLocation = new LatLng(53.5461, -113.4938); // Edmonton
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f));
        }

        Log.d(TAG, "Map is ready for Event ID: " + eventId);
    }

    /**
     * Sets up LiveData observers for the ViewModel.
     */
    private void setupObservers() {
        if (eventId == null) {
            Toast.makeText(getContext(), "Error: Event Map not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getEntrants().observe(getViewLifecycleOwner(), entrants -> {
            updateMapMarkers(entrants);
        });
    }

    /**
     * Helper method to clear the map and draw markers for all entrants.
     * This handles the conversion from Firestore GeoPoint to Google Maps LatLng.
     */
    private void updateMapMarkers(List<Entrant> entrants) {
        if (map == null || entrants == null) {
            return;
        }

        map.clear(); // Clear old markers

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasValidLocation = false;

        for (Entrant entrant : entrants) {
            GeoPoint geoPoint = entrant.getGeoLocation();

            if (geoPoint != null) {
                LatLng userLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                String name = (entrant.getUserName() != null) ? entrant.getUserName() : "Anonymous Entrant";
                String snippet = "Status: " + entrant.getStatus();

                map.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title(name)
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                // Add this point to the boundaries builder
                builder.include(userLocation);
                hasValidLocation = true;
            }
        }

        // If we have at least one entrant with a location, zoom to fit them
        if (hasValidLocation) {
            try {
                LatLngBounds bounds = builder.build();
                int padding = 200; // Offset from edges of the map in pixels
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                Log.e(TAG, "Error moving camera to bounds", e);
            }
        } else {
            // If list is empty or no one has a location, stay at default (Edmonton)
            LatLng defaultLocation = new LatLng(53.5461, -113.4938);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f));
        }
    }
}