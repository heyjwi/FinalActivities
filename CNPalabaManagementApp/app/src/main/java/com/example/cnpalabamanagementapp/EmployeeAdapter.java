package com.example.cnpalabamanagementapp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {
    private List<Employee> employeeList;
    private Context context;
    private OnEmployeeClickListener listener;
    private List<Employee> originalList;


    public EmployeeAdapter(List<Employee> employeeList, Context context, OnEmployeeClickListener listener) {
        this.employeeList = new ArrayList<>(employeeList);
        this.originalList = new ArrayList<>(employeeList);
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false); // Changed to item_employee
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        int firstPos = position * 2;
        int secondPos = firstPos + 1;

        if (firstPos < employeeList.size()) {
            holder.bindEmployee(employeeList.get(firstPos), true);
        } else {
            holder.hideFirstCard();
        }

        if (secondPos < employeeList.size()) {
            holder.bindEmployee(employeeList.get(secondPos), false);
        } else {
            holder.hideSecondCard();
        }
    }

    @Override
    public int getItemCount() {
        return (employeeList.size() + 1) / 2;
    }

    public void updateList(List<Employee> employees) {
        this.employeeList = new ArrayList<>(employees);
        this.originalList = new ArrayList<>(employees); // Update both lists
        notifyDataSetChanged();
    }


    class EmployeeViewHolder extends RecyclerView.ViewHolder {
        CardView card1, card2;
        TextView tvName1, tvName2;
        ImageView image1, image2;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            card1 = itemView.findViewById(R.id.cardEmployee1);
            card2 = itemView.findViewById(R.id.cardEmployee2);
            tvName1 = itemView.findViewById(R.id.tvEmployeeName1);
            tvName2 = itemView.findViewById(R.id.tvEmployeeName2);
            image1 = itemView.findViewById(R.id.imageEmployee1);
            image2 = itemView.findViewById(R.id.imageEmployee2);

            // Verify views exist
            if (card1 == null || card2 == null || image1 == null || image2 == null) {
                throw new IllegalStateException("Missing required views in item_employee.xml");
            }

            // Set click listeners
            card1.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onEmployeeClick(employeeList.get(getAdapterPosition() * 2));
                }
            });

            card2.setOnClickListener(v -> {
                int secondPos = getAdapterPosition() * 2 + 1;
                if (secondPos < employeeList.size()) {
                    listener.onEmployeeClick(employeeList.get(secondPos));
                }
            });

        }

        public void bindEmployee(Employee employee, boolean isFirstCard) {
            if (isFirstCard) {
                card1.setVisibility(View.VISIBLE);
                tvName1.setText(employee.getFullName());
                loadImage(employee.getProfileImageUrl(), image1);
                card1.setOnClickListener(v -> listener.onEmployeeClick(employee)); // Add this
            } else {
                card2.setVisibility(View.VISIBLE);
                tvName2.setText(employee.getFullName());
                loadImage(employee.getProfileImageUrl(), image2);
                card2.setOnClickListener(v -> listener.onEmployeeClick(employee)); // Add this
            }
        }

        public void hideFirstCard() {
            card1.setVisibility(View.INVISIBLE);
        }

        public void hideSecondCard() {
            card2.setVisibility(View.INVISIBLE);
        }

        private void loadImage(String url, ImageView imageView) {
            // 1. First check for null or empty URL
            if (url == null || url.isEmpty()) {
                Log.e("IMAGE_LOAD", "Empty image URL");
                Glide.with(context)
                        .load(R.drawable.default_profile2)
                        .into(imageView);
                return;
            }

            // 2. NEW: Check for valid HTTP/HTTPS protocol
            if (!url.startsWith("http")) {
                Log.e("IMAGE_URL", "Invalid URL format (missing http/https): " + url);
                Glide.with(context)
                        .load(R.drawable.default_profile2)
                        .into(imageView);
                return;
            }

            Log.d("IMAGE_LOAD", "Attempting to load: " + url);

            // 3. Original Glide loading code
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.default_profile2)
                    .error(R.drawable.default_profile2)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e("IMAGE_LOAD", "Load failed for URL: " + url, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d("IMAGE_LOAD", "Image loaded successfully: " + url);
                            return false;
                        }
                    })
                    .into(imageView);
        }
    }

    public List<Employee> getEmployeeList() {
        return new ArrayList<>(employeeList); // Return a copy to avoid direct mutation
    }

    public interface OnEmployeeClickListener {
        void onEmployeeClick(Employee employee);
    }

    public void filter(String query) {
        query = query.toLowerCase().trim();
        employeeList.clear();

        if (query.isEmpty()) {
            employeeList.addAll(originalList);
        } else {
            for (Employee employee : originalList) {
                if (employee.getFullName().toLowerCase().contains(query)) {
                    employeeList.add(employee);
                }
            }
        }
        notifyDataSetChanged();
    }
}