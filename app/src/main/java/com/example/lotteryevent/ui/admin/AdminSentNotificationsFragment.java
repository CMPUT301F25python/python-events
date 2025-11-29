package com.example.lotteryevent.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.lotteryevent.R;

/**
 * Fragment to redirect admin to event-selection screen where they can choose an event and view relevant notifications
 */
public class AdminSentNotificationsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.action_adminSentNotificationsFragment_to_adminSelectEventFragment);

        return new View(requireContext());
    }

}
