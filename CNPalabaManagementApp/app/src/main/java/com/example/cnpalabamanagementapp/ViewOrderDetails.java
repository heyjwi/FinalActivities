package com.example.cnpalabamanagementapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
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

public class ViewOrderDetails extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final String IMGBB_API_KEY = "ad511d5ece6e8fadd3b4c283eae26d6d";
    private static final String DEFAULT_DOCUMENTATION_URL = "https://example.com/default_image.jpg";

    private Uri imageUri;
    private ImageView documentationImg;
    private TextView dateCompletedTextView;
    private LinearLayout linearLayoutCompleted, completedOrderLayout;
    private TextView documentationTxt;
    private FrameLayout documentationFrame;
    private ViewGroup statusRadioGroup;
    private TextView statusText;
    private FrameLayout headerFrameLayout;

    private ProgressBar progressBar;
    private ScrollView contentView;
    private ImageView closeOrderDetailsBtn;
    private CardView markOrderCompleteBtn;
    private Order currentOrder; // Add this to store the current order


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (documentationImg != null) {
                documentationImg.setImageURI(imageUri);
                uploadImageToImgBB();
            } else {
                Toast.makeText(this, "Failed to display image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_order_details);

        linearLayoutCompleted =findViewById(R.id.linearLayoutCompleted);
        completedOrderLayout = findViewById(R.id.completedOrderLayout);
        documentationTxt = findViewById(R.id.documentationTxt);
        documentationFrame = findViewById(R.id.documentationFrame);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        documentationImg = findViewById(R.id.documentationImg);
        dateCompletedTextView = findViewById(R.id.orderCompleted);
        statusText = findViewById(R.id.statusText);
        headerFrameLayout = findViewById(R.id.headerFrameLayout);

        String orderId = getIntent().getStringExtra("order_id");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        TextView received = findViewById(R.id.received);
        TextView washingDrying = findViewById(R.id.washing);
        TextView folding = findViewById(R.id.folding);
        TextView pickup = findViewById(R.id.pickup);

        View.OnClickListener statusClickListener = v -> {
            // Reset all views
            received.setSelected(false);
            washingDrying.setSelected(false);
            folding.setSelected(false);
            pickup.setSelected(false);

            // Set the clicked view as selected
            v.setSelected(true);

            // Update status in Firestore
            String newStatus;
            if (v == received) {
                newStatus = "Received";
            } else if (v == washingDrying) {
                newStatus = "Washing/Drying";
            } else if (v == folding) {
                newStatus = "Folding";
            } else {
                newStatus = "Pickup";
            }

            if (orderId != null) {
                db.collection("orders").document(orderId)
                        .update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            // Update the currentOrder's status immediately
                            if (currentOrder != null) {
                                currentOrder.setStatus(newStatus);
                                updateCompleteButtonState(newStatus);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                            // Revert the UI if update fails
                            if (currentOrder != null) {
                                String currentStatus = currentOrder.getStatus();
                                updateStatusUI(currentStatus);
                            }
                        });
            }
        };

        received.setOnClickListener(statusClickListener);
        washingDrying.setOnClickListener(statusClickListener);
        folding.setOnClickListener(statusClickListener);
        pickup.setOnClickListener(statusClickListener);

        progressBar = findViewById(R.id.progressBar);
        contentView = findViewById(R.id.contentView);
        markOrderCompleteBtn = findViewById(R.id.markAsCompleteBtn);
        closeOrderDetailsBtn = findViewById(R.id.closeOrderDetails);
        closeOrderDetailsBtn.setOnClickListener(v -> finish());

        markOrderCompleteBtn.setOnClickListener(v -> {
            if (currentOrder != null && "Pickup".equalsIgnoreCase(currentOrder.getStatus())) {
                markOrderAsComplete();
            } else {
                Toast.makeText(this, "Order must be in 'Pickup' status to mark as complete", Toast.LENGTH_SHORT).show();
            }
        });

        if (orderId != null) {
            loadOrderDetails(orderId);
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateStatusUI(String status) {
        TextView received = findViewById(R.id.received);
        TextView washingDrying = findViewById(R.id.washing);
        TextView folding = findViewById(R.id.folding);
        TextView pickup = findViewById(R.id.pickup);

        received.setSelected(false);
        washingDrying.setSelected(false);
        folding.setSelected(false);
        pickup.setSelected(false);

        if (status != null) {
            switch (status.toLowerCase()) {
                case "received":
                    received.setSelected(true);
                    break;
                case "washing/drying":
                    washingDrying.setSelected(true);
                    break;
                case "folding":
                    folding.setSelected(true);
                    break;
                case "pickup":
                    pickup.setSelected(true);
                    break;
                default:
                    received.setSelected(true);
            }
        }
        updateCompleteButtonState(status);
    }

    private void displayOrderDetails(Order order) {
        // Implement your UI updates here
        // Example:
        currentOrder = order;
        TextView orderNumber = findViewById(R.id.orderNumber);
        TextView orderReceived = findViewById(R.id.orderReceived);
        TextView customerName = findViewById(R.id.customerName);
        TextView orderHandler = findViewById(R.id.orderHandler);
        TextView serviceType = findViewById(R.id.serviceType);
        TextView loadType = findViewById(R.id.loadType);
        TextView addOns = findViewById(R.id.addOns);
        TextView total = findViewById(R.id.orderTotal);
        TextView customerNote = findViewById(R.id.customerNote);

        orderNumber.setText("Order No. " + order.getOrderNumber());
        orderReceived.setText(order.getFormattedDate());
        customerName.setText(order.getCustomerName());
        orderHandler.setText(order.getHandlerName());
        serviceType.setText(order.getServiceType());
        loadType.setText(order.getLoadType());

        int ArielQuantity = order.getArielQuantity();
        int DownyQuantity = order.getDownyQuantity();

        if (ArielQuantity == 0 && DownyQuantity == 0) {
            addOns.setText("None");
        } else if (ArielQuantity != 0 && DownyQuantity == 0) {
            addOns.setText("Ariel " + ArielQuantity + "x");
        } else if (ArielQuantity == 0 && DownyQuantity != 0) {
            addOns.setText("Downy " + DownyQuantity + "x");
        } else {
            addOns.setText("Ariel " + ArielQuantity + "x, Downy " + DownyQuantity);
        }

        total.setText("₱" + order.getTotal());

        if (order.getOrderNote() == null || order.getOrderNote().trim().isEmpty()) {
            customerNote.setText("No special instructions provided by customer");
        } else {
            customerNote.setText(order.getOrderNote());  // Use getOrderNote() instead
        }


        progressBar.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        markOrderCompleteBtn.setVisibility(View.VISIBLE);

        TextView received = findViewById(R.id.received);
        TextView washingDrying = findViewById(R.id.washing);
        TextView folding = findViewById(R.id.folding);
        TextView pickup = findViewById(R.id.pickup);

        updateCompleteButtonState(order.getStatus());


        String status = order.getStatus();
        if (status != null) {
            switch (status.toLowerCase()) {
                case "received":
                    received.setSelected(true);
                    break;
                case "washing/drying":
                    washingDrying.setSelected(true);
                    break;
                case "folding":
                    folding.setSelected(true);
                    break;
                case "pickup":
                    pickup.setSelected(true);
                    break;
                default:
                    received.setSelected(true); // Default to "Received"
            }
        }

        if ("Completed".equalsIgnoreCase(order.getStatus())) {
            String docUrl = order.getDocumentationUrl() != null ? order.getDocumentationUrl() : "";
            updateUIForCompletedOrder(docUrl);
            dateCompletedTextView.setText(order.getFormattedCompletedDate());
        } else {
            // Ensure regular UI is shown for non-completed orders
            linearLayoutCompleted.setVisibility(View.GONE);
            completedOrderLayout.setVisibility(View.GONE);
            documentationTxt.setVisibility(View.GONE);
            documentationFrame.setVisibility(View.GONE);
            statusRadioGroup.setVisibility(View.VISIBLE);
            if (statusText != null) statusText.setVisibility(View.VISIBLE);
        }

    }

    private void loadOrderDetails(String orderId) {
        Log.d("LoadOrder", "Loading order ID: " + orderId); // Add this
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("LoadOrder", "Document exists"); // Add this
                        Order order = documentSnapshot.toObject(Order.class);
                        if (order != null) {
                            Log.d("LoadOrder", "Order loaded: " + order.getOrderNumber()); // Add this
                            order.setDocumentId(documentSnapshot.getId());
                            displayOrderDetails(order);
                        } else {
                            Log.e("LoadOrder", "Order object is null"); // Add this
                        }
                    } else {
                        Log.e("LoadOrder", "Document doesn't exist"); // Add this
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoadOrder", "Error: " + e.getMessage()); // Add this
                    Toast.makeText(this, "Error loading order details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateCompleteButtonState(String status) {
        if ("Pickup".equalsIgnoreCase(status)) {
            markOrderCompleteBtn.setClickable(true);
            markOrderCompleteBtn.setAlpha(1f);
        } else {
            markOrderCompleteBtn.setClickable(false);
            markOrderCompleteBtn.setAlpha(0.5f);
        }
    }

    private void markOrderAsComplete() {
        if (currentOrder == null || currentOrder.getDocumentId() == null) return;

        // Disable button while processing
        markOrderCompleteBtn.setClickable(false);
        markOrderCompleteBtn.setAlpha(0.5f);

        new AlertDialog.Builder(this)
                .setTitle("Documentation Required")
                .setMessage("Please take a photo of the completed order for documentation")
                .setPositiveButton("Take Photo", (dialog, which) -> {
                    if (checkCameraPermission()) {
                        openCamera();
                    } else {
                        requestCameraPermission();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                STORAGE_PERMISSION_CODE);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Order_" + currentOrder.getOrderNumber());
        values.put(MediaStore.Images.Media.DESCRIPTION, "Completed order documentation");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
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

    private void updateUIForCompletedOrder(String imageUrl) {
        // Show the completed order UI elements
        if (headerFrameLayout != null) {
            ViewGroup.LayoutParams params = headerFrameLayout.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    240,
                    getResources().getDisplayMetrics()
            );
            headerFrameLayout.setLayoutParams(params);
        }
        if (linearLayoutCompleted != null) linearLayoutCompleted.setVisibility(View.VISIBLE);
        if (linearLayoutCompleted != null) linearLayoutCompleted.setVisibility(View.VISIBLE);
        if (completedOrderLayout != null) completedOrderLayout.setVisibility(View.VISIBLE);
        if (documentationTxt != null) documentationTxt.setVisibility(View.VISIBLE);
        if (documentationFrame != null) documentationFrame.setVisibility(View.VISIBLE);

        // Set the completed date
        if (dateCompletedTextView != null) {
            dateCompletedTextView.setText(
                    new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date())
            );
        }

        // Hide status selection UI
        if (statusRadioGroup != null) statusRadioGroup.setVisibility(View.GONE);
        if (statusText != null) statusText.setVisibility(View.GONE);
        if (markOrderCompleteBtn != null) markOrderCompleteBtn.setVisibility(View.GONE);

        // Load the image if available
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals(DEFAULT_DOCUMENTATION_URL)) {
            if (documentationImg != null) {
                // Make sure the ImageView is visible
                documentationImg.setVisibility(View.VISIBLE);

                // Use Glide to load the image
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.documentation_round_frame)  // Optional placeholder
                        .into(documentationImg);
            } else {
                Log.e("ImageViewError", "documentationImg is null");
            }
        } else {
            Log.d("ImageURL", "No valid image URL provided");
        }
    }

    private void updateOrderAsComplete(String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Completed");
        updates.put("completedDate", new Date());
        updates.put("documentationUrl", imageUrl);

        db.collection("orders").document(currentOrder.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update UI for completed order
                    updateUIForCompletedOrder(imageUrl);
                    Toast.makeText(this, "Order marked as complete!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to mark order as complete", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageToImgBB() {
        if (imageUri == null || currentOrder == null) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading documentation...");
        progressDialog.setCancelable(false);
        progressDialog.show();

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
                    .addFormDataPart("image", "order_" + currentOrder.getOrderNumber() + ".jpg",
                            RequestBody.create(MediaType.parse("image/*"), imageBytes))
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
                        Toast.makeText(ViewOrderDetails.this, "Documentation upload failed", Toast.LENGTH_SHORT).show();
                        updateOrderAsComplete(DEFAULT_DOCUMENTATION_URL);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        String imageUrl = parseImageUrlFromResponse(responseBody);

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            if (imageUrl != null) {
                                updateOrderAsComplete(imageUrl);
                            } else {
                                updateOrderAsComplete(DEFAULT_DOCUMENTATION_URL);
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(ViewOrderDetails.this, "Documentation upload failed", Toast.LENGTH_SHORT).show();
                            updateOrderAsComplete(DEFAULT_DOCUMENTATION_URL);
                        });
                    }
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateOrderAsComplete(DEFAULT_DOCUMENTATION_URL);
        }
    }


}