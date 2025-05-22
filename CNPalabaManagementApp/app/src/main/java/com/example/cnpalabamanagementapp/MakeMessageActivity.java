package com.example.cnpalabamanagementapp;

import android.app.ProgressDialog;
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

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MakeMessageActivity extends AppCompatActivity {

    private TextView senderTextView, recipientTextView;
    private EditText messageInput;
    private CardView sendButton;
    private String recipientUsername, recipientName, recipientUserId;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_make_message);

        sessionManager = new SessionManager(this);
        // Get intent extras
        recipientUsername = getIntent().getStringExtra("recipient_username");
        recipientName = getIntent().getStringExtra("recipient_name");

        initializeViews();
        setupViews();
        setupSendButton();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        senderTextView = findViewById(R.id.sender);
        recipientTextView = findViewById(R.id.sendTo);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.sendMsgBtn);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending message...");
        progressDialog.setCancelable(false);

        // Close button
        findViewById(R.id.imageView5).setOnClickListener(v -> finish());
    }

    private void setupViews() {
        // Set sender (current user)
        senderTextView.setText(sessionManager.getUsername());

        // Set recipient
        recipientTextView.setText(recipientName != null ? recipientName : recipientUsername);
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            // Only pass recipientUsername (no userId)
            sendMessageToStaff(recipientUsername, message);
        });
    }

    private void sendMessageToStaff(String recipientUsername, String message) {
        progressDialog.show();

        String currentUsername = sessionManager.getUsername();
        Date timestamp = new Date();

        // 1. Save Message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("fromUsername", currentUsername);
        messageData.put("toUsername", recipientUsername);
        messageData.put("message", message);
        messageData.put("timestamp", timestamp);
        messageData.put("isRead", false);

        FirebaseFirestore.getInstance()
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(msgRef -> {
                    // 2. Save Notification
                    Map<String, Object> notifData = new HashMap<>();
                    notifData.put("type", "MESSAGE");
                    notifData.put("fromUsername", currentUsername);
                    notifData.put("toUsername", recipientUsername);
                    notifData.put("content", message.substring(0, Math.min(50, message.length())));
                    notifData.put("timestamp", timestamp);
                    notifData.put("isRead", false);

                    FirebaseFirestore.getInstance()
                            .collection("notifications")
                            .add(notifData)
                            .addOnSuccessListener(notifRef -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "✓ Message sent", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}