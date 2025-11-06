package com.example.lotteryevent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.viewmodels.HomeViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * A {@link Fragment} that serves as the main home screen of the application.
 * <p>
 * This fragment is responsible for displaying the primary content to the user: a grid
 * of their created events, fetched from Firestore. It uses a {@link HomeViewModel} to manage
 * data operations and a {@link RecyclerView} with a {@link EventAdapter} to display the grid.
 * It also manages its own toolbar title and options menu, including a profile icon.
 *
 * @see MainActivity
 * @see HomeViewModel
 * @see EventAdapter
 */
public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private EventAdapter eventAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;


    /**
     * Required empty public constructor for fragment instantiation by the system.
     * Fragments must have a no-argument constructor so that they can be re-instantiated
     * by the framework during configuration changes or process death.
     */
    public HomeFragment() { }

    /**
     * Called to have the fragment instantiate its user interface view.
     * <p>
     * This method inflates the layout for this fragment and initializes the {@link HomeViewModel}.
     * The ViewModel is scoped to this fragment's lifecycle.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     *                           The fragment should not add the view itself, but this can be used to generate
     *                           the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     *                           as given here.
     * @return The inflated {@link View} for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize the ViewModel using ViewModelProvider, which correctly scopes it to the fragment's lifecycle.
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view.
     * <p>
     * This method performs all initial setup of the view and its components. It sets the title on the
     * activity's action bar, initializes the fragment-specific options menu, configures the RecyclerView,
     * sets up listeners for UI interactions like pull-to-refresh and the FAB, and initiates data observation.
     * Finally, it triggers the initial data fetch.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     *                           as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Toolbar title and menu
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Your Events");
        }
        setupMenu();

        // Setup main UI components
        setupRecyclerView(view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> homeViewModel.fetchUserEvents());

        FloatingActionButton fab = view.findViewById(R.id.fab_add_event);
        fab.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_createEventFragment));

        // Setup observers to connect data from ViewModel to the UI
        setupObservers();

        // Trigger the initial data fetch from the ViewModel
        homeViewModel.fetchUserEvents();
    }

    /**
     * Initializes the RecyclerView and its associated components.
     * <p>
     * This method performs the following setup steps:
     * <ul>
     *     <li>Finds the {@link RecyclerView} by its ID in the fragment's view.</li>
     *     <li>Creates a new instance of the {@link EventAdapter}.</li>
     *     <li>Attaches the adapter to the RecyclerView. The layout manager (GridLayoutManager)
     *         is defined in the corresponding XML layout file ({@code fragment_home.xml}).</li>
     *     <li>Sets an item click listener on the adapter. When an event item is clicked, it navigates
     *         to the {@code OrganizerEventPageFragment}, passing the event's ID as a safe argument.
     *         It includes a check to prevent navigation if the event ID is null.</li>
     * </ul>
     *
     * @param view The parent view of the fragment, which contains the RecyclerView.
     */
    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.events_recycler_view);
        eventAdapter = new EventAdapter(R.layout.tile_event);
        recyclerView.setAdapter(eventAdapter);

        eventAdapter.setOnItemClickListener(event -> {
            // Make sure the event ID is not null before navigating
            if (event.getEventId() != null) {
                // Use the generated NavDirections class for type-safe navigation
                HomeFragmentDirections.ActionHomeFragmentToOrganizerEventPageFragment action =
                        HomeFragmentDirections.actionHomeFragmentToOrganizerEventPageFragment(event.getEventId());
                Navigation.findNavController(view).navigate(action);
            } else {
                Toast.makeText(getContext(), "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up observers on the {@link LiveData} objects from the {@link HomeViewModel}.
     * This enables the UI to automatically update in response to data changes.
     * <p>
     * - Observes the list of events and submits them to the {@link EventAdapter}.
     * - Observes the loading status to show or hide the {@link SwipeRefreshLayout} progress indicator.
     */
    private void setupObservers() {
        // Observe the list of events from the ViewModel. When the data changes (e.g., after a fetch),
        // update the RecyclerView adapter with the new list.
        homeViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                eventAdapter.setEvents(events);
            }
        });

        // Observe the loading state to show/hide the SwipeRefreshLayout's spinner.
        // This provides visual feedback to the user during data fetching.
        homeViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
    }

    /**
     * Sets up the fragment-specific options menu in the toolbar.
     * <p>
     * This method uses the modern {@link MenuProvider} API, which is lifecycle-aware. The menu is
     * associated with the fragment's view lifecycle, ensuring it is only created and visible when
     * the fragment is in the {@link Lifecycle.State#RESUMED} state. This prevents menu items from
     * persisting when navigating to other fragments.
     * <p>
     * It inflates the {@code R.menu.home_fragment_menu} and handles click events on the profile icon
     * item ({@code R.id.profile_icon}).
     *
     * @see MenuProvider
     * @see Lifecycle.State
     */
    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {

            /**
             * Called by the MenuHost to allow the MenuProvider to inflate its menu items into the given Menu.
             *
             * @param menu         The menu to inflate items into.
             * @param menuInflater The inflater to be used to inflate the menu.
             */
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // Use the menu file created for the profile icon
                menuInflater.inflate(R.menu.home_fragment_menu, menu);
            }

            /**
             * Called by the MenuHost to handle selection of a menu item.
             *
             * @param menuItem The menu item that was selected.
             * @return {@code true} if the menu item selection was handled, {@code false} otherwise.
             */
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // Handle clicks on the profile icon
                if (menuItem.getItemId() == R.id.profile_icon) {
                    NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.action_homeFragment_to_userProfileFragment);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}