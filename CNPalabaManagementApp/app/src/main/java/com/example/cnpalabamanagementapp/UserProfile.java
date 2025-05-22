package com.example.cnpalabamanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

public class UserProfile extends AppCompatActivity {

    CardView logoutButton;
    ImageView backButton;
    private SessionManager session;
    private EmployeeViewModel viewModel;
    private ImageView profileImage;
    private TextView profileRole;
    private EditText fullName, email, contactNum, branch, address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);


        backButton = findViewById(R.id.backButton_adminProfile);
        logoutButton = findViewById(R.id.logoutButton);
        profileImage = findViewById(R.id.profile_image);
        profileRole = findViewById(R.id.profile_role);
        fullName = findViewById(R.id.full_name);
        email = findViewById(R.id.email);
        contactNum = findViewById(R.id.contact_num);
        branch = findViewById(R.id.branch);
        address = findViewById(R.id.address);

        session = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);

        loadProfileData();
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserProfile.this, MainActivityUser.class);
                startActivity(intent);
                overridePendingTransition(R.anim.stay_still, R.anim.slide_in_up);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionManager sessionManager = new SessionManager(UserProfile.this);
                sessionManager.logout();

                Intent intent = new Intent(UserProfile.this, Login.class);
                startActivity(intent);
                finish(); // Close current activity
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadProfileData() {
        // Basic fields that all users have
        profileRole.setText(session.getUsername());
        fullName.setText(session.getFullName());
        email.setText(session.getEmail());
        contactNum.setText(session.getContact());
        branch.setText(session.getBranch());
        address.setText(session.getAddress());

        // Load profile image
        Glide.with(this)
                .load(session.getProfileImage())
                .placeholder(R.drawable.profile_user)
                .circleCrop()
                .into(profileImage);

        // Optional: Fetch fresh data from Firestore
        String userId = session.getUserId();
        if (userId != null) {
            viewModel.getEmployeeById(userId).observe(this, employee -> {
                if (employee != null) {
                    // Update only the basic fields
                    fullName.setText(employee.getFullName());
                    email.setText(employee.getEmail());
                    contactNum.setText(employee.getContactNumber());
                    branch.setText(employee.getBranch());
                    address.setText(employee.getAddress());

                    // Update session with basic fields only
                    session.login(
                            session.getUsername(),
                            session.getRole(),
                            session.getUserId(),
                            employee.getFullName(),
                            employee.getEmail(),
                            employee.getContactNumber(),
                            employee.getBranch(),
                            employee.getAddress(),
                            employee.getProfileImageUrl()
                    );
                }
            });
        }
    }
    }