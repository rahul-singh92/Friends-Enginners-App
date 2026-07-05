package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jitendersingh.friendsengineer.adapters.WageCollectionAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-facing, view-only list of workers for a given branch + month.
 * Shows Name + Father's Name only. Tapping a worker generates the PDF
 * on the fly and opens it directly — no download/share buttons here,
 * that's intentionally reserved for the worker-facing screen.
 */
public class AdminEmployeeListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private android.widget.TextView headerTitle;
    private android.widget.TextView headerSubtitle;
    private android.widget.TextView workerCount;

    private WageCollectionAdapter adapter;
    private final List<String> displayList = new ArrayList<>();
    // Parallel list of the actual employee documents, same order as displayList.
    private final List<DocumentSnapshot> employeeDocs = new ArrayList<>();

    // Unfiltered master copies — search filters from these, displayList/
    // employeeDocs above hold whatever subset is currently visible.
    private final List<String> fullDisplayList = new ArrayList<>();
    private final List<DocumentSnapshot> fullEmployeeDocs = new ArrayList<>();

    private android.widget.EditText searchInput;
    private android.widget.TextView emptyTitle;
    private android.widget.TextView emptySubtitle;
    private LinearLayout btnAddWorker;

    private FirebaseFirestore firestore;
    private String department;
    private String month;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_employee_list);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        recyclerView = findViewById(R.id.recyclerViewEmployees);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        workerCount = findViewById(R.id.workerCount);
        searchInput = findViewById(R.id.searchInput);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptySubtitle = findViewById(R.id.emptySubtitle);
        btnAddWorker = findViewById(R.id.btnAddWorker);

        btnAddWorker.setOnClickListener(v -> showAddForm());

        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Long-press on a worker row -> shows an "Edit / Update" option.
        // Implemented via GestureDetector directly on the RecyclerView so
        // we don't need to modify the shared WageCollectionAdapter (which
        // other screens also use without long-press support).
        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(
                this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(android.view.MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child == null) return;

                int position = recyclerView.getChildAdapterPosition(child);
                if (position == RecyclerView.NO_POSITION || position >= employeeDocs.size()) return;

                showItemOptionsDialog(employeeDocs.get(position));
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, android.view.MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }
        });

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        firestore = FirebaseFirestore.getInstance();
        department = getIntent().getStringExtra("department");
        month = getIntent().getStringExtra("month");

        if (department == null || month == null) {
            Toast.makeText(this, "Missing department/month", Toast.LENGTH_SHORT).show();
            showEmpty();
            return;
        }

        headerTitle.setText(department.replace("_", " "));
        headerSubtitle.setText(month.replace("_", " ") + " · Tap a worker to view their wage slip");

        loadEmployees();
    }

    private void loadEmployees() {
        firestore.collection("salary_data")
                .document(department)
                .collection("salary_months")
                .document(month)
                .collection("employees")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    fullDisplayList.clear();
                    fullEmployeeDocs.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = safe(doc.getString("name"));
                        String fatherName = safe(doc.getString("fatherName"));

                        String display = name.isEmpty() ? "Unknown" : name;
                        if (!fatherName.isEmpty()) {
                            display += " - " + fatherName;
                        }

                        fullDisplayList.add(display);
                        fullEmployeeDocs.add(doc);
                    }

                    if (fullDisplayList.isEmpty()) {
                        emptyTitle.setText("No workers found");
                        emptySubtitle.setText("This month has no workers yet");
                        showEmpty();
                        return;
                    }

                    // Initialize the visible list with no filter applied yet.
                    applyFilter(searchInput.getText().toString());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load employees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmpty();
                });
    }

    /**
     * Filters the master employee list by name or father's name
     * (case-insensitive, matches either field) and refreshes the
     * visible list + adapter accordingly.
     */
    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();

        displayList.clear();
        employeeDocs.clear();

        for (int i = 0; i < fullDisplayList.size(); i++) {
            String entry = fullDisplayList.get(i);
            if (q.isEmpty() || entry.toLowerCase().contains(q)) {
                displayList.add(entry);
                employeeDocs.add(fullEmployeeDocs.get(i));
            }
        }

        if (displayList.isEmpty()) {
            emptyTitle.setText("No matching workers");
            emptySubtitle.setText("Try a different name or father's name");
            showEmpty();
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        workerCount.setText(String.valueOf(displayList.size()));

        if (adapter == null) {
            adapter = new WageCollectionAdapter(displayList, this::onEmployeeClicked);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void onEmployeeClicked(String displayedText) {
        int index = displayList.indexOf(displayedText);
        if (index == -1) return;

        DocumentSnapshot doc = employeeDocs.get(index);
        generateAndOpenPdf(doc);
    }

    /** Long-press context menu — currently just "Edit / Update". */
    private void showItemOptionsDialog(DocumentSnapshot doc) {
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle(safe(doc.getString("name")))
                .setItems(new String[]{"Edit / Update"}, (dialogInterface, which) -> {
                    if (which == 0) {
                        showEditForm(doc);
                    }
                })
                .show();
    }

    // Ordered list of every editable field: {firestoreKey, displayLabel}.
    // punchingNo is intentionally excluded from this editable list (see below) —
    // it's shown separately as read-only since it's also the Firestore document ID.
    private static final String[][] EDITABLE_FIELDS = {
            {"name", "Name"},
            {"fatherName", "Father's Name"},
            {"designation", "Designation"},
            {"department", "Department"},
            {"doj", "Date of Joining"},
            {"pfNo", "PF No."},
            {"esiNo", "ESI No."},
            {"uanNo", "UAN No."},
            {"totalDays", "Total Payable Days"},
            {"workingDays", "No. of Days Worked"},
            {"wo", "Weekly Off"},
            {"holiday", "Holiday"},
            {"otHours", "OT Hours"},
            {"basic", "Basic"},
            {"hra", "HRA"},
            {"conveyance", "Conveyance"},
            {"specialAllowance", "Special Allowance"},
            {"cl", "CL"},
            {"pl", "PL"},
            {"bonus", "Bonus"},
            {"grossRate", "Gross"},
            {"basicEarned", "Basic Earned"},
            {"hraEarned", "HRA Earned"},
            {"conveyanceEarned", "Conveyance Earned"},
            {"specialAllowanceEarned", "Special Allowance Earned"},
            {"clEarned", "CL Earned"},
            {"plEarned", "PL Earned"},
            {"bonusEarned", "Bonus Earned"},
            {"otEarned", "OT Earned"},
            {"totalEarning", "Total Earning"},
            {"pfDeduction", "PF Deduction"},
            {"esiDeduction", "ESI Deduction"},
            {"otDeduction", "OT Deduction"},
            {"advanceDeduction", "Advance Deduction"},
            {"tea", "Tea"},
            {"canteen", "Canteen"},
            {"totalDeduction", "Total Deduction"},
            {"netSalary", "Net Salary (Amount)"},
            {"netSalaryWords", "Net Salary (Words)"},
            {"bankName", "Bank Name"},
            {"accountNo", "Account No."},
    };

    // Fields that should default to "0" when left blank on Add.
    // Everything else in EDITABLE_FIELDS defaults to "n/a" when left blank.
    private static final java.util.Set<String> NUMERIC_FIELDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "totalDays", "workingDays", "wo", "holiday", "otHours",
            "basic", "hra", "conveyance", "specialAllowance", "cl", "pl", "bonus", "grossRate",
            "basicEarned", "hraEarned", "conveyanceEarned", "specialAllowanceEarned",
            "clEarned", "plEarned", "bonusEarned", "otEarned", "totalEarning",
            "pfDeduction", "esiDeduction", "otDeduction", "advanceDeduction", "tea", "canteen",
            "totalDeduction", "netSalary"
    ));

    /**
     * Builds a blank form to add a brand-new worker. Name, Father's Name,
     * and Punching No. are mandatory (Punching No. doubles as the Firestore
     * document ID, so it can't be left blank or defaulted). Every other
     * field is optional — left blank, it's auto-filled with "0" for numeric
     * fields or "n/a" for text fields when saved.
     */
    private void showAddForm() {

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout formLayout = new LinearLayout(this);
        formLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        formLayout.setPadding(pad, pad, pad, pad);
        scrollView.addView(formLayout);

        android.widget.TextView noteView = new android.widget.TextView(this);
        noteView.setText("Fields marked * are required. Everything else, if left blank, " +
                "will be saved as \"n/a\" (text) or \"0\" (numeric).");
        noteView.setTextColor(0xFFA0A0A0);
        noteView.setTextSize(12f);
        noteView.setPadding(0, 0, 0, dpToPx(12));
        formLayout.addView(noteView);

        // Punching No. — mandatory here since it's the Firestore document ID.
        android.widget.EditText punchingInput = addFormField(formLayout, "Punching No. *", "");

        Map<String, android.widget.EditText> fieldMap = new HashMap<>();
        for (String[] field : EDITABLE_FIELDS) {
            String key = field[0];
            String label = field[1];
            boolean mandatory = key.equals("name") || key.equals("fatherName");
            android.widget.EditText input = addFormField(formLayout, mandatory ? label + " *" : label, "");
            fieldMap.put(key, input);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Add New Worker")
                .setView(scrollView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button addButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            addButton.setOnClickListener(v -> saveNewEmployee(punchingInput, fieldMap, dialog));
        });

        dialog.show();
    }

    /** Small helper: adds a label + EditText pair to a form, returns the EditText. */
    private android.widget.EditText addFormField(LinearLayout formLayout, String label, String initialValue) {
        android.widget.TextView labelView = new android.widget.TextView(this);
        labelView.setText(label);
        labelView.setTextColor(0xFF808080);
        labelView.setTextSize(12f);
        labelView.setPadding(0, dpToPx(8), 0, dpToPx(2));
        formLayout.addView(labelView);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(initialValue);
        input.setTextColor(0xFFFFFFFF);
        input.setSingleLine(true);
        input.setBackgroundColor(0xFF2A2A2A);
        input.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        formLayout.addView(input);

        return input;
    }

    /**
     * Validates mandatory fields (Punching No., Name, Father's Name),
     * fills in defaults for everything else left blank, then writes the
     * new employee document to Firestore.
     */
    private void saveNewEmployee(android.widget.EditText punchingInput,
                                 Map<String, android.widget.EditText> fieldMap,
                                 androidx.appcompat.app.AlertDialog dialog) {

        String punchingNo = punchingInput.getText().toString().trim();
        String name = fieldMap.get("name").getText().toString().trim();
        String fatherName = fieldMap.get("fatherName").getText().toString().trim();

        if (punchingNo.isEmpty()) {
            Toast.makeText(this, "Punching No. is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fatherName.isEmpty()) {
            Toast.makeText(this, "Father's Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> employee = new HashMap<>();
        employee.put("sn", "0");
        employee.put("punchingNo", punchingNo);

        for (Map.Entry<String, android.widget.EditText> entry : fieldMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getText().toString().trim();

            if (value.isEmpty()) {
                value = NUMERIC_FIELDS.contains(key) ? "0" : "n/a";
            }
            employee.put(key, value);
        }

        employee.put("nameSearch", name.toLowerCase());
        employee.put("fatherNameSearch", fatherName.toLowerCase());

        DocumentReferenceCheckAndSave(punchingNo, employee, dialog);
    }

    /** Checks whether this Punching No. already exists before writing, to avoid silently overwriting an existing worker. */
    private void DocumentReferenceCheckAndSave(String punchingNo, Map<String, Object> employee,
                                               androidx.appcompat.app.AlertDialog dialog) {

        com.google.firebase.firestore.DocumentReference ref = firestore.collection("salary_data")
                .document(department)
                .collection("salary_months")
                .document(month)
                .collection("employees")
                .document(punchingNo);

        ref.get().addOnSuccessListener(existing -> {
            if (existing.exists()) {
                new androidx.appcompat.app.AlertDialog.Builder(this, R.style.DarkAlertDialog)
                        .setTitle("Punching No. already exists")
                        .setMessage("A worker with Punching No. \"" + punchingNo +
                                "\" already exists for this month. Overwrite it?")
                        .setPositiveButton("Overwrite", (d, w) -> writeNewEmployee(ref, employee, dialog))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                writeNewEmployee(ref, employee, dialog);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to check existing worker: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void writeNewEmployee(com.google.firebase.firestore.DocumentReference ref,
                                  Map<String, Object> employee,
                                  androidx.appcompat.app.AlertDialog dialog) {
        ref.set(employee)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Worker added successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadEmployees();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Builds a scrollable form pre-filled with the employee's current
     * values. Every field from EDITABLE_FIELDS gets a label + EditText;
     * punchingNo is shown separately as read-only since it's also the
     * Firestore document ID and can't be safely renamed from here.
     */
    private void showEditForm(DocumentSnapshot doc) {

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout formLayout = new LinearLayout(this);
        formLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        formLayout.setPadding(pad, pad, pad, pad);
        scrollView.addView(formLayout);

        // Read-only punching number notice
        android.widget.TextView punchingLabel = new android.widget.TextView(this);
        punchingLabel.setText("Punching No. (read-only): " + safe(doc.getString("punchingNo")));
        punchingLabel.setTextColor(0xFFA0A0A0);
        punchingLabel.setTextSize(13f);
        punchingLabel.setPadding(0, 0, 0, dpToPx(12));
        formLayout.addView(punchingLabel);

        Map<String, android.widget.EditText> fieldMap = new HashMap<>();

        for (String[] field : EDITABLE_FIELDS) {
            String key = field[0];
            String label = field[1];
            String currentValue = safe(doc.getString(key));

            android.widget.TextView labelView = new android.widget.TextView(this);
            labelView.setText(label);
            labelView.setTextColor(0xFF808080);
            labelView.setTextSize(12f);
            labelView.setPadding(0, dpToPx(8), 0, dpToPx(2));
            formLayout.addView(labelView);

            android.widget.EditText input = new android.widget.EditText(this);
            input.setText(currentValue);
            input.setTextColor(0xFFFFFFFF);
            input.setSingleLine(true);
            input.setBackgroundColor(0xFF2A2A2A);
            input.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
            formLayout.addView(input);

            fieldMap.put(key, input);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Edit Worker Details")
                .setView(scrollView)
                .setPositiveButton("Save", null) // set below so we can validate before dismissing
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button saveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> saveEmployeeUpdates(doc, fieldMap, dialog));
        });

        dialog.show();
    }

    /** Reads every field's current text, writes the update to Firestore, then refreshes the list. */
    private void saveEmployeeUpdates(DocumentSnapshot doc,
                                     Map<String, android.widget.EditText> fieldMap,
                                     androidx.appcompat.app.AlertDialog dialog) {

        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, android.widget.EditText> entry : fieldMap.entrySet()) {
            updates.put(entry.getKey(), entry.getValue().getText().toString().trim());
        }

        // Keep the normalized search fields in sync if name/fatherName changed,
        // since WageSlipActivity matches workers using these fields.
        String updatedName = (String) updates.get("name");
        String updatedFatherName = (String) updates.get("fatherName");
        updates.put("nameSearch", updatedName != null ? updatedName.trim().toLowerCase() : "");
        updates.put("fatherNameSearch", updatedFatherName != null ? updatedFatherName.trim().toLowerCase() : "");

        firestore.collection("salary_data")
                .document(department)
                .collection("salary_months")
                .document(month)
                .collection("employees")
                .document(doc.getId())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Worker details updated", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadEmployees(); // refresh the list so the display text reflects any name change
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Builds the same SalaryData mapping used in WageSlipDetailActivity,
     * generates the PDF into cache, and opens it immediately via the
     * system PDF viewer. No download/share step here by design.
     */
    private void generateAndOpenPdf(DocumentSnapshot doc) {
        try {
            SalarySlipPdfGenerator.SalaryData data = new SalarySlipPdfGenerator.SalaryData();

            String[] parts = month.split("_");
            if (parts.length == 2) {
                data.mon1 = parts[0];
                data.mon2 = getNextMonthName(parts[0]);
                data.year = parts[1];
            }

            data.name = safe(doc.getString("name"));
            data.fatherName = safe(doc.getString("fatherName"));
            data.designation = safe(doc.getString("designation"));
            data.department = safe(doc.getString("department"));
            data.doj = safe(doc.getString("doj"));

            data.punchingNo = safe(doc.getString("punchingNo"));
            data.pfNo = safe(doc.getString("pfNo"));
            data.esiNo = safe(doc.getString("esiNo"));
            data.uanNo = safe(doc.getString("uanNo"));
            data.tpd = safe(doc.getString("totalDays"));
            data.nodw = safe(doc.getString("workingDays"));
            data.wO = safe(doc.getString("wo"));
            data.holiday = safe(doc.getString("holiday"));
            data.otH = safe(doc.getString("otHours"));
            data.basic = safe(doc.getString("basic"));
            data.hra = safe(doc.getString("hra"));
            data.convenience = safe(doc.getString("conveyance"));
            data.cl = safe(doc.getString("cl"));
            data.pl = safe(doc.getString("pl"));
            data.bonus = safe(doc.getString("bonus"));
            data.gross = safe(doc.getString("grossRate"));
            data.basicE = safe(doc.getString("basicEarned"));
            data.hraE = safe(doc.getString("hraEarned"));
            data.convenieceE = safe(doc.getString("conveyanceEarned"));
            data.otE = safe(doc.getString("otEarned"));
            data.clE = safe(doc.getString("clEarned"));
            data.plE = safe(doc.getString("plEarned"));
            data.bonusE = safe(doc.getString("bonusEarned"));
            data.totalEarning = safe(doc.getString("totalEarning"));
            data.pfD = safe(doc.getString("pfDeduction"));
            data.esiD = safe(doc.getString("esiDeduction"));
            data.oteD = safe(doc.getString("otDeduction"));
            data.advanceD = safe(doc.getString("advanceDeduction"));
            data.tea = safe(doc.getString("tea"));
            data.canteen = safe(doc.getString("canteen"));
            data.totalDeduction = safe(doc.getString("totalDeduction"));
            data.netSalary = safe(doc.getString("netSalary"));
            data.netSalaryWords = safe(doc.getString("netSalaryWords"));

            data.bankName = safe(doc.getString("bankName"));
            data.accountNo = safe(doc.getString("accountNo"));

            String workerId = doc.getId();
            File pdfFile = new File(getCacheDir(), "admin_" + workerId + "_" + month + ".pdf");

            SalarySlipPdfGenerator generator = new SalarySlipPdfGenerator(this);
            generator.generate(pdfFile, data);

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "View Wage Slip"));
            } else {
                Toast.makeText(this, "No PDF Viewer installed", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            android.util.Log.e("AdminEmployeeList", "generateAndOpenPdf failed", e);
            String msg = e.getMessage();
            if (msg == null || msg.trim().isEmpty()) {
                msg = e.getClass().getSimpleName() + " while generating PDF";
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private static final String[] MONTHS = {
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    };

    private String getNextMonthName(String currentMonth) {
        if (currentMonth == null) return "";
        String cm = currentMonth.trim().toUpperCase();

        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equals(cm)) {
                return MONTHS[(i + 1) % 12];
            }
        }
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].startsWith(cm) || cm.startsWith(MONTHS[i].substring(0, 3))) {
                return MONTHS[(i + 1) % 12];
            }
        }
        return cm;
    }

    private void showEmpty() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
}