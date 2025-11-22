package com.example.lotteryevent.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lotteryevent.R;

/**
 * A {@link Fragment} subclass that allows users to view the lottery guidelines.
 * Its sole responsibility is to display the data.
 */
public class LotteryGuidelinesFragment extends Fragment {

    /**
     * Inflates the fragment's layout.
     *
     * @param inflater The LayoutInflater object to inflate views.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // inflate layout for this fragment
        return inflater.inflate(R.layout.fragment_lottery_guidelines, container, false);
    }

}