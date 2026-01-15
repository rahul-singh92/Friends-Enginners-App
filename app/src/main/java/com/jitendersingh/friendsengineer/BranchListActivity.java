package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class BranchListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private BranchAdapter adapter;
    private List<String> branches;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_list);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar for modern look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewBranches);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get branches from resources
        String[] branchArray = getResources().getStringArray(R.array.branch_array);
        branches = Arrays.asList(branchArray);

        adapter = new BranchAdapter(branches, this::onBranchClicked);
        recyclerView.setAdapter(adapter);

        // Show/hide empty state
        if (branches.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void onBranchClicked(String branchName) {
        Intent intent = new Intent(BranchListActivity.this, BranchPdfListActivity.class);
        intent.putExtra("branchName", branchName);
        startActivity(intent);
    }
}
