package com.example.cnpalabamanagementapp;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;
    private String userRole;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications,
                               OnNotificationClickListener listener,
                               String userRole) {
        this.notifications = notifications;
        this.listener = listener;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Set icon based on type
        int iconRes = R.drawable.announcement; // default
        switch(notification.getType()) {
            case MESSAGE:
                iconRes = R.drawable.message_notif;
                holder.notificationSubject.setText(notification.getFromUsername() + " messaged you");
                break;
            case ANNOUNCEMENT:
                iconRes = R.drawable.announcement;
                holder.notificationSubject.setText(notification.getSubject());
                break;
            case REPORT:
                iconRes = R.drawable.warning;
                holder.notificationSubject.setText(notification.getSubject());
                break;
        }
        holder.notificationIcon.setImageResource(iconRes);

        // Set content (trim to one line if needed)
        String content = notification.getContent();
        if (content.length() > 50) {
            content = content.substring(0, 50) + "...";
        }
        holder.notificationContent.setText(content);

        // Format time
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
        holder.notificationTime.setText(sdf.format(notification.getTimestamp()));

        // Set read/unread indicator
        int indicatorColor = notification.isRead() ?
                ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_color) :
                ContextCompat.getColor(holder.itemView.getContext(), R.color.light_red);

        GradientDrawable indicator = (GradientDrawable) holder.notificationIndicator.getBackground();
        indicator.setColor(indicatorColor);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);

                // Mark as read when clicked
                if (!notification.isRead()) {
                    notification.setRead(true);
                    notifyItemChanged(position);

                    // Update in Firestore - different collections for notifications vs messages
                    String collection = notification.getType() == Notification.NotificationType.MESSAGE ?
                            "messages" : "notifications";

                    FirebaseFirestore.getInstance()
                            .collection(collection)
                            .document(notification.getId())
                            .update("isRead", true);
                }
            }
        });

        // Handle admin-specific views
        if (holder.removeButton != null) {
            holder.removeButton.setVisibility(
                    "admin".equals(userRole) ? View.VISIBLE : View.GONE
            );

            holder.removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        notifications.clear();
        notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private ImageView notificationIcon;
        private TextView notificationSubject;
        private TextView notificationContent;
        private TextView notificationTime;
        private View notificationIndicator;
        private ImageView removeButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationIcon = itemView.findViewById(R.id.notification_icon);
            notificationSubject = itemView.findViewById(R.id.notification_subject);
            notificationContent = itemView.findViewById(R.id.notification_content);
            notificationTime = itemView.findViewById(R.id.notification_time);
            notificationIndicator = itemView.findViewById(R.id.notification_indicator);
            removeButton = itemView.findViewById(R.id.notificationRemoveifUser);
        }
    }
}