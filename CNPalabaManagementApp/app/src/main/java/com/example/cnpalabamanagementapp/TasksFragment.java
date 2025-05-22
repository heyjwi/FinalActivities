package com.example.cnpalabamanagementapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


public class TasksFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FragmentAdapter adapter;

    public TasksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Initialize TabLayout and ViewPager2
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Create and set the adapter for ViewPager2
        adapter = new FragmentAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String title = (position == 0) ? "Announcements" : "Tasks";

            TextView customTextView = new TextView(getContext());
            customTextView.setGravity(Gravity.CENTER);
            customTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f // Optional for weighting
            ));
            customTextView.setText(title);
            customTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            customTextView.setAllCaps(false); // This actually works now
            customTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.inter_24pt_semibold));
            customTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.dark_color));
            customTextView.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.tab_selector));
            tab.setCustomView(customTextView);
        }).attach();

        return view;
    }

    // Create the FragmentAdapter class
    public static class FragmentAdapter extends FragmentStateAdapter {
        public FragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new UserAnnouncementFragment();  // Your Announcements fragment
            } else {
                return new UserTasksFragment();  // Your Tasks fragment
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Number of tabs
        }
    }

    }
