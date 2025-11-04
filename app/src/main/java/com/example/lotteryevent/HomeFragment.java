package com.example.lotteryevent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * A {@link Fragment} that serves as the main home screen of the application.
 * <p>
 * This fragment is responsible for displaying the primary content to the user, a list
 * of their events. It also manages its own toolbar title and options menu, including a profile
 * icon. It inflates its UI from the {@code R.layout.fragment_home} layout resource.
 *
 * @see MainActivity
 */
public class HomeFragment extends Fragment {

    /**
     * Required empty public constructor for fragment instantiation by the system.
     * Fragments must have a no-argument constructor so that they can be re-instantiated
     * by the framework during configuration changes or process death.
     */
    public HomeFragment() { }

    /**
     * Called to have the fragment instantiate its user interface view. This is optional,
     * and non-graphical fragments can return null.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     *                           The fragment should not add the view itself, but this can be used to generate
     *                           the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     *                           as given here.
     * @return The inflated {@link View} for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,* but before any saved state has been restored in to the view.
     * <p>
     * This method performs initial setup of the view and its components. It sets the title on the
     * activity's action bar, initializes the fragment-specific options menu, and sets up the
     * OnClickListener for the FloatingActionButton to navigate to the event creation screen.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     *                           as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Your Events");
        setupMenu();

        FloatingActionButton fab = view.findViewById(R.id.fab_add_event);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_createEventFragment);
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
                // Use the new menu file we created for the profile icon
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
                    Toast.makeText(getContext(), "Profile clicked!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}