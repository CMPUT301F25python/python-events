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
import com.example.lotteryevent.viewmodels.LotteryGuidelinesViewModel;

public class LotteryGuidelinesFragment extends Fragment {

    private LotteryGuidelinesViewModel mViewModel;

    public static LotteryGuidelinesFragment newInstance() {
        return new LotteryGuidelinesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lottery_guidelines, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LotteryGuidelinesViewModel.class);
        // TODO: Use the ViewModel
    }

}