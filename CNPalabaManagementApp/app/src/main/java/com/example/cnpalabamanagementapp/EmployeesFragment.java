package com.example.cnpalabamanagementapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EmployeesFragment extends Fragment implements EmployeeAdapter.OnEmployeeClickListener {
    private static final String PREFS_NAME = "EmployeePrefs";
    private static final String SORT_MODE_KEY = "sort_mode";
    private static final int SORT_BY_AZ = 0;
    private static final int SORT_BY_ZA = 1;
    private static final int SORT_BY_NEWEST = 2;

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private EmployeeViewModel viewModel;
    private ImageView sortButton;// Added ViewModel
    private EditText searchEditText;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_employees, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel (shared at activity level)
        viewModel = new ViewModelProvider(requireActivity()).get(EmployeeViewModel.class);
        searchEditText = view.findViewById(R.id.searchEditText);
        setupSearch();
        recyclerView = view.findViewById(R.id.eemployeesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new EmployeeAdapter(new ArrayList<>(), requireContext(), this); // Pass the fragment as listener
        recyclerView.setAdapter(adapter);


        sortButton = view.findViewById(R.id.sortButton);

        sortButton.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            List<Employee> currentList = new ArrayList<>(adapter.getEmployeeList());
            if (currentList == null || currentList.isEmpty()) return;

            int sortMode = prefs.getInt(SORT_MODE_KEY, SORT_BY_NEWEST);

            switch (sortMode) {
                case SORT_BY_AZ:
                    Collections.sort(currentList, (e1, e2) -> e2.getFullName().compareToIgnoreCase(e1.getFullName()));
                    editor.putInt(SORT_MODE_KEY, SORT_BY_ZA);
                    Toast.makeText(requireContext(), "Sorted by Z - A", Toast.LENGTH_SHORT).show();
                    break;
                case SORT_BY_ZA:
                    Collections.sort(currentList, (e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt())); // Newest
                    editor.putInt(SORT_MODE_KEY, SORT_BY_NEWEST);
                    sortButton.setImageResource(R.drawable.sort_descending);
                    Toast.makeText(requireContext(), "Sorted by Newest", Toast.LENGTH_SHORT).show();
                    break;
                case SORT_BY_NEWEST:
                default:
                    Collections.sort(currentList, (e1, e2) -> e1.getFullName().compareToIgnoreCase(e2.getFullName()));
                    editor.putInt(SORT_MODE_KEY, SORT_BY_AZ);
                    Toast.makeText(requireContext(), "Sorted by A - Z", Toast.LENGTH_SHORT).show();
                    break;
            }

            editor.apply();
            adapter.updateList(currentList);
        });

        // Observe employee data
        viewModel.getEmployees().observe(getViewLifecycleOwner(), employees -> {
            if (employees == null || employees.isEmpty()) return;

            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int sortMode = prefs.getInt(SORT_MODE_KEY, SORT_BY_NEWEST);

            switch (sortMode) {
                case SORT_BY_AZ:
                    Collections.sort(employees, (e1, e2) -> e1.getFullName().compareToIgnoreCase(e2.getFullName()));
                    break;
                case SORT_BY_ZA:
                    Collections.sort(employees, (e1, e2) -> e2.getFullName().compareToIgnoreCase(e1.getFullName()));
                    break;
                case SORT_BY_NEWEST:
                default:
                    Collections.sort(employees, (e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()));
                    sortButton.setImageResource(R.drawable.sort_descending);
                    break;
            }

            adapter.updateList(employees);

            if (!employees.isEmpty()) {
                Log.d("VIEWMODEL_DATA", "First employee: " + employees.get(0).getFullName());
            }
        });

        // Add Staff button handler
        view.findViewById(R.id.AddStaffBtn).setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), AddStaff.class));
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add clear functionality if needed
        searchEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawable = searchEditText.getCompoundDrawables()[2];
                if (drawable != null && event.getRawX() >= (searchEditText.getRight() - drawable.getBounds().width())) {
                    searchEditText.setText("");
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.fetchEmployees(); // Trigger data fetch when fragment becomes visible
    }

    @Override
    public void onEmployeeClick(Employee employee) {
        Toast.makeText(requireContext(), "Clicked: " + employee.getFullName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireActivity(), ViewStaffDetails.class);

        intent.putExtra("employee", employee);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.fetchEmployees(); // Refresh list when returning
    }
}


