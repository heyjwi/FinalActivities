package com.example.cnpalabamanagementapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private List<Announcement> announcements;

    public AnnouncementAdapter(List<Announcement> announcements) {
        this.announcements = announcements;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.announcement_item, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement announcement = announcements.get(position);

        holder.subject.setText(announcement.getSubject());
        holder.content.setText(announcement.getMessage());
        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(announcement.getDateCreated());
        holder.time.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    public static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView subject, content, sender, time;
        View indicator;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.notification_subject);
            content = itemView.findViewById(R.id.notification_content);
            time = itemView.findViewById(R.id.notification_time);
            indicator = itemView.findViewById(R.id.notification_indicator);
        }
    }
}