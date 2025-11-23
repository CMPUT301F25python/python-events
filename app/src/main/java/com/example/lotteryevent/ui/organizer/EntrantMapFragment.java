package com.example.lotteryevent.ui.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lotteryevent.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class EntrantMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "EntrantMapFragment";
    private String eventId;
    private GoogleMap map;
    private ProgressBar loadingProgressBar;

    public EntrantMapFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = EntrantMapFragmentArgs.fromBundle(getArguments()).getEventId();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingProgressBar = view.findViewById(R.id.map_loading_bar);

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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;

        // Hide the progress bar now that the map is ready
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }

        LatLng defaultLocation = new LatLng(53.5461, -113.4938); // Edmonton
        map.addMarker(new MarkerOptions().position(defaultLocation).title("Event Location"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f));

        Log.d(TAG, "Map is ready for Event ID: " + eventId);
    }
}