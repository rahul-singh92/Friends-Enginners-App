package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class TableListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView listView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_list);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Available Data Tables");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        listView = findViewById(R.id.table_list_view);
        emptyView = findViewById(R.id.empty_view);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Load and display available tables
        loadTables();

        // Set item click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String tableName = (String) parent.getItemAtPosition(position);

            // Launch the data list activity with the selected table name
            Intent intent = new Intent(TableListActivity.this, DataListActivity.class);
            intent.putExtra("TABLE_NAME", tableName);
            startActivity(intent);
        });

        // Set long item click listener for deleting table
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String tableName = (String) parent.getItemAtPosition(position);

            new androidx.appcompat.app.AlertDialog.Builder(TableListActivity.this)
                    .setTitle("Delete Table")
                    .setMessage("Are you sure you want to delete table: " + tableName + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteTable(tableName);
                        loadTables(); // refresh list
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }

    private void loadTables() {
        List<String> tableNames = getTableNames();

        if (tableNames.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, tableNames);
            listView.setAdapter(adapter);
        }
    }

    private List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query for table names that start with our prefix
        try (Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'excel_data_%'", null)) {

            if (cursor.moveToFirst()) {
                do {
                    String tableName = cursor.getString(0);
                    tableNames.add(tableName);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tableNames;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}