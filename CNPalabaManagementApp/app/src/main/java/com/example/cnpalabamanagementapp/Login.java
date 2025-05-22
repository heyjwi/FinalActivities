package com.example.cnpalabamanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Login extends AppCompatActivity {
    EditText usernameInput, passwordInput;
    CardView loginButton;
    FirebaseFirestore db;
    TextView forgotPass;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Auto-login if session exists
        if (sessionManager.isLoggedIn()) {
            redirectBasedOnRole(sessionManager.getRole());
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        usernameInput = findViewById(R.id.login_user_input);
        passwordInput = findViewById(R.id.login_pass_input);
        loginButton = findViewById(R.id.cardView);
        forgotPass = findViewById(R.id.login_forgot_pass);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            authenticateUser(username, password);
        });

        setupWindowInsets();
    }

    private void authenticateUser(String username, String password) {
        db.collection("users")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        handleSuccessfulLogin(doc);
                    } else {
                        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSuccessfulLogin(DocumentSnapshot doc) {
        try {
            String userId = doc.getId();
            String username = doc.getString("username");
            String role = doc.getString("role");
            if (role == null) role = "user";

            String firstName = doc.getString("firstName");
            String lastName = doc.getString("lastName");
            String middleName = doc.getString("middleName");
            String fullName = (firstName != null ? firstName : "") +
                    (middleName != null ? " " + middleName : "") +
                    (lastName != null ? " " + lastName : "");

            String email = doc.getString("email");
            String contact = doc.getString("contactNumber");
            String branch = doc.getString("branch");
            String address = doc.getString("address");
            String profileImage = doc.getString("profileImageUrl");

            if ("admin".equalsIgnoreCase(role)) {
                // Admin gets all fields
                String birthDate = doc.getString("birthDate");
                String emergencyContact = doc.getString("emergencyContact");
                String emergencyContactNumber = doc.getString("emergencyContactNumber");
                String emergencyContactEmail = doc.getString("emergencyContactEmail");

                Log.d("LoginDebug", "Admin Data - BirthDate: " + birthDate +
                        ", EmergencyContact: " + emergencyContact +
                        ", EmergencyNumber: " + emergencyContactNumber +
                        ", EmergencyEmail: " + emergencyContactEmail);

                sessionManager.adminLogin(
                        username, role, userId, fullName, email, contact, branch,
                        address, profileImage, birthDate, emergencyContact,
                        emergencyContactNumber, emergencyContactEmail
                );
            } else {
                // Regular user gets basic fields
                sessionManager.login(
                        username, role, userId, fullName, email, contact,
                        branch, address, profileImage
                );
            }

            redirectBasedOnRole(role);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error processing user data", Toast.LENGTH_SHORT).show();
            Log.e("Login", "Error parsing user data", e);
        }
    }

    private void redirectBasedOnRole(String role) {
        Intent intent = "admin".equalsIgnoreCase(role)
                ? new Intent(this, MainActivityAdmin.class)
                : new Intent(this, MainActivityUser.class);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

