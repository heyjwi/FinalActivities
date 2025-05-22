package com.example.cnpalabamanagementapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddOrder extends AppCompatActivity {

    // UI References

    // Pricing constants
    private static final int FULL_SERVICE_PRICE = 150;
    private static final int SUPERWASH_PRICE = 60;
    private static final int DRY_PRICE = 60;
    private static final int COMBO_PRICE = 100; // Superwash + Dry
    private static final int FOLD_PRICE = 50;
    private static final int ARIEL_PRICE = 25;
    private static final int DOWNY_PRICE = 15;

    private LinearLayout fullServiceSelector, selfServiceSelector;
    private View fullServiceUnderline, selfServiceUnderline;
    private ImageView closeAddOrder;
    private CheckBox superwashCheckBox, dryCheckBox, foldCheckBox;
    private TextView arielQuantity, downyQuantity, orderTotalPrice;
    private RadioGroup loadTypeRadioGroup;
    private RadioButton regularLoadType, heavyItemsLoadType, smallComforterLoadType, largeComforterLoadType;
    private CardView addOrderBtn;
    private EditText customerNameEditText, contactNumberEditText, emailEditText, customerNote;

    // State Variables
    private int arielQty = 0;
    private int downyQty = 0;
    private boolean isFullServiceSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_order);

        initializeViews();

        // First reset all selections
        resetAllSelections();

        // Then handle the intent extras to set the correct selection
        handleIntentExtras();

        // If no service type was specified in intent, default to none selected
        if (getIntent().getExtras() == null || !getIntent().getExtras().containsKey("SERVICE_TYPE")) {
            resetServiceSelection();
        }


        // Setup the rest of the UI
        setupServiceSelection();
        setupQuantityControls();
        setupRadioGroup();
        setupCheckboxListeners();
        setupCloseButton();
        setupAddOrderButton();
        applyWindowInsets();
        updateTotalDisplay();
    }

    private void resetServiceSelection() {
        isFullServiceSelected = false;
        fullServiceUnderline.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        selfServiceUnderline.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        setServicesEnabled(true);
        setAddonsEnabled(true);
        updateLoadTypeEnabledState();
        updateTotalDisplay();
    }


    private void handleIntentExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String serviceType = extras.getString("SERVICE_TYPE", "");
            if ("FULL".equals(serviceType)) {
                selectFullService();
            } else if ("SELF".equals(serviceType)) {
                selectSelfService();
            }
        }
    }

    private void setupAddOrderButton() {
        addOrderBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                submitOrder();
            }
        });
    }

    private boolean validateInputs() {
        // 1. First check if service type is selected
        if (!isFullServiceSelected && fullServiceUnderline.getBackground() == null
                && !isFullServiceSelected && selfServiceUnderline.getBackground() == null) {
            showError("Please select service type");
            return false;
        }

        // 2. For self-service, check at least one service is selected
        if (!isFullServiceSelected) {
            if (!superwashCheckBox.isChecked() && !dryCheckBox.isChecked() && !foldCheckBox.isChecked()) {
                showError("Please select at least one service");
                return false;
            }
        }

        // 3. Check load type is selected (for both service types)
        if (isFullServiceSelected || superwashCheckBox.isChecked()) {
            if (getSelectedLoadType().isEmpty()) {
                showError("Please select load type");
                return false;
            }
        }

        // 4. Validate customer information
        String customerName = customerNameEditText.getText().toString().trim();
        if (customerName.isEmpty()) {
            showError("Please enter customer name");
            customerNameEditText.requestFocus();
            return false;
        }

        String contactNumber = contactNumberEditText.getText().toString().trim();
        if (contactNumber.isEmpty()) {
            showError("Please enter contact number");
            contactNumberEditText.requestFocus();
            return false;
        }

        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            showError("Please enter email address");
            emailEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email");
            emailEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void submitOrder() {
        saveOrderToFirestore();
    }

    private void saveOrderToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SessionManager sessionManager = new SessionManager(this);
        int total = calculateTotal();

        // First get the current counter
        db.collection("counters").document("orders")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    int nextOrderNumber;

                    if (documentSnapshot.exists() && documentSnapshot.contains("count")) {
                        nextOrderNumber = documentSnapshot.getLong("count").intValue() + 1;
                    } else {
                        nextOrderNumber = 1;
                    }

                    // Create the order with all fields
                    Map<String, Object> order = new HashMap<>();
                    order.put("orderNumber", nextOrderNumber);
                    order.put("customerName", customerNameEditText.getText().toString().trim());
                    order.put("contactNumber", contactNumberEditText.getText().toString().trim());
                    order.put("email", emailEditText.getText().toString().trim());
                    order.put("serviceType", isFullServiceSelected ? "Full Service" : "Self Service");
                    order.put("selectedServices", getSelectedServices());
                    order.put("loadType", getSelectedLoadType());
                    order.put("arielQuantity", arielQty);
                    order.put("downyQuantity", downyQty);
                    order.put("timestamp", FieldValue.serverTimestamp());
                    order.put("status", "Received");
                    order.put("orderNote", customerNote.getText().toString().trim());
                    order.put("total", total);
                    order.put("handlerUserId", sessionManager.getUserId());
                    order.put("handlerName", sessionManager.getUsername());
                    order.put("handlerBranch", sessionManager.getBranch());

                    // Add the order to Firestore
                    db.collection("orders")
                            .add(order)
                            .addOnSuccessListener(documentReference -> {
                                // Update the counter (using set() to create or overwrite)
                                Map<String, Object> counterData = new HashMap<>();
                                counterData.put("count", nextOrderNumber);

                                db.collection("counters").document("orders")
                                        .set(counterData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Firestore", "Counter updated to: " + nextOrderNumber);
                                            showSuccess("Order #" + nextOrderNumber + " added successfully! Total: ₱" + total);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore", "Error updating counter", e);
                                            showError("Order saved but counter update failed");
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error adding order", e);
                                showError("Failed to add order");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting counter", e);
                    showError("Failed to get order number");
                });
    }

    private List<String> getSelectedServices() {
        List<String> selectedServices = new ArrayList<>();
        if (superwashCheckBox.isChecked()) selectedServices.add("Superwash");
        if (dryCheckBox.isChecked()) selectedServices.add("Dry");
        if (foldCheckBox.isChecked()) selectedServices.add("Fold");
        return selectedServices;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void initializeViews() {
        orderTotalPrice = findViewById(R.id.orderTotalPrice);
        customerNameEditText = findViewById(R.id.CustomerName); // replace with your actual ID
        contactNumberEditText = findViewById(R.id.CustomerContactNumber);
        customerNote = findViewById(R.id.OrderNote);
        emailEditText = findViewById(R.id.CustomerEmail); // replace with your actual ID
        addOrderBtn = findViewById(R.id.cardView);
        closeAddOrder = findViewById(R.id.closeOrder);
        fullServiceSelector = findViewById(R.id.fullServiceSelector);
        selfServiceSelector = findViewById(R.id.selfServiceSelector);
        fullServiceUnderline = findViewById(R.id.fullServiceUnderline);
        selfServiceUnderline = findViewById(R.id.selfServiceUnderline);

        // Checkboxes
        superwashCheckBox = findViewById(R.id.superwashCheckBox);
        dryCheckBox = findViewById(R.id.dryCheckBox);
        foldCheckBox = findViewById(R.id.foldCheckBox);


        // Quantity displays
        arielQuantity = findViewById(R.id.arielQuantity);
        downyQuantity = findViewById(R.id.downyQuantity);

        // Radio Group
        loadTypeRadioGroup = findViewById(R.id.loadTypeRadioGroup);
        regularLoadType = findViewById(R.id.regularLoadType);
        heavyItemsLoadType = findViewById(R.id.heavyItemsLoadType);
        smallComforterLoadType = findViewById(R.id.smallComforterLoadType);
        largeComforterLoadType = findViewById(R.id.largeComforterLoadType);
    }

    private void resetAllSelections() {
        // Reset service selection underlines
        fullServiceUnderline.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        selfServiceUnderline.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));

        // Reset radio buttons
        loadTypeRadioGroup.clearCheck();

        // Reset checkboxes
        superwashCheckBox.setChecked(false);
        dryCheckBox.setChecked(false);
        foldCheckBox.setChecked(false);

        // Reset quantities
        arielQty = 0;
        downyQty = 0;
        updateQuantityDisplays();

        // Reset state
        isFullServiceSelected = false;
    }

    private void setupServiceSelection() {
        fullServiceSelector.setOnClickListener(v -> {
            selectFullService();
            updateSelection(true);
        });

        selfServiceSelector.setOnClickListener(v -> {
            selectSelfService();
            updateSelection(false);
        });
    }

    private void setupQuantityControls() {
        // Ariel controls
        findViewById(R.id.btnArielMinus).setOnClickListener(v -> {
            if (arielQty > 0) arielQty--;
            updateQuantityDisplays();
            updateTotalDisplay();
        });

        findViewById(R.id.btnArielPlus).setOnClickListener(v -> {
            if (arielQty < 99) arielQty++;
            updateQuantityDisplays();
            updateTotalDisplay();
        });

        // Downy controls
        findViewById(R.id.btnDownyMinus).setOnClickListener(v -> {
            if (downyQty > 0) downyQty--;
            updateQuantityDisplays();
            updateTotalDisplay();
        });

        findViewById(R.id.btnDownyPlus).setOnClickListener(v -> {
            if (downyQty < 99) downyQty++;
            updateQuantityDisplays();
            updateTotalDisplay();
        });
    }

    private void setupRadioGroup() {
            // Set button tint for all radio buttons
            ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_color));
            regularLoadType.setButtonTintList(colorStateList);
            heavyItemsLoadType.setButtonTintList(colorStateList);
            smallComforterLoadType.setButtonTintList(colorStateList);
            largeComforterLoadType.setButtonTintList(colorStateList);

            // Set initial state
            updateLoadTypeEnabledState();

            // Handle superwash checkbox changes
            superwashCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateLoadTypeEnabledState();
            });
    }

    private void updateLoadTypeEnabledState() {
        if (isFullServiceSelected) {
            // Always enabled for full service
            regularLoadType.setEnabled(true);
            heavyItemsLoadType.setEnabled(true);
            smallComforterLoadType.setEnabled(true);
            largeComforterLoadType.setEnabled(true);
        } else {
            // Only enabled for self-service if superwash is checked
            regularLoadType.setEnabled(superwashCheckBox.isChecked());
            heavyItemsLoadType.setEnabled(superwashCheckBox.isChecked());
            smallComforterLoadType.setEnabled(superwashCheckBox.isChecked());
            largeComforterLoadType.setEnabled(superwashCheckBox.isChecked());

            if (!superwashCheckBox.isChecked()) {
                loadTypeRadioGroup.clearCheck(); // Clear selection when disabled
            }
        }
    }

    private String getSelectedLoadType() {
        int selectedId = loadTypeRadioGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.regularLoadType) {
            return "Regular Clothes";
        } else if (selectedId == R.id.heavyItemsLoadType) {
            return "Heavy Items";
        } else if (selectedId == R.id.smallComforterLoadType) {
            return "Small Comforters";
        } else if (selectedId == R.id.largeComforterLoadType) {
            return "Large Comforters";
        }
        return ""; // Return empty string if nothing is selected
    }

    private void setupCloseButton() {
        closeAddOrder.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(this::finish)
                                .start();
                    })
                    .start();
        });
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void selectFullService() {
            isFullServiceSelected = true;
            setAllServicesChecked(true);
            arielQty = 2;
            downyQty = 3;
            updateQuantityDisplays();
            setServicesEnabled(false);
            setAddonsEnabled(false);
            updateLoadTypeEnabledState();
            updateSelection(true);
            updateTotalDisplay();

    }

    private void selectSelfService() {
        isFullServiceSelected = false;
        setAllServicesChecked(false);
        arielQty = 0;
        downyQty = 0;
        updateQuantityDisplays();
        setServicesEnabled(true);
        setAddonsEnabled(true);
        updateLoadTypeEnabledState();
        updateSelection(false);
        updateTotalDisplay();
    }

    private void setAllServicesChecked(boolean checked) {
        superwashCheckBox.setChecked(checked);
        dryCheckBox.setChecked(checked);
        foldCheckBox.setChecked(checked);
    }

    private void setServicesEnabled(boolean enabled) {
        superwashCheckBox.setEnabled(enabled);
        dryCheckBox.setEnabled(enabled);
        foldCheckBox.setEnabled(enabled);
    }

    private void setAddonsEnabled(boolean enabled) {
        findViewById(R.id.btnArielMinus).setEnabled(enabled);
        findViewById(R.id.btnArielPlus).setEnabled(enabled);
        findViewById(R.id.btnDownyMinus).setEnabled(enabled);
        findViewById(R.id.btnDownyPlus).setEnabled(enabled);
    }

    private void updateSelection(boolean isFullServiceSelected) {
            int selectedColor = ContextCompat.getColor(this, R.color.light_red);
            int unselectedColor = ContextCompat.getColor(this, R.color.transparent);

            if (fullServiceUnderline != null) {
                fullServiceUnderline.setBackgroundColor(
                        isFullServiceSelected ? selectedColor : unselectedColor);
            }

            if (selfServiceUnderline != null) {
                selfServiceUnderline.setBackgroundColor(
                        !isFullServiceSelected ? selectedColor : unselectedColor);
            }
    }

    private void updateQuantityDisplays() {
        arielQuantity.setText(String.valueOf(arielQty));
        downyQuantity.setText(String.valueOf(downyQty));
    }

    private int calculateTotal() {
        if (isFullServiceSelected) {
            // Full service always costs ₱150 regardless of add-ons
            return FULL_SERVICE_PRICE;
        } else {
            // Self-service pricing logic
            int total = 0;
            boolean hasSuperwash = superwashCheckBox.isChecked();
            boolean hasDry = dryCheckBox.isChecked();

            if (hasSuperwash && hasDry) {
                total += COMBO_PRICE;
            } else {
                if (hasSuperwash) total += SUPERWASH_PRICE;
                if (hasDry) total += DRY_PRICE;
            }

            if (foldCheckBox.isChecked()) {
                total += FOLD_PRICE;
            }

            // Add add-ons costs
            total += (arielQty * ARIEL_PRICE);
            total += (downyQty * DOWNY_PRICE);

            return total;
        }
    }

    private void updateTotalDisplay() {
        int total = calculateTotal();
        orderTotalPrice.setText(String.format("Total: ₱%d", total));
    }

    // Add this new method to setup checkbox listeners
    private void setupCheckboxListeners() {
        superwashCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLoadTypeEnabledState();
            updateTotalDisplay();
        });

        dryCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTotalDisplay();
        });

        foldCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTotalDisplay();
        });
    }
}
