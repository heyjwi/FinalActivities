package com.example.cnpalabamanagementapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class DashboardFragmentUser extends Fragment {
    private TextView welcomUserText, pendingOrdersText, completedOrdersText, totalEarnings;
    private SessionManager sessionManager;
    private CardView fullServiceBtn;
    private CardView selfServiceBtn;
    private CardView totalOrdersCardview;
    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<Order> ordersList;
    private FirebaseFirestore db;
    private ImageView notifications;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_user, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(getActivity());
        String username = sessionManager.getUsername();
        String fullName = sessionManager.getFullName();
        String role = sessionManager.getRole();
        String userId = sessionManager.getUserId();
        ImageView adminProfile = view.findViewById(R.id.user_profile);
        welcomUserText = view.findViewById(R.id.welcome_user_text);
        welcomUserText.setText("Welcome, " + capitalizeFirstLetter(username));
        adminProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserProfile.class);
            startActivity(intent);
        });
        notifications = view.findViewById(R.id.notifications);
        notifications.setOnClickListener(v -> {

        });
        totalOrdersCardview = view.findViewById(R.id.totalOrdersCardview);
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersList = new ArrayList<>();
        ordersAdapter = new OrdersAdapter(ordersList, order -> {
            openOrderDetails(order);
        });

        ordersRecyclerView.setAdapter(ordersAdapter);
        loadActiveOrdersForCurrentUser();
        
        
        totalOrdersCardview = view.findViewById(R.id.totalOrdersCardview);
        pendingOrdersText = view.findViewById(R.id.pending_orders);
        completedOrdersText = view.findViewById(R.id.completed_orders);
        countOrdersForCurrentUser();

        totalEarnings = view.findViewById(R.id.total_earnings);
        fullServiceBtn = view.findViewById(R.id.full_service_btn);
        selfServiceBtn = view.findViewById(R.id.self_service_btn);
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

//        notifications.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notifications.class);
//            startActivity(intent);
//        });
        calculateTotalEarnings();
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    private void openOrderDetails(Order order) {
        Intent intent = new Intent(getActivity(), ViewOrderDetails.class);
        intent.putExtra("order_id", order.getDocumentId());
        intent.putExtra("order_status", order.getStatus());
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

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;

        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    private void calculateTotalEarnings() {
        String currentUsername = sessionManager.getUsername();
        if (currentUsername == null || currentUsername.isEmpty()) {
            Log.e("DashboardFragment", "No username found in session");
            return;
        }

        db.collection("orders")
                .whereEqualTo("handlerName", currentUsername)
                .whereEqualTo("status", "Completed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double totalCompletedValue = 0;

                        for (DocumentSnapshot document : task.getResult()) {
                            // Safely get the total value
                            Object totalObj = document.get("total");
                            if (totalObj instanceof Number) {
                                totalCompletedValue += ((Number) totalObj).doubleValue();
                            }
                        }

                        // Calculate employee earnings (20% of total)
                        double employeeEarnings = totalCompletedValue * 0.20;

                        // Update UI
                        String formattedTotal = String.format("₱%.2f", totalCompletedValue);
                        String formattedEarnings = String.format("₱%.2f", employeeEarnings);

                        runOnUiThread(() -> {
                            totalEarnings.setText(formattedEarnings);
                            // If you want to show both values:
                            // totalEarnings.setText("Total: " + formattedTotal + "\nYour Earnings: " + formattedEarnings);
                        });

                        Log.d("Earnings", "Total completed: " + formattedTotal);
                        Log.d("Earnings", "Employee share: " + formattedEarnings);
                    } else {
                        Log.e("DashboardFragment", "Error calculating earnings", task.getException());
                        totalEarnings.setText("₱0.00");
                    }
                });
    }
}