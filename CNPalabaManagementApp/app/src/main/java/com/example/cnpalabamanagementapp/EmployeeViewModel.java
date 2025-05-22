package com.example.cnpalabamanagementapp;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class EmployeeViewModel extends ViewModel {
    private final MutableLiveData<List<Employee>> _employees = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Public getter (expose immutable LiveData)
    public LiveData<List<Employee>> getEmployees() {
        return _employees;
    }

    public void fetchEmployees() {
        db.collection("users")
                .whereEqualTo("role", "user")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Employee> employees = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            employees.add(employee);
                        }
                        _employees.setValue(employees); // Update LiveData
                    } else {
                        // Handle error (you can add error LiveData if needed)
                        Log.e("EmployeeViewModel", "Error loading employees", task.getException());
                    }
                });
    }

    public LiveData<Employee> getEmployeeById(String userId) {
        MutableLiveData<Employee> employeeData = new MutableLiveData<>();

        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        employeeData.setValue(task.getResult().toObject(Employee.class));
                    }
                });

        return employeeData;
    }

    public void updateEmployeeProfile(String username, String newImageUrl) {
        if (_employees.getValue() != null) {  // Changed from employeesLiveData to _employees
            List<Employee> updatedList = new ArrayList<>(_employees.getValue());
            for (Employee emp : updatedList) {
                if (emp.getUsername().equals(username)) {
                    emp.setProfileImageUrl(newImageUrl);
                    break;
                }
            }
            _employees.postValue(updatedList);
        }
        fetchEmployees(); // Force refresh from Firestore
    }

    public void deleteEmployee(String username, final OnDeleteListener listener) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Remove from local list
                                    if (_employees.getValue() != null) {
                                        List<Employee> updatedList = new ArrayList<>(_employees.getValue());
                                        updatedList.removeIf(emp -> emp.getUsername().equals(username));
                                        _employees.postValue(updatedList);
                                    }
                                    listener.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    listener.onFailure(e.getMessage());
                                });
                    } else {
                        listener.onFailure("Employee not found");
                    }
                });
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}