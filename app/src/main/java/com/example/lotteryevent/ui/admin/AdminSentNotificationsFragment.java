package com.example.lotteryevent.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.lotteryevent.R;

public class AdminSentNotificationsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_admin_sent_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Button btnSelectEvent = view.findViewById(R.id.btn_select_event);

        btnSelectEvent.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_adminSentNotificationsFragment_to_adminSelectEventFragment
                )
        );
    }
}
