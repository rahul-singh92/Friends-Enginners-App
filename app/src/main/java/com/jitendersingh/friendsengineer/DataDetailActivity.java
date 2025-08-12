package com.jitendersingh.friendsengineer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataDetailActivity extends AppCompatActivity {

    private static final String TAG = "DataDetailActivity";
    private String tableName;
    private int recordId;
    private DatabaseHelper dbHelper;
    private LinearLayout detailContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_detail);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get parameters from intent
        tableName = getIntent().getStringExtra("TABLE_NAME");
        recordId = getIntent().getIntExtra("RECORD_ID", -1);

        if (tableName == null || tableName.isEmpty() || recordId == -1) {
            Toast.makeText(this, "Invalid parameters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Record Details");
            getSupportActionBar().setSubtitle("ID: " + recordId);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        detailContainer = findViewById(R.id.detail_container);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Load and display record details
        loadRecordDetails();
    }

    private void loadRecordDetails() {
        Map<String, String> recordDetails = getRecordDetails();

        if (recordDetails.isEmpty()) {
            Toast.makeText(this, "No details found for this record", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        // Display each field in the container
        for (Map.Entry<String, String> entry : recordDetails.entrySet()) {
            View itemView = inflater.inflate(R.layout.item_field_detail, detailContainer, false);

            TextView labelView = itemView.findViewById(R.id.field_label);
            TextView valueView = itemView.findViewById(R.id.field_value);

            String label = formatColumnName(entry.getKey());
            String value = entry.getValue();

            labelView.setText(label);
            valueView.setText(value);

            detailContainer.addView(itemView);
        }
    }

    private Map<String, String> getRecordDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Get all column names first
            try (Cursor columnCursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
                int nameIndex = columnCursor.getColumnIndex("name");
                StringBuilder columnsBuilder = new StringBuilder();

                if (nameIndex != -1) {
                    while (columnCursor.moveToNext()) {
                        String columnName = columnCursor.getString(nameIndex);
                        columnsBuilder.append(columnName).append(", ");
                    }
                }

                String columns = columnsBuilder.toString();
                if (columns.endsWith(", ")) {
                    columns = columns.substring(0, columns.length() - 2);
                }

                // Query the specific record
                String query = "SELECT " + columns + " FROM " + tableName + " WHERE id = ?";
                try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(recordId)})) {
                    if (cursor != null && cursor.moveToFirst()) {
                        for (int i = 0; i < cursor.getColumnCount(); i++) {
                            String columnName = cursor.getColumnName(i);
                            String value = cursor.getString(i);
                            details.put(columnName, value != null ? value : "NO VALUE");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading record details: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return details;
    }

    private String formatColumnName(String columnName) {
        // Format column name for display (e.g., "first_name" -> "First Name")
        if (columnName == null || columnName.isEmpty()) {
            return "Unknown";
        }

        // Replace underscores with spaces
        String formatted = columnName.replace('_', ' ');

        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : formatted.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
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