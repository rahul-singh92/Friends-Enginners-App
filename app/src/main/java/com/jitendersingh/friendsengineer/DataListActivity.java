package com.jitendersingh.friendsengineer;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;  // Added for confirmation dialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class DataListActivity extends AppCompatActivity {

    private static final String TAG = "DataListActivity";
    private String tableName;
    private DatabaseHelper dbHelper;
    private ListView listView;
    private TextView emptyView;
    private SearchView searchView;
    private DataAdapter adapter;
    private List<DataItem> allDataItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tableName = getIntent().getStringExtra("TABLE_NAME");
        if (tableName == null || tableName.isEmpty()) {
            Toast.makeText(this, "Invalid table name", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Records");
            getSupportActionBar().setSubtitle(tableName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.data_list_view);
        emptyView = findViewById(R.id.empty_view);
        searchView = findViewById(R.id.search_view);

        dbHelper = new DatabaseHelper(this);

        loadData();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterData(newText);
                return true;
            }
        });

        // Normal click: open detail activity
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DataItem item = (DataItem) parent.getItemAtPosition(position);
            Intent intent = new Intent(DataListActivity.this, DataDetailActivity.class);
            intent.putExtra("TABLE_NAME", tableName);
            intent.putExtra("RECORD_ID", item.getId());
            startActivity(intent);
        });

        // Long click: confirm and delete record
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            DataItem item = (DataItem) parent.getItemAtPosition(position);
            showDeleteConfirmationDialog(item);
            return true; // consume the event
        });
    }

    private void showDeleteConfirmationDialog(DataItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this record?\n\nName: " + item.getName())
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteRecord(item.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecord(int recordId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete(tableName, "id = ?", new String[]{String.valueOf(recordId)});
        if (deletedRows > 0) {
            Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
            // Refresh data list after deletion
            loadData();
        } else {
            Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {
        allDataItems = getDataFromTable();

        if (allDataItems.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            adapter = new DataAdapter(this, allDataItems);
            listView.setAdapter(adapter);
        }
    }

    private void filterData(String query) {
        List<DataItem> filtered = new ArrayList<>();
        for (DataItem item : allDataItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase()) ||
                    item.getFatherName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter = new DataAdapter(this, filtered);
        listView.setAdapter(adapter);
    }

    private List<DataItem> getDataFromTable() {
        List<DataItem> dataItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            boolean hasNameColumn = false;
            boolean hasFatherNameColumn = false;

            try (Cursor columnCursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
                int nameIndex = columnCursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    while (columnCursor.moveToNext()) {
                        String columnName = columnCursor.getString(nameIndex).toUpperCase();
                        if (columnName.equals("NAME")) {
                            hasNameColumn = true;
                        } else if (columnName.contains("FATHER") && columnName.contains("NAME")) {
                            hasFatherNameColumn = true;
                        }
                    }
                }
            }

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT id");

            String nameColumn = hasNameColumn ? ", Name" : "";
            String fatherNameColumn = hasFatherNameColumn ? ", FATHER_S_NAME" : "";

            if (!hasNameColumn && !hasFatherNameColumn) {
                try (Cursor columnCursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
                    int nameIndex = columnCursor.getColumnIndex("name");
                    List<String> availableColumns = new ArrayList<>();

                    if (nameIndex != -1) {
                        while (columnCursor.moveToNext() && availableColumns.size() < 2) {
                            String colName = columnCursor.getString(nameIndex);
                            if (!colName.equals("id")) {
                                availableColumns.add(colName);
                            }
                        }
                    }

                    for (String col : availableColumns) {
                        queryBuilder.append(", ").append(col);
                    }
                }
            } else {
                queryBuilder.append(nameColumn).append(fatherNameColumn);
            }

            queryBuilder.append(" FROM ").append(tableName);

            try (Cursor cursor = db.rawQuery(queryBuilder.toString(), null)) {
                int idColumnIndex = cursor.getColumnIndex("id");
                int nameColumnIndex = hasNameColumn ? cursor.getColumnIndex("Name") : -1;
                int fatherNameColumnIndex = hasFatherNameColumn ? cursor.getColumnIndex("FATHER_S_NAME") : -1;

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(idColumnIndex);
                    String name = nameColumnIndex != -1 ? cursor.getString(nameColumnIndex) : "N/A";
                    String fatherName = fatherNameColumnIndex != -1 ? cursor.getString(fatherNameColumnIndex) : "N/A";

                    dataItems.add(new DataItem(id, name, fatherName));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading data: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return dataItems;
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

    public static class DataItem {
        private final int id;
        private final String name;
        private final String fatherName;

        public DataItem(int id, String name, String fatherName) {
            this.id = id;
            this.name = name;
            this.fatherName = fatherName;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getFatherName() {
            return fatherName;
        }
    }
}
