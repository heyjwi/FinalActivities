package com.example.cnpalabamanagementapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private List<Order> ordersList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrdersAdapter(List<Order> ordersList, OnOrderClickListener listener) {
        this.ordersList = ordersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dashboard_order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = ordersList.get(position);

        // Verify all views are not null before setting text
        if (holder.orderNumber != null) {
            holder.orderNumber.setText("Order No." + order.getOrderNumber());
        } else {
            Log.e("OrdersAdapter", "orderNumber TextView is null!");
        }

        if (holder.customerName != null) {
            holder.customerName.setText(order.getCustomerName());
        } else {
            Log.e("OrdersAdapter", "customerName TextView is null!");
        }

        if (holder.orderStatus != null) {
            holder.orderStatus.setText("Status: " + order.getStatus());
        } else {
            Log.e("OrdersAdapter", "orderStatus TextView is null!");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ordersList.size();
    }

    public void updateData(List<Order> newOrders) {
        ordersList = newOrders;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber;
        TextView customerName;  // This must match customerFirstAndLastName in XML
        TextView orderStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                orderNumber = itemView.findViewById(R.id.orderNumber);
                customerName = itemView.findViewById(R.id.customerFirstAndLastName); // Must match XML
                orderStatus = itemView.findViewById(R.id.orderStatus);

                // Debug check
                if (orderNumber == null || customerName == null || orderStatus == null) {
                    throw new RuntimeException("Some views not found in layout");
                }
            } catch (Exception e) {
                Log.e("OrderViewHolder", "Error finding views: " + e.getMessage());
                throw e;
            }
        }
    }
}