package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.jitendersingh.friendsengineer.adapters.AdvancePagerAdapter;

public class AdvanceRequestActivity extends BaseActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AdvancePagerAdapter pagerAdapter;
    private LinearLayout backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advance_request);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar for modern look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        backButton = findViewById(R.id.backButton);
        tabLayout = findViewById(R.id.advance_tab_layout);
        viewPager = findViewById(R.id.advance_view_pager);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup ViewPager2
        pagerAdapter = new AdvancePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Setup TabLayout with ViewPager2 using TabLayoutMediator
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Pending");
                            break;
                        case 1:
                            tab.setText("Accepted");
                            break;
                        case 2:
                            tab.setText("Rejected");
                            break;
                    }
                }
        ).attach();
    }
}
