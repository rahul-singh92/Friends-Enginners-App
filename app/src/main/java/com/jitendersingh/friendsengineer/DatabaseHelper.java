package com.jitendersingh.friendsengineer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.util.Log;

import com.jitendersingh.friendsengineer.models.Worker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "excel_data.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_PREFIX = "excel_data_";
    private static final String TAG = "DatabaseHelper";

    // Default table name used if not specified
    private static final String DEFAULT_TABLE_NAME = "excel_data_table";
    private String currentTableName = DEFAULT_TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Database tables will be created dynamically based on Excel columns
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades if needed
    }

    /**
     * Creates a table dynamically based on column names from Excel file
     * @param columnNames List of column names from the Excel file's first row
     */
    public void createTableIfNotExists(List<String> columnNames, String sheetType, String year, String month) {
        if (columnNames == null || columnNames.isEmpty()) {
            Log.e(TAG, "Cannot create table with empty column list");
            return;
        }

        // Compose new table name
        currentTableName = "excel_data_" + sanitizeColumnName(sheetType) + "_" + sanitizeColumnName(year) + "_" + sanitizeColumnName(month);

        SQLiteDatabase db = getWritableDatabase();

        try {
            // Check if table already exists
            if (isTableExists(db, currentTableName)) {
                // Drop old table before creating new
                Log.d(TAG, "Table exists, deleting old table: " + currentTableName);
                deleteTable(currentTableName);
            }

            // Build SQL for table creation
            StringBuilder createTableSQL = new StringBuilder();
            createTableSQL.append("CREATE TABLE IF NOT EXISTS ").append(currentTableName).append(" (");
            createTableSQL.append("id INTEGER PRIMARY KEY AUTOINCREMENT");

            for (String columnName : columnNames) {
                String sanitizedColumnName = sanitizeColumnName(columnName);
                createTableSQL.append(", ").append(sanitizedColumnName).append(" TEXT");
            }
            createTableSQL.append(")");

            Log.d(TAG, "Creating table with SQL: " + createTableSQL.toString());
            db.execSQL(createTableSQL.toString());
            Log.d(TAG, "Table created: " + currentTableName);

        } catch (Exception e) {
            Log.e(TAG, "Error creating table: " + e.getMessage(), e);
        }
    }

    public void createWageTableWithDate(List<String> columnNames, String scheduleType, String day, String month, String year) {
        if (columnNames == null || columnNames.isEmpty()) {
            Log.e(TAG, "Cannot create table with empty column list");
            return;
        }

        // Build table name: schedule_24_05_2025
        currentTableName = sanitizeColumnName(scheduleType) + "_" +
                sanitizeColumnName(day) + "_" +
                sanitizeColumnName(month) + "_" +
                sanitizeColumnName(year);

        SQLiteDatabase db = getWritableDatabase();

        try {
            if (isTableExists(db, currentTableName)) {
                Log.d(TAG, "Table already exists, deleting: " + currentTableName);
                deleteTable(currentTableName);
            }

            StringBuilder createTableSQL = new StringBuilder();
            createTableSQL.append("CREATE TABLE IF NOT EXISTS ").append(currentTableName).append(" (");
            createTableSQL.append("id INTEGER PRIMARY KEY AUTOINCREMENT");

            for (String columnName : columnNames) {
                String sanitizedColumnName = sanitizeColumnName(columnName);
                createTableSQL.append(", ").append(sanitizedColumnName).append(" TEXT");
            }

            createTableSQL.append(")");
            db.execSQL(createTableSQL.toString());
            Log.d(TAG, "Wage Table created: " + currentTableName);

        } catch (Exception e) {
            Log.e(TAG, "Error creating wage table: " + e.getMessage(), e);
        }
    }

    // Add this method to create the credentials table dynamically
    public void createCredentialsTableIfNotExists(String tableName, List<String> columnNames) {
        SQLiteDatabase db = this.getWritableDatabase();
        StringBuilder createQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(tableName).append(" (id INTEGER PRIMARY KEY AUTOINCREMENT");

        for (String column : columnNames) {
            createQuery.append(", ").append(column).append(" TEXT");
        }

        createQuery.append(");");
        db.execSQL(createQuery.toString());
    }

    // Add this method to insert data into the credentials table
    public void insertIntoCredentialsTable(String tableName, List<String> columnNames, List<List<String>> data) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (List<String> row : data) {
            ContentValues values = new ContentValues();
            for (int i = 0; i < columnNames.size(); i++) {
                values.put(columnNames.get(i), row.get(i));
            }
            db.insert(tableName, null, values);
        }
    }



    /**
     * Checks if a table exists in the database
     */
    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }
        try {
            String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
            String[] args = {tableName};
            try (android.database.Cursor cursor = db.rawQuery(query, args)) {
                boolean exists = (cursor != null && cursor.getCount() > 0);
                return exists;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if table exists: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<String> getCredentialTables() {
        ArrayList<String> tables = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'credentials_%'",
                null
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }
            cursor.close();
        }
        return tables;
    }

    public List<String> getCredentialTableData(String tableName) {
        List<String> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + tableName;

        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndex("id");
                    int nameIndex = cursor.getColumnIndex("Name");
                    int fatherIndex = cursor.getColumnIndex("FATHER_S_NAME");
                    int usernameIndex = cursor.getColumnIndex("Username");
                    int passwordIndex = cursor.getColumnIndex("Password");

                    String id = idIndex != -1 ? cursor.getString(idIndex) : "";
                    String name = nameIndex != -1 ? cursor.getString(nameIndex) : "";
                    String father = fatherIndex != -1 ? cursor.getString(fatherIndex) : "";
                    String uname = usernameIndex != -1 ? cursor.getString(usernameIndex) : "";
                    String pass = passwordIndex != -1 ? cursor.getString(passwordIndex) : "";

                    String formatted = "ID: " + id + "\nName: " + name + "\nFather's Name: " + father + "\nUsername: " + uname + "\nPassword: " + pass;
                    dataList.add(formatted);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dataList;
    }





    /**
     * Inserts data from Excel into the database table
     * @param columnNames List of column names from the Excel file
     * @param data List of data rows from the Excel file
     */
    public void insertDynamicData(List<String> columnNames, List<List<String>> data) {
        if (columnNames == null || columnNames.isEmpty() || data == null || data.isEmpty()) {
            Log.e(TAG, "Cannot insert data: invalid column names or data");
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        int totalRows = data.size();
        int processedRows = 0;
        int batchSize = 25; // Insert in smaller batches to avoid transaction too large errors

        try {
            Log.d(TAG, "Starting insertion of " + totalRows + " rows");

            while (processedRows < totalRows) {
                db.beginTransaction();
                int endIndex = Math.min(processedRows + batchSize, totalRows);

                for (int rowIndex = processedRows; rowIndex < endIndex; rowIndex++) {
                    List<String> row = data.get(rowIndex);
                    ContentValues values = new ContentValues();

                    for (int i = 0; i < columnNames.size(); i++) {
                        String columnName = sanitizeColumnName(columnNames.get(i));
                        String value = (i < row.size() && row.get(i) != null && !row.get(i).isEmpty()) ?
                                row.get(i) : "NO VALUE";
                        values.put(columnName, value);
                    }

                    long newRowId = db.insert(currentTableName, null, values);
                    if (newRowId == -1) {
                        Log.e(TAG, "Error inserting row " + rowIndex);
                    }
                }

                db.setTransactionSuccessful();
                db.endTransaction();

                processedRows = endIndex;
                Log.d(TAG, "Processed " + processedRows + " of " + totalRows + " rows");
            }

            Log.d(TAG, "All data inserted successfully into " + currentTableName);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting data: " + e.getMessage(), e);
        }
    }

    /**
     * Sanitizes column names to be SQLite-compatible
     * @param columnName The raw column name from Excel
     * @return A sanitized column name safe for SQLite
     */
    private String sanitizeColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return "column_unknown";
        }

        // Replace spaces and special characters, ensure it starts with a letter
        String sanitized = columnName.trim().replaceAll("[^a-zA-Z0-9_]", "_");

        // Ensure column name starts with a letter or underscore
        if (!sanitized.matches("^[a-zA-Z_].*")) {
            sanitized = "col_" + sanitized;
        }

        // Ensure the column name isn't a SQLite reserved keyword
        if (isReservedKeyword(sanitized)) {
            sanitized = "col_" + sanitized;
        }

        return sanitized;
    }

    /**
     * Checks if a string is a SQLite reserved keyword
     * @param word The word to check
     * @return True if the word is a reserved keyword
     */
    private boolean isReservedKeyword(String word) {
        String[] keywords = {"ADD", "ALL", "ALTER", "AND", "AS", "AUTOINCREMENT",
                "BETWEEN", "BY", "CASE", "CHECK", "COLLATE", "COLUMN",
                "COMMIT", "CONSTRAINT", "CREATE", "DEFAULT", "DELETE",
                "DESC", "DISTINCT", "DROP", "ELSE", "ESCAPE", "EXISTS",
                "FROM", "GROUP", "HAVING", "IN", "INDEX", "INSERT",
                "INTO", "IS", "JOIN", "KEY", "LIKE", "LIMIT", "NOT",
                "NULL", "ON", "OR", "ORDER", "PRIMARY", "REFERENCES",
                "SELECT", "SET", "TABLE", "THEN", "TO", "TRANSACTION",
                "UNION", "UNIQUE", "UPDATE", "USING", "VALUES", "VIEW",
                "WHEN", "WHERE"};

        String upperWord = word.toUpperCase();
        for (String keyword : keywords) {
            if (keyword.equals(upperWord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the name of the current table
     * @return The name of the current table
     */
    public String getCurrentTableName() {
        return currentTableName;
    }

    /**
     * Deletes a table from the database
     * @param tableName The name of the table to delete
     */
    public void deleteTable(String tableName) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            Log.d(TAG, "Table deleted: " + tableName);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting table: " + e.getMessage());
        }
    }

    public boolean isValidWorker(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM credentials_worker WHERE Username = ? AND Password = ?", new String[]{username, password});

        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        return exists;
    }

    public Cursor getWorkerNameByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT Name,FATHER_S_NAME FROM credentials_worker WHERE Username = ?", new String[]{username});
    }

    public void createRequestAdvanceTableIfNotExists() {
        SQLiteDatabase db = this.getWritableDatabase();
        String createQuery = "CREATE TABLE IF NOT EXISTS Request_Advance_Table (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "father_name TEXT, " +
                "request_amount TEXT, " +
                "reason TEXT, " +
                "request_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";
        db.execSQL(createQuery);
    }

    public boolean insertRequestAdvance(String name, String fatherName, String amount, String reason) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("father_name", fatherName);
        cv.put("request_amount", amount);
        cv.put("reason", reason);
        // Manually add local current time string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());  // ensure local timezone
        String currentDateTime = sdf.format(new Date());

        cv.put("request_time", currentDateTime); // insert time yourself

        long result = db.insert("Request_Advance_Table", null, cv);
        return result != -1;
    }

    public boolean deleteRequest(String name, String requestTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affectedRows = db.delete("Request_Advance_Table", "name = ? AND request_time = ?", new String[]{name, requestTime});
        db.close();
        return affectedRows > 0;
    }

    public void createAcceptedAdvanceTableIfNotExists() {
        SQLiteDatabase db = this.getWritableDatabase();
        String createTable = "CREATE TABLE IF NOT EXISTS Accepted_Advance_Request (" +
                "name TEXT, " +
                "father_name TEXT, " +
                "request_amount TEXT, " +
                "reason TEXT, " +
                "request_time TEXT PRIMARY KEY)";
        db.execSQL(createTable);
        db.close();
    }

    public boolean insertAcceptedRequest(Worker worker) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", worker.getName());
        values.put("father_name", worker.getFatherName());
        values.put("request_amount", worker.getAmount());
        values.put("reason", worker.getReason());
        values.put("request_time", worker.getRequestTime());

        long result = db.insert("Accepted_Advance_Request", null, values);
        db.close();
        return result != -1;
    }

    public List<Worker> getAcceptedRequestsFromDB() {
        createAcceptedAdvanceTableIfNotExists();

        List<Worker> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT name, father_name, request_amount, reason, request_time FROM Accepted_Advance_Request", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String fatherName = cursor.getString(1);
                String amount = cursor.getString(2);
                String reason = cursor.getString(3);
                String requestTime = cursor.getString(4);

                list.add(new Worker("",name, fatherName, amount, reason, requestTime));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public boolean deleteAcceptedRequest(String name, String requestTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affectedRows = db.delete("Accepted_Advance_Request",
                "name=? AND request_time=?",
                new String[]{name, requestTime});
        db.close();
        return affectedRows > 0;
    }

    public void createRejectedAdvanceRequestTableIfNotExists() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS Rejected_Advance_Request (" +
                "name TEXT, father_name TEXT, request_amount TEXT, reason TEXT, request_time TEXT)");
        db.close();
    }

    public boolean insertRejectedRequest(Worker worker) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", worker.getName());
        values.put("father_name", worker.getFatherName());
        values.put("request_amount", worker.getAmount());
        values.put("reason", worker.getReason());
        values.put("request_time", worker.getRequestTime());

        long result = db.insert("Rejected_Advance_Request", null, values);
        db.close();
        return result != -1;
    }

    public int getRequestCountThisMonth(String username) {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        // Get worker's name from username
        String name = null;
        Cursor nameCursor = getWorkerNameByUsername(username);
        if (nameCursor != null && nameCursor.moveToFirst()) {
            int nameIndex = nameCursor.getColumnIndex("Name");
            if (nameIndex != -1) {
                name = nameCursor.getString(nameIndex);
            }
            nameCursor.close();
        }

        if (name == null) return 0;

        // SQL for current month
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        // Count in Request_Advance_Table
        Cursor pendingCursor = db.rawQuery(
                "SELECT COUNT(*) FROM Request_Advance_Table WHERE name = ? AND strftime('%Y-%m', request_time) = ?",
                new String[]{name, currentMonth});
        if (pendingCursor.moveToFirst()) count += pendingCursor.getInt(0);
        pendingCursor.close();

        // Count in Accepted_Advance_Request
        Cursor acceptedCursor = db.rawQuery(
                "SELECT COUNT(*) FROM Accepted_Advance_Request WHERE name = ? AND strftime('%Y-%m', request_time) = ?",
                new String[]{name, currentMonth});
        if (acceptedCursor.moveToFirst()) count += acceptedCursor.getInt(0);
        acceptedCursor.close();

        db.close();
        return count;
    }

    public void insertWorkerDetail(String pNo, String esiNo, String uanNo, String pfNo,
                                   String name, String fatherName, String contact, String doj,
                                   String dept, String dol, String image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String createWorkerDetailTable = "CREATE TABLE IF NOT EXISTS worker_detail (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "p_no TEXT, " +
                "esi_no TEXT, " +
                "uan_no TEXT, " +
                "pf_no TEXT, " +
                "name TEXT, " +
                "father_name TEXT, " +
                "contact_number TEXT, " +
                "date_of_joining TEXT, " +
                "department TEXT, " +
                "date_of_leave TEXT," +
                "image TEXT)";
        db.execSQL(createWorkerDetailTable);


        values.put("p_no", pNo);
        values.put("esi_no", esiNo);
        values.put("uan_no", uanNo);
        values.put("pf_no", pfNo);
        values.put("name", name);
        values.put("father_name", fatherName);
        values.put("contact_number", contact);
        values.put("date_of_joining", doj);
        values.put("department", dept);
        values.put("date_of_leave", dol);
        values.put("image", image);

        db.insert("worker_detail", null, values);
        db.close();
    }

    public Cursor getWorkerDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, father_name FROM worker_detail", null);
    }

    public Cursor getSingleWorkerDetail(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM worker_detail WHERE id = ?", new String[]{String.valueOf(id)});
    }

    public Cursor getWorkerDetailsByNameAndFather(String name, String fatherName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM worker_detail WHERE name=? AND father_name=?", new String[]{name, fatherName});
    }

}