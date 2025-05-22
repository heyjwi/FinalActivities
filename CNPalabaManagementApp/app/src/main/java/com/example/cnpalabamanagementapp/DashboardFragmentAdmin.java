package com.example.cnpalabamanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.TaskExecutor;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardFragmentAdmin extends Fragment {

    private CardView totalOrdersCardview;
    private CardView staffOverViewCardView;
    private CardView fullServiceBtn;
    private CardView selfServiceBtn;
    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<Order> ordersList;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private TextView pendingOrdersText;
    private TextView completedOrdersText;
    private TextView totalStaffText;
    private TextView topStaffText;
    private ImageView notificationBtn;

    public DashboardFragmentAdmin() {
        super(R.layout.fragment_dashboard);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        notificationBtn = view.findViewById(R.id.admin_notifications);
        totalOrdersCardview = view.findViewById(R.id.totalOrdersCardview);
        staffOverViewCardView = view.findViewById(R.id.staffOverviewCard);
        totalOrdersCardview = view.findViewById(R.id.totalOrdersCardview);
        pendingOrdersText = view.findViewById(R.id.pending_orders);
        completedOrdersText = view.findViewById(R.id.completed_orders);
        countOrdersForCurrentUser();

        totalStaffText = view.findViewById(R.id.total_staff_txt);
        topStaffText = view.findViewById(R.id.top_staff_txt);
        loadStaffStatistics();
        
        fullServiceBtn = view.findViewById(R.id.full_service_btn);
        selfServiceBtn= view.findViewById(R.id.self_service_btn);

        // Set click listeners for service buttons
        fullServiceBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddOrder.class);
            intent.putExtra("SERVICE_TYPE", "FULL");
            startActivity(intent);
        });

        selfServiceBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddOrder.class);
            intent.putExtra("SERVICE_TYPE", "SELF");
            startActivity(intent);
        });

        totalOrdersCardview.setOnClickListener(v -> {
            // Navigate to Orders tab
            MainActivityAdmin activity = (MainActivityAdmin) getActivity();
            if (activity != null) {
                activity.navigateToTab(R.id.nav_orders);
            }
        });

        staffOverViewCardView.setOnClickListener(v -> {

            MainActivityAdmin activity = (MainActivityAdmin) getActivity();
            if (activity != null) {
                activity.navigateToTab(R.id.nav_employees);
            }
        });

        notificationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Notifications.class);
            startActivity(intent);
        });





        // Initialize RecyclerView
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize list and adapter
        ordersList = new ArrayList<>();
        ordersAdapter = new OrdersAdapter(ordersList, order -> {
            openOrderDetails(order);
        });

        ordersRecyclerView.setAdapter(ordersAdapter);

        loadActiveOrdersForCurrentUser();

        ImageView adminProfile = view.findViewById(R.id.admin_profile);
        adminProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdminProfile.class);
            startActivity(intent);
        });
    }

    private void openOrderDetails(Order order) {
        Intent intent = new Intent(getActivity(), ViewOrderDetails.class);
        intent.putExtra("order_id", order.getDocumentId());
        intent.putExtra("order_status", order.getStatus());
        startActivity(intent);
    }

    private void loadStaffStatistics() {
        // Get total staff count (users with role "user")
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("role", "user")  // Only count users with "user" role (staff)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalStaff = task.getResult().size();
                        totalStaffText.setText(String.valueOf(totalStaff));

                        // After getting total staff, find top performer
                        if (totalStaff > 0) {
                            findTopPerformingStaff();
                        } else {
                            topStaffText.setText("No staff available");
                        }
                    } else {
                        Log.e("Dashboard", "Error getting staff count", task.getException());
                        totalStaffText.setText("0");
                    }
                });
    }

    private void findTopPerformingStaff() {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnCompleteListener(staffTask -> {
                    if (staffTask.isSuccessful()) {
                        List<DocumentSnapshot> staffMembers = staffTask.getResult().getDocuments();
                        Map<String, Integer> staffCompletedOrders = new HashMap<>();
                        AtomicInteger processedCount = new AtomicInteger(0);

                        if (staffMembers.isEmpty()) {
                            topStaffText.setText("No staff available");
                            return;
                        }

                        // For each staff member, count their completed orders
                        for (DocumentSnapshot staff : staffMembers) {
                            String staffUsername = staff.getString("username");
                            String firstName = staff.getString("firstName");
                            String lastName = staff.getString("lastName");

                            // Concatenate the full name
                            String fullName = (firstName + " " + lastName).trim();

                            FirebaseFirestore.getInstance().collection("orders")
                                    .whereEqualTo("handlerName", staffUsername)
                                    .whereEqualTo("status", "Completed")
                                    .get()
                                    .addOnCompleteListener(orderTask -> {
                                        if (orderTask.isSuccessful()) {
                                            int completedCount = orderTask.getResult().size();
                                            staffCompletedOrders.put(fullName, completedCount);

                                            // When all staff have been processed
                                            if (processedCount.incrementAndGet() == staffMembers.size()) {
                                                determineTopStaff(staffCompletedOrders);
                                            }
                                        } else {
                                            Log.e("Dashboard", "Error counting orders for " + staffUsername, orderTask.getException());
                                            if (processedCount.incrementAndGet() == staffMembers.size()) {
                                                determineTopStaff(staffCompletedOrders);
                                            }
                                        }
                                    });
                        }
                    } else {
                        Log.e("Dashboard", "Error getting staff list", staffTask.getException());
                        topStaffText.setText("Error loading data");
                    }
                });
    }

    private void determineTopStaff(Map<String, Integer> staffCompletedOrders) {
        if (staffCompletedOrders.isEmpty()) {
            topStaffText.setText("No completed orders");
            return;
        }

        // Find staff with most completed orders
        Map.Entry<String, Integer> topPerformer = Collections.max(
                staffCompletedOrders.entrySet(),
                Map.Entry.comparingByValue()
        );

        if (topPerformer.getValue() > 0) {
            topStaffText.setText(topPerformer.getKey());
        } else {
            topStaffText.setText("No completed orders");
        }
    }

    private void countOrdersForCurrentUser() {
        String currentUsername = sessionManager.getUsername();
        if (currentUsername == null || currentUsername.isEmpty()) {
            Log.e("DashboardFragment", "No username found in session");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query for active orders (case-insensitive check)
        db.collection("orders")
                .whereEqualTo("handlerName", currentUsername)
                .whereIn("status", Arrays.asList(
                        "Received", "received",
                        "Washing/Drying", "washing/drying",
                        "Folding", "folding",
                        "Pickup", "pickup"
                ))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int activeCount = task.getResult().size();
                        Log.d("DashboardFragment", "Found " + activeCount + " active orders");
                        pendingOrdersText.setText(String.valueOf(activeCount));
                    } else {
                        Log.e("DashboardFragment", "Error getting active orders", task.getException());
                        pendingOrdersText.setText("0");
                    }
                });

        // Query for completed orders (case-insensitive)
        db.collection("orders")
                .whereEqualTo("handlerName", currentUsername)
                .whereIn("status", Arrays.asList("Completed", "completed"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int completedCount = task.getResult().size();
                        Log.d("DashboardFragment", "Found " + completedCount + " completed orders");

                        // Debug: Print all completed orders
                        for (DocumentSnapshot doc : task.getResult()) {
                            Log.d("DashboardFragment", "Completed order: " + doc.getId() +
                                    ", status: " + doc.getString("status") +
                                    ", handler: " + doc.getString("handlerName"));
                        }

                        completedOrdersText.setText(String.valueOf(completedCount));
                    } else {
                        Log.e("DashboardFragment", "Error getting completed orders", task.getException());
                        completedOrdersText.setText("0");
                    }
                });
    }

    private void navigateToAddOrder(boolean isFullService) {
        Intent intent = new Intent(getActivity(), AddOrder.class);
        intent.putExtra("SERVICE_TYPE", isFullService ? "FULL" : "SELF");
        startActivity(intent);
    }

    private void loadActiveOrdersForCurrentUser() {
        String currentUsername = sessionManager.getUsername();
        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("orders")
                .whereNotEqualTo("status", "Completed") // Active orders only
                .whereEqualTo("handlerName", currentUsername) // Only current user's orders
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return;

                    if (task.isSuccessful()) {
                        processQueryResults(task.getResult());
                    } else {
                        handleQueryError(task.getException());
                    }
                });
    }

    private void processQueryResults(QuerySnapshot snapshot) {
        if (getContext() == null) return;

        ordersList.clear();
        for (QueryDocumentSnapshot document : snapshot) {
            try {
                Order order = document.toObject(Order.class);
                order.setDocumentId(document.getId());
                ordersList.add(order);
            } catch (Exception e) {
                Log.e("DashboardFragment", "Error parsing document " + document.getId(), e);
            }
        }

        // Sort by timestamp descending (client-side if index isn't ready)
        Collections.sort(ordersList, (o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));

        if (ordersList.isEmpty()) {

        } else {
            ordersAdapter.updateData(ordersList);
        }
    }

    private void handleQueryError(Exception exception) {
        if (getContext() == null) return;

        Log.e("DashboardFragment", "Error loading orders", exception);

        String errorMessage = "Error loading orders";
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) exception;
            errorMessage += ": " + firestoreEx.getCode();

            // Special handling for index errors
            if (firestoreEx.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                errorMessage += "\nCreating index... try again in a few minutes";
                // You'll need to create a Firestore index for:
                // Collection: orders
                // Fields: status (ASC), handlerName (ASC), timestamp (DESC)
            }
        }

        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private void setupRealTimeUpdates() {
        String currentUsername = sessionManager.getUsername();
        if (currentUsername == null || currentUsername.isEmpty()) {
            return;
        }

        db.collection("orders")
                .whereNotEqualTo("status", "Completed")
                .whereEqualTo("handlerName", currentUsername)
                .addSnapshotListener((value, error) -> {
                    if (getContext() == null) return;

                    if (error != null) {
                        handleQueryError(error);
                        return;
                    }

                    if (value != null) {
                        processQueryResults(value);
                    }
                });
    }
}