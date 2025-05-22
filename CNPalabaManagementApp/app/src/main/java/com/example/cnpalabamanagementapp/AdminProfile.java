package com.example.cnpalabamanagementapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminProfile extends AppCompatActivity {

    // Views declarations
    private ImageView editAdminPfp;
    private EditText fullNameEditText, emailEditText, contactNumEditText, addressEditText,
            birthDateEditText, emergencyContactEditText, emergencyContactNumEditText,
            emergencyContactEmailEditText;
    private TextView usernameTextView;
    private ImageView profileImageView, backButton;
    private CardView logoutButton;
    private SessionManager sessionManager;

    private static final int PICK_IMAGE_REQUEST = 100;
    private Uri selectedImageUri;
    private static final String IMGBB_API_KEY = "ad511d5ece6e8fadd3b4c283eae26d6d";
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/*");
    private FrameLayout photoFrame;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    // Apply circular transformation immediately
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.profile_user)
                            .circleCrop()  // This is the key change
                            .error(R.drawable.profile_user)
                            .into(profileImageView);
                    uploadProfileImage();
                } else if (result.getResultCode() == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(this, ImagePicker.getError(result.getData()), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        // 1. Initialize SessionManager first
        sessionManager = new SessionManager(this);
        initializeViews();
        setUserData();

        // 4. Setup other components
        setupPermissionToggles();
        setupButtonListeners();

        editAdminPfp.setOnClickListener(v -> openImagePicker());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void openImagePicker() {
        ImagePicker.with(this)
                .crop()                    // Crop image
                .compress(1024)            // Final image size will be less than 1MB
                .maxResultSize(1080, 1080) // Final image resolution
                .createIntent(intent -> {
                    imagePickerLauncher.launch(intent);
                    return null;
                });
    }

    private void uploadProfileImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading profile image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "profile.jpg",
                            RequestBody.create(MEDIA_TYPE_IMAGE, imageBytes))
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(AdminProfile.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        String imageUrl = parseImageUrlFromResponse(responseBody);

                        if (imageUrl != null) {
                            // Update Firestore and session
                            updateProfileImage(imageUrl);
                        }
                    }
                    runOnUiThread(progressDialog::dismiss);
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String parseImageUrlFromResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject data = jsonObject.getJSONObject("data");
            return data.getString("url");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateProfileImage(String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = sessionManager.getUserId();

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firestore
        db.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    // Update session
                    sessionManager.updateProfileImage(imageUrl);

                    // Update UI
                    runOnUiThread(() -> {
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.profile_user)
                                .circleCrop()
                                .error(R.drawable.profile_user)
                                .into(profileImageView);

                        Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
                });
    }

    private void initializeViews() {
        // Initialize ALL EditText fields
        fullNameEditText = findViewById(R.id.full_name);
        emailEditText = findViewById(R.id.email);
        contactNumEditText = findViewById(R.id.contact_num);
        addressEditText = findViewById(R.id.address);
        birthDateEditText = findViewById(R.id.BirthDate);
        emergencyContactEditText = findViewById(R.id.EmergencyContact);
        emergencyContactNumEditText = findViewById(R.id.EmergencyContactNum);
        emergencyContactEmailEditText = findViewById(R.id.EmergencyContactEmail);
        editAdminPfp = findViewById(R.id.edit_profile_photo);

        // Initialize other views
        usernameTextView = findViewById(R.id.username);
        profileImageView = findViewById(R.id.profile_image);
        backButton = findViewById(R.id.backButton_adminProfile);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setUserData() {
        Log.d("ProfileDebug", "Retrieved - BirthDate: " + sessionManager.getBirthDate() +
                ", EmergencyContact: " + sessionManager.getEmergencyContact());
        // Add null checks for safety
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Safe way to set text - check if EditText exists first
        setTextSafely(fullNameEditText, sessionManager.getFullName());
        setTextSafely(emailEditText, sessionManager.getEmail());
        setTextSafely(contactNumEditText, sessionManager.getContact());
        setTextSafely(addressEditText, sessionManager.getAddress());
        setTextSafely(birthDateEditText, sessionManager.getBirthDate());
        setTextSafely(emergencyContactEditText, sessionManager.getEmergencyContact());
        setTextSafely(emergencyContactNumEditText, sessionManager.getEmergencyContactNumber());
        setTextSafely(emergencyContactEmailEditText, sessionManager.getEmergencyContactEmail());
        setTextSafely(usernameTextView, sessionManager.getUsername());

        // Load profile image
        String profileImageUrl = sessionManager.getProfileImage();
        if (profileImageView != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile_user)
                    .circleCrop()
                    .error(R.drawable.profile_user)
                    .into(profileImageView);
        }
    }

    private void setTextSafely(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "");
        }
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivityAdmin.class));
            overridePendingTransition(R.anim.stay_still, R.anim.slide_in_up);
        });

        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }


    private void setupPermissionToggles() {
        setupPermissionToggle(R.id.expand_employee_info, R.id.employee_info_desc);
        setupPermissionToggle(R.id.expand_camera, R.id.camera_desc);
        setupPermissionToggle(R.id.expand_storage, R.id.storage_desc);
        setupPermissionToggle(R.id.expand_internet, R.id.internet_desc);
        setupPermissionToggle(R.id.expand_location, R.id.location_desc);
        setupPermissionToggle(R.id.expand_notifications, R.id.notifications_desc);
    }

    private void setupPermissionToggle(int arrowViewId, int descViewId) {
        ImageView arrowView = findViewById(arrowViewId);
        TextView descView = findViewById(descViewId);

        View.OnClickListener toggleListener = v -> {
            if (descView.getVisibility() == View.VISIBLE) {
                descView.setVisibility(View.GONE);
                arrowView.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                descView.setVisibility(View.VISIBLE);
                arrowView.setImageResource(R.drawable.baseline_arrow_drop_up_24);
            }
        };

        arrowView.setOnClickListener(toggleListener);
        ((ViewGroup) arrowView.getParent().getParent()).setOnClickListener(toggleListener);
    }
}