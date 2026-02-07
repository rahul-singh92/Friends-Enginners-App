package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.jitendersingh.friendsengineer.adapters.AdvancePagerAdapter;

public class AdvanceRequestActivity extends BaseActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AdvancePagerAdapter pagerAdapter;
    private LinearLayout backButton;

    private EditText searchBar;
    private Spinner filterSpinner;

    public static String currentSearchText = "";
    public static String currentFilterType = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advance_request);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        backButton = findViewById(R.id.backButton);
        tabLayout = findViewById(R.id.advance_tab_layout);
        viewPager = findViewById(R.id.advance_view_pager);

        searchBar = findViewById(R.id.searchBar);
        filterSpinner = findViewById(R.id.filterSpinner);

        backButton.setOnClickListener(v -> finish());

        // Spinner Filters
        String[] filters = {"All", "Day", "Week", "Month", "Year"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        pagerAdapter = new AdvancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("Pending");
                    else if (position == 1) tab.setText("Accepted");
                    else tab.setText("Rejected");
                }
        ).attach();

        // Search Listener
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().toLowerCase().trim();
                refreshCurrentFragment();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter Listener
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                currentFilterType = filterSpinner.getSelectedItem().toString();
                refreshCurrentFragment();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Refresh when tab changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                refreshCurrentFragment();
            }
        });
    }

    private void refreshCurrentFragment() {
        if (pagerAdapter != null) {
            pagerAdapter.refreshFragment(viewPager.getCurrentItem());
        }
    }
}
