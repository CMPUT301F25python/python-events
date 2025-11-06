package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ConfirmEntrantsNotifyFragment extends Fragment {
    private ArrayList<String> chosenEntrants;
    private NotificationCustomManager notifManager;
    private View view;
    private Button confirmAndNotifyButton;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private ArrayList<String> erroredUsers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_confirm_chosen_entrants_and_notfify, container, false);

        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        this.notifManager = new NotificationCustomManager(getContext());
        this.chosenEntrants = requireArguments().getStringArrayList("chosen_entrants");
        this.erroredUsers = new ArrayList<>();

        confirmAndNotifyButton = view.findViewById(R.id.confirm_entrants_notify_button);
        confirmAndNotifyButton.setOnClickListener(view -> {
            confirmChosenEntrantsAndNotify();

//            if (chosenEntrants.isEmpty()) {
//                Toast.makeText(getContext(), "No users to confirm and notify.", Toast.LENGTH_SHORT).show();
//            } else if (!confirmChosenEntrants()) {
//                Toast.makeText(getContext(), "Error found while confirming chosen entrants. Please contact support.", Toast.LENGTH_SHORT).show();
//            } else {
//                handleNotify();
//            }
        });
        return view;
    }

    private void confirmChosenEntrantsAndNotify() {
        if (chosenEntrants.isEmpty()) {
            Toast.makeText(getContext(), "No users to confirm and notify.", Toast.LENGTH_SHORT).show();
        } else {
            for (String user : chosenEntrants) {
                usersRef.document(user).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            handleNotify();
                        } else {
                            erroredUsers.add(user);
                        }
                    }
                });
            }
        }
    }

    private void handleNotify() {

    }

}
