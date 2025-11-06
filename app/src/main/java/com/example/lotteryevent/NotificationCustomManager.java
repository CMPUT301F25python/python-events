package com.example.lotteryevent;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NotificationCustomManager {
    private static final String TAG = "NotificationCustomManager";
    Context myContext;
    private String channelID = "lottery_win_notifications";
    private String channelDescription = "Notifications on winning the lottery.";
    private FirebaseFirestore db;
    private final static AtomicInteger c = new AtomicInteger(0);

    public static int getID() {
        return c.incrementAndGet();
    }

    public NotificationCustomManager(Context context) {
        db = FirebaseFirestore.getInstance();
        myContext = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, channelDescription, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(this.channelDescription);

            NotificationManager notificationManager = myContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("MissingPermission")
    public void generateNotification() {
        // Intent that triggers when the notification is tapped
        Intent intent = new Intent(this.myContext, AfterNotification.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this.myContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.myContext, channelID)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle("Congratulations!")
                .setContentText("You won the lottery for ____! Confirm your registration now!")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        // Display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.myContext);
        notificationManager.notify(getID(), builder.build());
    }

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

        db.collection("users").document(uid).collection("notifications").add(notification)
                .addOnSuccessListener(v -> Log.d(TAG, "Notification added for user ID " + uid + " with notification ID " + v.getId()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to add notification for user " + uid, e);
                    Toast.makeText(myContext, "Failed to send notification to user " + uid, Toast.LENGTH_SHORT).show();
                });
    }

    public void checkAndDisplayUnreadNotifications(String uid) {
        db.collection("users").document(uid).collection("notifications")
    }
}
