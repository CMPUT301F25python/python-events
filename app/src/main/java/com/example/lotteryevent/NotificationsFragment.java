package com.example.lotteryevent;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.adapters.NotificationAdapter;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.data.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This fragment is used to view all notifications, included those already seen. If a notification
 * indicates a lottery win, the user can click on it to go to the even details page to accept or
 * decline the invitation. To mark a notif as seen, need to click on it.
 */
public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ListenerRegistration registration;
    private NotificationCustomManager notificationCustomManager;

    /**
     * Inflates layout
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    /**
     * Sets up recycler view and adapter, gets notifications to show
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Use the adapter constructor that takes the row layout you want here
        adapter = new NotificationAdapter(R.layout.item_notification);
        recyclerView.setAdapter(adapter);

        notificationCustomManager = new NotificationCustomManager(getContext());
        String notificationId = (getArguments() != null) ? getArguments().getString("notificationId") : null;
        if (notificationId != null) { // set notif as seen
            notificationCustomManager.markNotificationAsSeen(notificationId);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            db.collection("notifications").whereEqualTo("recipientId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (!isAdded()) return;
                        if (e != null) {
                            Log.w(TAG, "Load failed with an error", e);
                            return;
                        }
                        if (value == null) {
                            Log.w(TAG, "No snapshot found");
                            return;
                        }

                        List<Notification> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification notification = doc.toObject(Notification.class);
                            list.add(notification);
                        }
                        adapter.setNotifications(list);
                        adapter.setOnItemClickListener(n -> redirectOnNotificationClick(n, view));
                    }
                });
        }
    }

    /**
     * Marks notif as seen and if a lottery success notif, redirects to event page
     * @param notification notification clicked on
     * @param view view of fragment
     */
    private void redirectOnNotificationClick(Notification notification, View view) {
        if (notification.getSeen() != true) {
            notification.setSeen(true);
            db.collection("notifications")
                    .document(notification.getNotificationId())
                    .update("seen", true)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("FIRESTORE_SUCCESS", "Notification updated with ID: " + notification.getNotificationId());
                    })
                    .addOnFailureListener(e -> {
                        Log.w("FIRESTORE_ERROR", "Error updating document", e);
                        Toast.makeText(getContext(), "Error updating notification", Toast.LENGTH_LONG).show();
                    });
        }
        if (Objects.equals(notification.getType(), "lottery_win")) {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", notification.getEventId());
            Navigation.findNavController(view)
                    .navigate(R.id.action_notificationsFragment_to_eventDetailsFragment, bundle);
        }
    }
}
