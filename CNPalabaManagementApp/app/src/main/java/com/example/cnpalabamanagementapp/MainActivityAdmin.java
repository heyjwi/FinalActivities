package com.example.cnpalabamanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivityAdmin extends AppCompatActivity {
    private BottomNavigationView bottomNav;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);
        bottomNav = findViewById(R.id.bottom_nav);

        initializeCounter();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            if (item.getItemId() == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragmentAdmin();
            } else if (item.getItemId() == R.id.nav_orders) {
                selectedFragment = new OrdersFragment();
            } else if (item.getItemId() == R.id.nav_employees) {
                selectedFragment = new EmployeesFragment();
            } else if (item.getItemId() == R.id.nav_tasks) {
                selectedFragment = new TasksFragment();
            } else if (item.getItemId() == R.id.nav_reports) {
                selectedFragment = new ReportsFragmentAdmin();
            } else {
                return false;
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit();

            return true;
        });

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new DashboardFragmentAdmin())
                    .commit();
        }

        // Optional: For edge-to-edge support
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void navigateToTab(int menuItemId) {
        bottomNav.setSelectedItemId(menuItemId);
    }

    public void initializeCounter() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if counter exists
        db.collection("counters").document("orders")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Initialize with 0 if it doesn't exist
                        Map<String, Object> counter = new HashMap<>();
                        counter.put("count", 0);

                        db.collection("counters").document("orders")
                                .set(counter)
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Counter initialized"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error initializing counter", e));
                    }
                });
    }
}