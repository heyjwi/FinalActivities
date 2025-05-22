package com.example.cnpalabamanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


    public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderClickListener, OrderAdapter.OnOrderLongClickListener {

        private RecyclerView ordersRecyclerView;
        private OrderAdapter orderAdapter;
        private List<Order> orderList = new ArrayList<>();
        private TextView emptyView;
        private Spinner spinner;
        private SessionManager sessionManager;
        private String currentUsername;
        private static final String PREF_SELECTED_STATUS = "selected_order_status";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_orders, container, false);

            sessionManager = new SessionManager(requireContext());
            currentUsername = sessionManager.getUsername();
            ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView);
            emptyView = view.findViewById(R.id.emptyView); // Add this TextView to your layout
            spinner = view.findViewById(R.id.spinner);


            ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            orderAdapter = new OrderAdapter(orderList, this, this);
            ordersRecyclerView.setAdapter(orderAdapter);
            
            setupSpinner();
            setupAddOrderButton(view);
            loadOrdersFromFirestore();
            return view;

        }
        @Override
        public void onOrderLongClick(Order order) {
            showDeleteConfirmationDialog(order);
        }

        @Override
        public void onResume() {
            super.onResume();
            loadOrdersFromFirestore(); // Refresh data when returning to fragment
        }

        private void showDeleteConfirmationDialog(Order order) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Order")
                    .setMessage("Are you sure you want to delete Order #" + order.getOrderNumber() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteOrder(order))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteOrder(Order order) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("orders").document(order.getDocumentId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Order deleted", Toast.LENGTH_SHORT).show();
                        // The Firestore listener will automatically update the list
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to delete order", Toast.LENGTH_SHORT).show();
                        Log.e("DeleteOrder", "Error deleting order", e);
                    });
        }


        public void onOrderClick(Order order) {
            Intent intent = new Intent(getActivity(), ViewOrderDetails.class);
            intent.putExtra("order_id", order.getDocumentId());
            intent.putExtra("order_status", order.getStatus());
            startActivity(intent);
        }

        private void loadOrdersFromFirestore() {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("orders")
                    .whereEqualTo("handlerName", currentUsername) // Only get orders for current user
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot querySnapshot,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w("Firestore", "Listen failed.", e);
                                return;
                            }

                            orderList.clear();

                            if (querySnapshot != null) {
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Order order = document.toObject(Order.class);
                                    if (order != null) {
                                        order.setDocumentId(document.getId());
                                        orderList.add(order);
                                    }
                                }

                                orderAdapter.notifyDataSetChanged();
                                checkEmptyState();

                                if (spinner.getSelectedItem() != null) {
                                    filterOrders(spinner.getSelectedItem().toString());
                                }
                            }
                        }
                    });
        }

        private void checkEmptyState() {
            if (orderList.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                ordersRecyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                ordersRecyclerView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            checkEmptyState();
        }

        private void setupSpinner() {
            String[] items = {"All", "Received", "Washing/Drying", "Folding", "Pickup", "Completed"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    R.layout.spinner_item,
                    items
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            // Restore saved selection
                SharedPreferences prefs = requireContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
            String savedStatus = prefs.getString(PREF_SELECTED_STATUS, "All");

            // Set the spinner selection
            int position = adapter.getPosition(savedStatus);
            if (position >= 0) {
                spinner.setSelection(position);
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedStatus = parent.getItemAtPosition(position).toString();
                    filterOrders(selectedStatus);

                    // Save the selected status
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PREF_SELECTED_STATUS, selectedStatus);
                    editor.apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        private void filterOrders(String status) {
            List<Order> filteredList;
            if ("All".equals(status)) {
                filteredList = new ArrayList<>(orderList);
            } else {
                filteredList = new ArrayList<>();
                for (Order order : orderList) {
                    if (status.equals(order.getStatus())) {
                        filteredList.add(order);
                    }
                }
            }

            // Update the adapter with filtered list
            orderAdapter.updateList(filteredList);
            checkEmptyState();
        }


        private void setupAddOrderButton(View view) {
            ImageView addOrder = view.findViewById(R.id.addOrder);
            addOrder.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddOrder.class);
                // Explicitly set no service type selected
                intent.putExtra("SERVICE_TYPE", "");
                startActivity(intent);
            });
        }


    }