package com.example.cnpalabamanagementapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Arrays;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;

public class Notifications extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notifications = new ArrayList<>();
    private ImageView addAnnouncementBtn;
    private ImageView backNotifications;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        backNotifications = findViewById(R.id.backNotifications);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        addAnnouncementBtn = findViewById(R.id.addAnnouncment);

        // Set visibility based on user role
        if (!sessionManager.getRole().equals("user")) {
            // Hide add announcement button for non-admin users
            addAnnouncementBtn.setVisibility(View.GONE);
        }

        adapter = new NotificationAdapter(notifications,
                new NotificationAdapter.OnNotificationClickListener() {
                    @Override
                    public void onNotificationClick(Notification notification) {
                        handleNotificationClick(notification);
                    }
                },
                sessionManager.getRole() // Pass user role to adapter
        );

        notificationsRecyclerView.setAdapter(adapter);

        addAnnouncementBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddAnnouncement.class);
            startActivity(intent);
        });

        backNotifications.setOnClickListener(v -> finish());

        loadNotifications();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void handleNotificationClick(Notification notification) {
        // Mark as read
        notification.setRead(true);
        adapter.notifyDataSetChanged();

        // Open appropriate screen based on notification type
        switch (notification.getType()) {
            case MESSAGE:
                openMessageScreen(notification);
                break;
            case REPORT:
                if (sessionManager.getRole().equals("admin")) {
                    openReportScreen(notification);
                }
                break;
            case ANNOUNCEMENT:
                openAnnouncementScreen(notification);
                break;
        }
    }


    private void loadNotifications() {
        String currentUserId = sessionManager.getUserId();

        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", currentUserId) // Only current user's notifications
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Notifications", "Error loading notifications", error);
                        return;
                    }

                    List<Notification> loadedNotifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setId(doc.getId());
                        loadedNotifications.add(notification);
                    }
                    adapter.updateNotifications(loadedNotifications);
                });
    }

    private void openMessageScreen(Notification notification) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("notification_id", notification.getId());
        startActivity(intent);
    }

    private void openReportScreen(Notification notification) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra("notification_id", notification.getId());
        startActivity(intent);
    }

    private void openAnnouncementScreen(Notification notification) {
        Intent intent = new Intent(this, AnnouncementActivity.class);
        intent.putExtra("notification_id", notification.getId());
        startActivity(intent);
    }
}
