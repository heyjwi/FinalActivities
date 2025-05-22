package com.example.cnpalabamanagementapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ViewStaffDetails extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView usernameTextView;
    private EditText fullNameEditText, emailEditText, contactNumEditText,
            addressEditText, birthDateEditText, emergencyContactEditText,
            emergencyContactNumEditText, emergencyContactEmailEditText, gender, createdAt, workStart;
    private Employee employee;
    private CardView deleteStaff;

    private ImageView editProfilePhoto;
    private static final int PICK_IMAGE_REQUEST = 100;
    private Uri selectedImageUri;
    private static final String IMGBB_API_KEY = "ad511d5ece6e8fadd3b4c283eae26d6d"; // Use your actual API key
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/*");


    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    // Apply circular transformation immediately
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.profile_user)
                            .circleCrop()
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
        Log.d("FirestoreUpdate", "TEST - Logging is working");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_staff_details);

        initializeViews();
        setupButtonListeners();
        // Get the employee data from intent
        employee = getIntent().getParcelableExtra("employee");

        if (employee != null) {
            displayEmployeeData();
        } else {
            Toast.makeText(this, "Employee data not available", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profile_image);
        usernameTextView = findViewById(R.id.username);
        fullNameEditText = findViewById(R.id.full_name);
        emailEditText = findViewById(R.id.email);
        contactNumEditText = findViewById(R.id.contactNum);
        addressEditText = findViewById(R.id.address);
        birthDateEditText = findViewById(R.id.BirthDate);
        emergencyContactEditText = findViewById(R.id.EmergencyContact);
        emergencyContactNumEditText = findViewById(R.id.EmergencyContactNum);
        emergencyContactEmailEditText = findViewById(R.id.EmergencyContactEmail);
        gender= findViewById(R.id.gender);
        createdAt = findViewById(R.id.dateCreated);
        workStart = findViewById(R.id.work_start);
        editProfilePhoto = findViewById(R.id.edit_profile_photo);
        editProfilePhoto.setOnClickListener(v -> openImagePicker());
        deleteStaff = findViewById(R.id.deleteStaffBtn);
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
        Log.d("FirestoreUpdate", "1. uploadProfileImage() called");
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
                    Log.e("FirestoreUpdate", "2a. ImgBB upload failed", e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(ViewStaffDetails.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("FirestoreUpdate", "2b. ImgBB response code: " + response.code());
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d("FirestoreUpdate", "2c. ImgBB response: " + responseBody);
                        String imageUrl = parseImageUrlFromResponse(responseBody);
                        Log.d("FirestoreUpdate", "2d. Parsed URL: " + imageUrl);

                        if (imageUrl != null) {
                            updateEmployeeProfileImage(imageUrl);
                        } else {
                            Log.e("FirestoreUpdate", "2e. Failed to parse image URL");
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

    private void updateEmployeeProfileImage(String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("username", employee.getUsername())
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);

                        document.getReference().update("profileImageUrl", imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    // 1. Update local employee object
                                    employee.setProfileImageUrl(imageUrl);

                                    // 2. Get the ViewModel and update its data
                                    EmployeeViewModel viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);
                                    viewModel.updateEmployeeProfile(employee.getUsername(), imageUrl);

                                    // 3. Update UI
                                    runOnUiThread(() -> {
                                        Glide.with(this).load(imageUrl).into(profileImageView);
                                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    // Error handling
                                });
                    }
                });
    }

    private void displayEmployeeData() {
        // Set profile image
        if (employee.getProfileImageUrl() != null && !employee.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(employee.getProfileImageUrl())
                    .placeholder(R.drawable.profile_user)
                    .error(R.drawable.profile_user)
                    .circleCrop()
                    .into(profileImageView);
        }

        // Set other fields
        usernameTextView.setText(employee.getUsername());
        fullNameEditText.setText(employee.getFullName());
        emailEditText.setText(employee.getEmail());
        contactNumEditText.setText(employee.getContactNumber());
        addressEditText.setText(employee.getAddress());
        birthDateEditText.setText(employee.getBirthDate());
        emergencyContactEditText.setText(employee.getEmergencyContact());
        emergencyContactNumEditText.setText(employee.getEmergencyContactNumber());
        emergencyContactEmailEditText.setText(employee.getEmergencyContactEmail());
        gender.setText(employee.getGender());
        workStart.setText(employee.getStartDate());
        createdAt.setText(formatFirestoreTimestamp(employee.getCreatedAt()));


    }

    private void setupButtonListeners() {
        findViewById(R.id.msgStaffBtn).setOnClickListener(v -> {
            Intent intent = new Intent(ViewStaffDetails.this, MakeMessageActivity.class);
            intent.putExtra("recipient_username", employee.getUsername());
            intent.putExtra("recipient_name", employee.getFullName());
            startActivity(intent);
        });

        findViewById(R.id.assignStaffTaskBtn).setOnClickListener(v -> {
            // Handle assign task button click
        });

        deleteStaff.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Staff")
                .setMessage("Are you sure you want to delete this staff member?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteEmployee();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEmployee() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting staff...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        EmployeeViewModel viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);
        viewModel.deleteEmployee(employee.getUsername(), new EmployeeViewModel.OnDeleteListener() {
            @Override
            public void onSuccess() {
                progressDialog.dismiss();
                Toast.makeText(ViewStaffDetails.this, "Staff deleted successfully", Toast.LENGTH_SHORT).show();
                finish(); // Close this activity and return to list
            }

            @Override
            public void onFailure(String error) {
                progressDialog.dismiss();
                Toast.makeText(ViewStaffDetails.this, "Failed to delete: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatFirestoreTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Date not available";
        }

        try {
            // Convert Firestore timestamp to Date
            Date date = timestamp.toDate();

            // Create formatter with desired pattern
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            // Format the date
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid date";
        }
    }

}