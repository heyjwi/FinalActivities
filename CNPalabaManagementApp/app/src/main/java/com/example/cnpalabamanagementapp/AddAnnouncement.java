package com.example.cnpalabamanagementapp;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;

public class AddAnnouncement extends AppCompatActivity {

    private ImageView closeAddAnnouncement;
    private TextView sender;
    private EditText subjectInput, messageInput;
    private CardView sendMsgBtn;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_announcement);

        // Initialize Firebase and Session
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize views
        closeAddAnnouncement = findViewById(R.id.closeAddAnnouncement);
        sender = findViewById(R.id.sender);
        subjectInput = findViewById(R.id.subjectInput);
        messageInput = findViewById(R.id.message_input);
        sendMsgBtn = findViewById(R.id.sendMsgBtn);

        // Set current user as sender
        String currentUser = sessionManager.getUsername();
        if (currentUser != null) {
            sender.setText(currentUser);
        }

        // Close button
        closeAddAnnouncement.setOnClickListener(v -> finish());

        // Send announcement button
        sendMsgBtn.setOnClickListener(v -> sendAnnouncement());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void sendAnnouncement() {
        String subject = subjectInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();
        String senderName = sender.getText().toString();

        // Validate inputs
        if (subject.isEmpty()) {
            subjectInput.setError("Subject is required");
            return;
        }

        if (message.isEmpty()) {
            messageInput.setError("Message is required");
            return;
        }

        // Create announcement object
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("subject", subject);
        announcement.put("message", message);
        announcement.put("sender", senderName);
        announcement.put("dateCreated", new Date()); // Current timestamp
        announcement.put("recipients", "All Users"); // Or specify recipients

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Sending...");
        progress.show();

        // Add to Firestore
        db.collection("announcements")
                .add(announcement)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Announcement sent successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity after success
                    progress.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error sending announcement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                });
    }
}