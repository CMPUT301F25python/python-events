package com.example.lotteryevent.ui;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lotteryevent.R;
import com.example.lotteryevent.viewmodels.RegistrationHistoryViewModel;

public class RegistrationHistoryFragment extends Fragment {

    private RegistrationHistoryViewModel mViewModel;

    public static RegistrationHistoryFragment newInstance() {
        return new RegistrationHistoryFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration_history, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(RegistrationHistoryViewModel.class);
        // TODO: Use the ViewModel
    }

}