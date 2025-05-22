package com.example.cnpalabamanagementapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private Context context;
    private List<Order> orderList;
    private OnOrderClickListener onOrderClickListener;
    private OnOrderLongClickListener onOrderLongClickListener;

    // Interface for click events
    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public interface OnOrderLongClickListener {
        void onOrderLongClick(Order order);
    }

    public OrderAdapter(List<Order> orderList, OnOrderClickListener clickListener, OnOrderLongClickListener longClickListener) {
        this.orderList = orderList;
        this.onOrderClickListener = clickListener;
        this.onOrderLongClickListener = longClickListener;
    }


    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.orderNum.setText("Order No. " + order.getOrderNumber());
        holder.orderDateCreated.setText(order.getFormattedDate());
        holder.customerName.setText(order.getCustomerName());
        holder.serviceType.setText(order.getServiceType());
        holder.orderQuickStatus.setText(order.getStatus());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onOrderClickListener != null) {
                onOrderClickListener.onOrderClick(order);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onOrderLongClickListener != null) {
                onOrderLongClickListener.onOrderLongClick(order);
                return true; // Consume the long click
            }
            return false;
        });

    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderNum, orderDateCreated, customerName, serviceType, orderQuickStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderNum = itemView.findViewById(R.id.orderNum);
            orderDateCreated = itemView.findViewById(R.id.orderDateCreated);
            customerName = itemView.findViewById(R.id.customerName);
            serviceType = itemView.findViewById(R.id.serviceType);
            orderQuickStatus = itemView.findViewById(R.id.orderQuickStatus);
        }
    }

    public void updateList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }
}