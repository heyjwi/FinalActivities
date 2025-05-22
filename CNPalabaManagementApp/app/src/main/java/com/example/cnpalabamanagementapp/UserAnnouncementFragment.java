package com.example.cnpalabamanagementapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;


public class UserAnnouncementFragment extends Fragment {
    private RecyclerView announcementsRecyclerView;
    private AnnouncementAdapter adapter;
    private List<Announcement> announcements = new ArrayList<>();
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_announcement, container, false);

        sessionManager = new SessionManager(requireContext());

        // Initialize RecyclerView
        announcementsRecyclerView = view.findViewById(R.id.announcements_recycler_view);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter
        adapter = new AnnouncementAdapter(announcements);
        announcementsRecyclerView.setAdapter(adapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Load announcements
        loadAnnouncements();

        return view;
    }

    private void loadAnnouncements() {
        db.collection("announcements")
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    announcements.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Announcement announcement = document.toObject(Announcement.class);
                        announcement.setId(document.getId());
                        announcements.add(announcement);

                        // If user is not admin, add to notifications
                        if (sessionManager.getRole().equals("user")) {
                            addToNotifications(announcement);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading announcements", Toast.LENGTH_SHORT).show();
                });
    }

    private void addToNotifications(Announcement announcement) {
        String currentUserId = sessionManager.getUserId();

        // Only create notifications for non-admin users
        if (!sessionManager.getRole().equals("user")) {
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e("Notification", "Cannot add notification - no user ID");
            return;
        }

        // Create notification object with all required fields
        Notification notification = new Notification(
                null, // auto-generated ID
                Notification.NotificationType.ANNOUNCEMENT,
                "New Announcement: " + announcement.getSubject(),
                announcement.getMessage(),
                new Date(),
                false,
                currentUserId, // to user's ID
                announcement.getId(), // announcement reference
                null, // fromUserId (null for system announcements)
                "System", // fromUsername
                sessionManager.getUsername() // toUsername
        );

        // Check for existing notification
        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("announcementId", announcement.getId())
                .whereEqualTo("userId", currentUserId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && (task.getResult() == null || task.getResult().isEmpty())) {
                        FirebaseFirestore.getInstance().collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("Notification", "Announcement notification added");
                                });
                    }
                });
    }
}