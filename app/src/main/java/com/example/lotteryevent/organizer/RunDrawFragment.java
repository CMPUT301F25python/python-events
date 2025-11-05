package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;

import com.example.lotteryevent.LotteryManager;
import com.example.lotteryevent.R;

public class RunDrawFragment extends Fragment {
    private LotteryManager lotteryManager;
    private EditText numSelectedEntrants;
    private String eventId = "temporary filler for event ID";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_draw, container, false);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lotteryManager = new LotteryManager();
        numSelectedEntrants = view.findViewById(R.id.numSelectedEntrants);

        Button runDrawButton = view.findViewById(R.id.runDrawButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        runDrawButton.setOnClickListener(v -> {
            String inputText = numSelectedEntrants.getText().toString().trim();

            if (inputText.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }

            int numToSelect = Integer.parseInt(inputText);
            lotteryManager.selectWinners(eventId, numToSelect);

            Toast.makeText(getContext(), "Draw initialized!", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

}
