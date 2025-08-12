package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BranchListActivity extends AppCompatActivity {

    ListView listView;
    String[] branches;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_list);

        listView = findViewById(R.id.branch_listview);

        branches = getResources().getStringArray(R.array.branch_array);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, branches);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBranch = branches[position];
            Intent intent = new Intent(BranchListActivity.this, BranchPdfListActivity.class);
            intent.putExtra("branchName", selectedBranch);
            startActivity(intent);
        });
    }
}
