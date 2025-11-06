package com.example.lotteryevent;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A class that provides management of notifications including sending notifications, by creating notification
 * documents under a given user's notification collection in Firebase, setting up a notification channel, and
 * generating notifications to be displayed to the user.
 */
public class NotificationCustomManager {
    private static final String TAG = "NotificationCustomManager";
    Context myContext;
    private String channelID = "lottery_win_notifications";
    private String channelDescription = "Notifications on winning the lottery.";
    private FirebaseFirestore db;
    private final static AtomicInteger c = new AtomicInteger(0);


    /**
     * Increments to get the next ID used for a notification
     * @return generated ID
     */
    public static int getID() {
        return c.incrementAndGet();
    }


    /**
     * Sets up manager instance and creates notification channel
     * @param context Context from the fragment creating a NotificationCustomManager object, used for toasts, intents
     */
    public NotificationCustomManager(Context context) {
        db = FirebaseFirestore.getInstance();
        myContext = context;
        createNotificationChannel();
    }

    /**
     * will be implemented in task 01.04.01
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, channelDescription, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(this.channelDescription);

            NotificationManager notificationManager = myContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Sends notification to the specified user by creating a notification document under the user's notification collection.
     * @param uid User ID of the user the notif is directed to
     * @param title Title of the notification
     * @param message Message content
     * @param eventId Event ID that the notif relates to
     * @param eventName Event name that the notif relates to
     * @param organizerId Organizer ID who sent the notif
     * @param organizerName Organizer Name who sent the notif
     */
    public void sendNotification(String uid, String title, String message, String eventId, String eventName, String organizerId, String organizerName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("eventName", eventName);
        notification.put("organizerId", organizerId);
        notification.put("organizerName", organizerName);
        notification.put("seen", false);
        String time = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.CANADA).format(new Date());
        notification.put("timestamp", time);

        // adds notif doc to the user's notif collection
        db.collection("users").document(uid).collection("notifications").add(notification)
                .addOnSuccessListener(v -> Log.d(TAG, "Notification added for user ID " + uid + " with notification ID " + v.getId()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to add notification for user " + uid, e);
                    Toast.makeText(myContext, "Failed to send notification to user " + uid, Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("MissingPermission")
    public void generateNotification(String title, String message) {
        // Intent that triggers when the notification is tapped
        Intent intent = new Intent(this.myContext, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this.myContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.myContext, channelID)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        // Display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.myContext);
        notificationManager.notify(getID(), builder.build());
    }

    public void checkAndDisplayUnreadNotifications(String uid) {
        db.collection("users").document(uid).collection("notifications")
            .whereEqualTo("seen", false).get()
            .addOnSuccessListener(notifs -> {
                int size = notifs.size();
                if (size == 1) {
                    DocumentSnapshot d = notifs.getDocuments().get(0);
                    String title = d.getString("title");
                    String message = d.getString("message");
                    String eventName = d.getString("eventName");
                    String organizerName = d.getString("organizerName");
                    String timestamp = d.getString("timestamp");

                    String fullMessage = message + "\n——————————————\nFrom organizer: " + organizerName + "\nEvent: " + eventName + "\n" + timestamp;

                    generateNotification(title, fullMessage);
                } else if (size > 1) {
                    String title = "You have " + String.valueOf(size) + "unread notifications";
                    String message = "Click here or go to the notifications section to see all messages you missed!";

                    generateNotification(title, message);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Failed to get notifications for user " + uid, e);
                Toast.makeText(myContext, "Failed to get notification for user", Toast.LENGTH_SHORT).show();
            });
    }

    public void listenForNotifications(String uid) {
        AtomicBoolean isFirstListener = new AtomicBoolean(true);
        db.collection("users").document(uid).collection("notifications").whereEqualTo("seen", false)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (isFirstListener.get()) {
                            isFirstListener.set(false);
                            return;
                        }
                        if (e != null) {
                            Log.w(TAG, "Listen failed with an error", e);
                            return;
                        }
                        if (value == null) {
                            Log.w(TAG, "No change to notifications found");
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == ADDED) {
                                DocumentSnapshot d = dc.getDocument();
                                String title = d.getString("title");
                                String message = d.getString("message");
                                String eventName = d.getString("eventName");
                                String organizerName = d.getString("organizerName");
                                String timestamp = d.getString("timestamp");

                                String fullMessage = message + "\n——————————————\nFrom organizer: " + organizerName + "\nEvent: " + eventName + "\n" + timestamp;

                                generateNotification(title, fullMessage);
                            }
                        }
                    }
                });
    }
}
