package com.example.cnpalabamanagementapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddStaff extends AppCompatActivity {


    private FirebaseFirestore db;
    private ImageView cancelAddBtn;
    private static final int PICK_IMAGE_REQUEST = 100;
    private Uri selectedImageUri;
    private ImageView photoImageView;
    private FrameLayout photoFrame;
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://example.com/default_profile.png";
    private static final String IMGBB_API_KEY = "ad511d5ece6e8fadd3b4c283eae26d6d";
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/*");

    // Modern way to handle activity results
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    photoImageView.setVisibility(View.VISIBLE);
                    photoImageView.setImageURI(selectedImageUri);
                } else if (result.getResultCode() == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(this, ImagePicker.getError(result.getData()), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_staff);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        setupViews();

        // Initialize views
        photoImageView = findViewById(R.id.photoImageView);
        photoFrame = findViewById(R.id.photoFrame);
        cancelAddBtn = findViewById(R.id.cancelAddNewStaff);

        // Setup spinner
        Spinner spinner = findViewById(R.id.spinner);
        String[] items = {"Select Branch", "CNSC Branch", "Magang Branch", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.branch_dropdown_item,
                items
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set click listeners
        photoFrame.setOnClickListener(v -> openImagePicker());
        cancelAddBtn.setOnClickListener(v -> finish());

        // Edge-to-edge handling
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

    private void setupViews() {
        // Set up your spinner with branch options
        Spinner branchSpinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.branch_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        branchSpinner.setAdapter(adapter);

        CardView addButton = findViewById(R.id.cardView);
        addButton.setOnClickListener(v -> saveStaffToFirestore());

        ImageView cancelButton = findViewById(R.id.cancelAddNewStaff);
        cancelButton.setOnClickListener(v -> finish());

        setupDatePicker(R.id.BirthDate);
        setupDatePicker(R.id.StartDate);
    }

    private void setupDatePicker(int editTextId) {
        EditText dateEditText = findViewById(editTextId);
        dateEditText.setOnClickListener(v -> showDatePickerDialog(editTextId));
    }

    private void showDatePickerDialog(int editTextId) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = (month1 + 1) + "/" + dayOfMonth + "/" + year1;
                    ((EditText) findViewById(editTextId)).setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveStaffToFirestore() {
        // Get all input values
        String firstName = ((EditText) findViewById(R.id.FirstName)).getText().toString().trim();
        String middleName = ((EditText) findViewById(R.id.MiddleName)).getText().toString().trim();
        String lastName = ((EditText) findViewById(R.id.LastName)).getText().toString().trim();
        String contactNumber = ((EditText) findViewById(R.id.ContactNumber)).getText().toString().trim();
        String email = ((EditText) findViewById(R.id.EmailAddress)).getText().toString().trim();
        String birthDate = ((EditText) findViewById(R.id.BirthDate)).getText().toString().trim();
        String startDate = ((EditText) findViewById(R.id.StartDate)).getText().toString().trim();
        String address = ((EditText) findViewById(R.id.Address)).getText().toString().trim();

        // Get gender selection
        String gender = "";
        RadioGroup genderGroup = findViewById(R.id.genderRadioGroup);
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.MaleRDB) {
            gender = "Male";
        } else if (selectedId == R.id.FemaleRDB) {
            gender = "Female";
        } else if (selectedId == R.id.OtherRDB) {
            gender = "Other";
        }

        // Get branch selection
        Spinner branchSpinner = findViewById(R.id.spinner);
        String branch = branchSpinner.getSelectedItem().toString();

        // Get emergency contact info
        String emergencyContact = ((EditText) findViewById(R.id.EmergencyContact)).getText().toString().trim();
        String emergencyContactNum = ((EditText) findViewById(R.id.EmergencyContactNum)).getText().toString().trim();
        String emergencyContactEmail = ((EditText) findViewById(R.id.EmergencyContactEmail)).getText().toString().trim();

        // Get role selection
        String role = "";
        RadioGroup roleGroup = findViewById(R.id.roleRadioGroup);
        int selectedRoleId = roleGroup.getCheckedRadioButtonId();
        if (selectedRoleId == R.id.AdminRDB) {
            role = "admin";
        } else if (selectedRoleId == R.id.StaffRDB) {
            role = "user";
        }

        // Get account credentials
        String username = ((EditText) findViewById(R.id.Username)).getText().toString().trim();
        String password = ((EditText) findViewById(R.id.Password)).getText().toString().trim();
        String confirmPassword = ((EditText) findViewById(R.id.ConfirmPassword)).getText().toString().trim();

        // Validate ALL fields first
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (role.isEmpty()) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create complete staff object
        Map<String, Object> staff = new HashMap<>();
        staff.put("firstName", firstName);
        staff.put("middleName", middleName);
        staff.put("lastName", lastName);
        staff.put("contactNumber", contactNumber);
        staff.put("email", email);
        staff.put("birthDate", birthDate);
        staff.put("startDate", startDate);
        staff.put("address", address);
        staff.put("gender", gender);
        staff.put("branch", branch);
        staff.put("emergencyContact", emergencyContact);
        staff.put("emergencyContactNumber", emergencyContactNum);
        staff.put("emergencyContactEmail", emergencyContactEmail);
        staff.put("role", role);
        staff.put("username", username);
        staff.put("password", password); // Store hashed password
        staff.put("createdAt", FieldValue.serverTimestamp());

        if (selectedImageUri != null) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Uploading image...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            uploadImageToImgBB(selectedImageUri, staff, progressDialog);
        } else {
            staff.put("profileImageUrl", DEFAULT_PROFILE_IMAGE_URL);
            saveStaffData(staff, null);
        }
    }

    private void uploadImageToImgBB(Uri imageUri, Map<String, Object> staff, ProgressDialog progressDialog) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
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
                        Toast.makeText(AddStaff.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        staff.put("profileImageUrl", DEFAULT_PROFILE_IMAGE_URL);
                        saveStaffData(staff, null);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        // Parse the response to get the image URL
                        // The response will be in JSON format with the URL in the "data.url" field
                        // You might want to use a JSON parser here for more robust parsing
                        String imageUrl = parseImageUrlFromResponse(responseBody);

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            staff.put("profileImageUrl", imageUrl != null ? imageUrl : DEFAULT_PROFILE_IMAGE_URL);
                            saveStaffData(staff, null);
                        });
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(AddStaff.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                            staff.put("profileImageUrl", DEFAULT_PROFILE_IMAGE_URL);
                            saveStaffData(staff, null);
                        });
                    }
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            staff.put("profileImageUrl", DEFAULT_PROFILE_IMAGE_URL);
            saveStaffData(staff, null);
        }
    }

    private String parseImageUrlFromResponse(String response) {
        try {
            // Simple parsing - for more robust parsing consider using JSONObject
            int urlIndex = response.indexOf("\"url\":\"") + 7;
            if (urlIndex > 7) {
                int endIndex = response.indexOf("\"", urlIndex);
                return response.substring(urlIndex, endIndex).replace("\\/", "/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveStaffData(Map<String, Object> staff, @Nullable ProgressDialog progressDialog) {
        db.collection("users")
                .whereEqualTo("username", staff.get("username").toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }

                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            db.collection("users")
                                    .add(staff)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(AddStaff.this, "Staff added successfully", Toast.LENGTH_SHORT).show();
                                        sendWelcomeEmail();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AddStaff.this, "Error adding staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(AddStaff.this, "Username already exists", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AddStaff.this, "Error checking username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendWelcomeEmail() {
        String recipientEmail = ((EditText) findViewById(R.id.EmailAddress)).getText().toString();
        String username = ((EditText) findViewById(R.id.Username)).getText().toString();
        String password = ((EditText) findViewById(R.id.Password)).getText().toString();
        String staffName = ((EditText) findViewById(R.id.FirstName)).getText().toString();

        String subject = "Welcome to Our Team!";
        String htmlBody = "<h1>Hello " + staffName + ", Welcome to CN Palaba!</h1>"
                + "<p>Your account details:</p>"
                + "<p><b>Username:</b> " + username + "</p>"
                + "<p><b>Password:</b> " + password + "</p>";

        EmailSender.sendEmail(recipientEmail, subject, htmlBody);
    }

}



