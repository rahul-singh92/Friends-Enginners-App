package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;
import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class UploadPdfBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "UploadPdfBottomSheet";
    private static final int PICK_EXCEL_FILE = 1002;
    private ProgressDialog progressDialog;

    private Spinner spinnerBranch, spinnerYear, spinnerMonth;
    private TextView btnSelectExcel, btnSubmit, cancelButton;
    private TextView selectedExcelFileName;
    private ImageView closeButton;
    private Uri selectedExcelUri;

    private FirebaseFirestore firestore;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                bottomSheet.setBackground(null);
                BottomSheetBehavior<FrameLayout> behavior =
                        BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(false);
                behavior.setSkipCollapsed(true);
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.bottom_sheet_pdf_upload, container, false);

        spinnerBranch = view.findViewById(R.id.spinner_branch_pdf);
        spinnerYear   = view.findViewById(R.id.spinner_year_pdf);
        spinnerMonth  = view.findViewById(R.id.spinner_month_pdf);
        btnSelectExcel       = view.findViewById(R.id.btn_select_excel);
        btnSubmit            = view.findViewById(R.id.btn_submit_pdf);
        cancelButton         = view.findViewById(R.id.cancelButton);
        closeButton          = view.findViewById(R.id.closeButton);
        selectedExcelFileName = view.findViewById(R.id.selectedExcelFileName);

        firestore = FirebaseFirestore.getInstance();

        setupSpinners();

        closeButton.setOnClickListener(v -> dismiss());
        cancelButton.setOnClickListener(v -> dismiss());

        btnSelectExcel.setOnClickListener(v -> openFilePicker(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                PICK_EXCEL_FILE));

        btnSubmit.setOnClickListener(v -> importExcelAndGeneratePdf());

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> branchAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.branch_array, R.layout.spinner_item_dark);
        branchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerBranch.setAdapter(branchAdapter);

        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.years_array, R.layout.spinner_item_dark);
        yearAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sheet_month, R.layout.spinner_item_dark);
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerMonth.setAdapter(monthAdapter);
    }

    private void openFilePicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        startActivityForResult(
                Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_EXCEL_FILE) {
                selectedExcelUri = data.getData();
                selectedExcelFileName.setText("Excel selected");
                selectedExcelFileName.setTextColor(0xFFFFFFFF);
                Toast.makeText(getContext(), "Excel Selected!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =========================================================
// MAIN: Read Excel → Fill SalaryData → Generate PDF
// Runs the heavy parsing on a background thread so the
// ProgressDialog actually updates on screen row-by-row,
// instead of jumping straight to the final message.
// =========================================================
    private void importExcelAndGeneratePdf() {

        if (selectedExcelUri == null) {
            Toast.makeText(getContext(),
                    "Please select an Excel file",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Importing Employees");
        progressDialog.setMessage("Starting...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Handler tied to the main thread, used to push UI updates
        // from the background worker thread.
        android.os.Handler mainHandler =
                new android.os.Handler(android.os.Looper.getMainLooper());

        new Thread(() -> {
            try {
                InputStream inputStream =
                        getContext().getContentResolver()
                                .openInputStream(selectedExcelUri);

                Workbook workbook = WorkbookFactory.create(inputStream);
                FormulaEvaluator evaluator =
                        workbook.getCreationHelper().createFormulaEvaluator();
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                int lastRowNum = sheet.getLastRowNum();
                Log.d(TAG, "Total Rows = " + lastRowNum);

                int headerRowIndex = -1;

                // ---- Find Header Row ----
                for (int i = 0; i <= lastRowNum; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String firstCell = safeCell(formatter, evaluator, row, 0);

                    if ("SN".equalsIgnoreCase(firstCell.trim())) {
                        headerRowIndex = i;
                        Log.d(TAG, "Header Row = " + i);
                        break;
                    }
                }

                if (headerRowIndex == -1) {
                    workbook.close();
                    inputStream.close();
                    mainHandler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(),
                                "SN Header Row Not Found", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // ---- Collection Name ----
                String branch = spinnerBranch.getSelectedItem().toString()
                        .trim().replaceAll("\\s+", "_");

                String month = spinnerMonth.getSelectedItem()
                        .toString()
                        .trim()
                        .toUpperCase();

                String year = spinnerYear.getSelectedItem()
                        .toString()
                        .trim();

                String monthDocument = month + "_" + year;

                // IMPORTANT: explicitly create the branch-level document.
                // Without this, "salary_data/{branch}" only exists implicitly
                // (as a path to the subcollection below) and will NOT show up
                // when querying firestore.collection("salary_data").get() —
                // which is exactly what WageSlipActivity relies on to list
                // branches for the worker. This is what caused "No Wage Slip
                // Present" to show even after a successful admin upload.
                Map<String, Object> branchMetadata = new HashMap<>();
                branchMetadata.put("branchName", branch);
                firestore.collection("salary_data")
                        .document(branch)
                        .set(branchMetadata, com.google.firebase.firestore.SetOptions.merge());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("branch", branch);
                metadata.put("month", month);
                metadata.put("year", year);
                metadata.put("uploadedOn", FieldValue.serverTimestamp());

                firestore.collection("salary_data")
                        .document(branch)
                        .collection("salary_months")
                        .document(monthDocument)
                        .set(metadata);

                // ---- Count total employees first (for progress denominator) ----
                int totalEmployees = 0;
                for (int rowIndex = headerRowIndex + 1;
                     rowIndex <= lastRowNum;
                     rowIndex++) {

                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    String punchingNo = safeCell(formatter, evaluator, row, 1);
                    if (!punchingNo.isEmpty()) totalEmployees++;
                }

                final int totalEmployeesFinal = totalEmployees;

                // ---- Read all Employee Rows ----
                WriteBatch batch = firestore.batch();
                int totalImported = 0;

                for (int rowIndex = headerRowIndex + 1;
                     rowIndex <= lastRowNum;
                     rowIndex++) {

                    Row dataRow = sheet.getRow(rowIndex);
                    if (dataRow == null) continue;

                    String punchingNo = safeCell(formatter, evaluator, dataRow, 1);
                    if (punchingNo.isEmpty()) continue;

                    Map<String, Object> employee = new HashMap<>();

                    employee.put("sn", safeCell(formatter, evaluator, dataRow, 0));
                    employee.put("punchingNo", safeCell(formatter, evaluator, dataRow, 1));
                    employee.put("department", safeCell(formatter, evaluator, dataRow, 2));
                    employee.put("name", safeCell(formatter, evaluator, dataRow, 3));
                    employee.put("fatherName", safeCell(formatter, evaluator, dataRow, 4));

                    // Normalized fields used for reliable lookup from the worker
                    // side (credentials_worker has no relation to punchingNo,
                    // so we match on name + fatherName instead). Trimmed and
                    // lowercased so casing/whitespace differences don't break it.
                    employee.put("nameSearch",
                            safeCell(formatter, evaluator, dataRow, 3).trim().toLowerCase());
                    employee.put("fatherNameSearch",
                            safeCell(formatter, evaluator, dataRow, 4).trim().toLowerCase());

                    employee.put("doj", safeCell(formatter, evaluator, dataRow, 5));
                    employee.put("designation", safeCell(formatter, evaluator, dataRow, 6));
                    employee.put("pfNo", safeCell(formatter, evaluator, dataRow, 7));
                    employee.put("esiNo", safeCell(formatter, evaluator, dataRow, 8));
                    employee.put("uanNo", safeCell(formatter, evaluator, dataRow, 9));

                    employee.put("basic", safeCell(formatter, evaluator, dataRow, 10));
                    employee.put("hra", safeCell(formatter, evaluator, dataRow, 11));
                    employee.put("conveyance", safeCell(formatter, evaluator, dataRow, 12));
                    employee.put("specialAllowance", safeCell(formatter, evaluator, dataRow, 13));
                    employee.put("cl", safeCell(formatter, evaluator, dataRow, 14));
                    employee.put("pl", safeCell(formatter, evaluator, dataRow, 15));
                    employee.put("bonus", safeCell(formatter, evaluator, dataRow, 16));
                    employee.put("grossRate", safeCell(formatter, evaluator, dataRow, 17));
                    employee.put("daysInMonth", safeCell(formatter, evaluator, dataRow, 18));
                    employee.put("workingDays", safeCell(formatter, evaluator, dataRow, 19));
                    employee.put("wo", safeCell(formatter, evaluator, dataRow, 20));
                    employee.put("holiday", safeCell(formatter, evaluator, dataRow, 21));
                    employee.put("totalDays", safeCell(formatter, evaluator, dataRow, 22));
                    employee.put("otHours", safeCell(formatter, evaluator, dataRow, 23));

                    employee.put("basicEarned", safeCell(formatter, evaluator, dataRow, 24));
                    employee.put("hraEarned", safeCell(formatter, evaluator, dataRow, 25));
                    employee.put("conveyanceEarned", safeCell(formatter, evaluator, dataRow, 26));
                    employee.put("specialAllowanceEarned", safeCell(formatter, evaluator, dataRow, 27));
                    employee.put("clEarned", safeCell(formatter, evaluator, dataRow, 28));
                    employee.put("plEarned", safeCell(formatter, evaluator, dataRow, 29));
                    employee.put("bonusEarned", safeCell(formatter, evaluator, dataRow, 30));
                    employee.put("otEarned", safeCell(formatter, evaluator, dataRow, 31));
                    employee.put("totalEarning", safeCell(formatter, evaluator, dataRow, 32));

                    employee.put("pfDeduction", safeCell(formatter, evaluator, dataRow, 33));
                    employee.put("esiDeduction", safeCell(formatter, evaluator, dataRow, 34));
                    employee.put("otDeduction", safeCell(formatter, evaluator, dataRow, 35));
                    employee.put("advanceDeduction", safeCell(formatter, evaluator, dataRow, 36));
                    employee.put("canteen", safeCell(formatter, evaluator, dataRow, 37));
                    employee.put("tea", safeCell(formatter, evaluator, dataRow, 38));
                    employee.put("totalDeduction", safeCell(formatter, evaluator, dataRow, 39));
                    String netSalary = safeCell(formatter, evaluator, dataRow, 40);

                    employee.put("netSalary", netSalary);

                    try {
                        double salary = Double.parseDouble(netSalary.replace(",", ""));
                        employee.put("netSalaryWords", NumberToWords.convert(salary));
                    } catch (Exception e) {
                        employee.put("netSalaryWords", "");
                    }
                    employee.put("bankName", safeCell(formatter, evaluator, dataRow, 42));
                    employee.put("accountNo", safeCell(formatter, evaluator, dataRow, 43));

                    DocumentReference employeeRef =
                            firestore.collection("salary_data")
                                    .document(branch)
                                    .collection("salary_months")
                                    .document(monthDocument)
                                    .collection("employees")
                                    .document(punchingNo);

                    batch.set(employeeRef, employee);
                    totalImported++;

                    final int importedSoFar = totalImported;

                    // Push progress update to the main thread so it's
                    // actually rendered before the loop continues.
                    mainHandler.post(() -> progressDialog.setMessage(
                            "Preparing " + importedSoFar + " / " + totalEmployeesFinal + " employees..."
                    ));

                    // Small delay so each update is visibly rendered.
                    // Safe here since we're on a background thread, not the UI thread.
                    try {
                        Thread.sleep(15);
                    } catch (InterruptedException ignored) {
                    }
                }

                workbook.close();
                inputStream.close();

                // Switch back to main thread to show "uploading" state and commit.
                mainHandler.post(() -> progressDialog.setMessage("Uploading to server..."));

                batch.commit()
                        .addOnSuccessListener(unused -> mainHandler.post(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(
                                    getContext(),
                                    "Employees Imported Successfully",
                                    Toast.LENGTH_LONG
                            ).show();
                            dismiss();
                        }))
                        .addOnFailureListener(e -> mainHandler.post(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(
                                    getContext(),
                                    "Upload Failed : " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }));

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(),
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    // =========================================================
    // Safe cell reader — never throws, never returns null
    // =========================================================
    private String safeCell(DataFormatter formatter,
                            FormulaEvaluator evaluator,
                            Row row, int col) {
        try {
            return formatter.formatCellValue(
                    row.getCell(col), evaluator).trim();
        } catch (Exception e) {
            try {
                return formatter.formatCellValue(
                        row.getCell(col)).trim();
            } catch (Exception ex) {
                return "";
            }
        }
    }
}