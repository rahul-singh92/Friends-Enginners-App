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

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class UploadPdfBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "UploadPdfBottomSheet";
    private static final int PICK_EXCEL_FILE = 1002;

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

        btnSubmit.setOnClickListener(v -> readExcelAndGeneratePdf());

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
    // =========================================================
    private void readExcelAndGeneratePdf() {

        if (selectedExcelUri == null) {
            Toast.makeText(getContext(),
                    "Please select an Excel file",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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
            String mon1 = "", mon2 = "", yearText = "";

            // ---- Find Header Row ----
            for (int i = 0; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String firstCell = safeCell(formatter, evaluator, row, 0);

                if ("SN".equalsIgnoreCase(firstCell.trim())) {
                    headerRowIndex = i;
                    Log.d(TAG, "Header Row = " + i);

                    Row monthRow = sheet.getRow(i - 1);
                    if (monthRow != null) {
                        // Month text is at column 30: "21 NOV TO DEC 2024"
                        String monthText = safeCell(formatter, evaluator, monthRow, 30);
                        Log.d(TAG, "Month Text = " + monthText);

                        String[] parts = monthText.split(" ");
                        if (parts.length >= 5) {
                            mon1 = parts[1].substring(0, 3).toUpperCase();
                            mon2 = parts[3].substring(0, 3).toUpperCase();
                            yearText = parts[4];
                        }
                    }
                    break;
                }
            }

            if (headerRowIndex == -1) {
                Toast.makeText(getContext(),
                        "SN Header Row Not Found", Toast.LENGTH_LONG).show();
                workbook.close();
                inputStream.close();
                return;
            }

            // ---- Collection Name ----
            String branch = spinnerBranch.getSelectedItem().toString()
                    .trim().replaceAll("\\s+", "_");
            String collectionName = branch + "_" + mon2 + "_" + yearText;
            Log.d(TAG, "Collection = " + collectionName);

            // Save collection entry to Firestore
            Map<String, Object> colData = new HashMap<>();
            colData.put("name", collectionName);
            firestore.collection("wage_collections")
                    .document(collectionName)
                    .set(colData);

            // ---- Read First Employee Row ----
            // Update this to loop through all rows later
            Row dataRow = sheet.getRow(headerRowIndex + 1);

            if (dataRow == null) {
                Toast.makeText(getContext(),
                        "No employee data found", Toast.LENGTH_SHORT).show();
                workbook.close();
                inputStream.close();
                return;
            }

            // =========================================
            // MAP EXCEL COLUMNS → SalaryData
            // Adjust column indexes to match your Excel
            // =========================================
            SalarySlipPdfGenerator.SalaryData sd =
                    new SalarySlipPdfGenerator.SalaryData();

            sd.mon1 = mon1;
            sd.mon2 = mon2;
            sd.year = yearText;

            // Employee info — adjust col indexes to match your Excel headers
            sd.name        = safeCell(formatter, evaluator, dataRow, 3);
            sd.fatherName  = safeCell(formatter, evaluator, dataRow, 4);
//            sd.punchingNo  = safeCell(formatter, evaluator, dataRow, 1);
            sd.punchingNo = "105998";
            // TODO: fill in the rest once you confirm column indexes
            // sd.designation = safeCell(formatter, evaluator, dataRow, 5);
            // sd.department  = safeCell(formatter, evaluator, dataRow, 6);
            // sd.doj         = safeCell(formatter, evaluator, dataRow, 7);
            // sd.pfNo        = safeCell(formatter, evaluator, dataRow, 8);
            // sd.esiNo       = safeCell(formatter, evaluator, dataRow, 9);
            // sd.uanNo       = safeCell(formatter, evaluator, dataRow, 10);
            // sd.tpd         = safeCell(formatter, evaluator, dataRow, 11);
            // sd.nodw        = safeCell(formatter, evaluator, dataRow, 12);
            // sd.wO          = safeCell(formatter, evaluator, dataRow, 13);
            // sd.holiday     = safeCell(formatter, evaluator, dataRow, 14);
            // sd.otH         = safeCell(formatter, evaluator, dataRow, 15);
            // sd.basic       = safeCell(formatter, evaluator, dataRow, 16);
            // sd.hra         = safeCell(formatter, evaluator, dataRow, 17);
            // sd.convenience = safeCell(formatter, evaluator, dataRow, 18);
            // sd.cl          = safeCell(formatter, evaluator, dataRow, 19);
            // sd.pl          = safeCell(formatter, evaluator, dataRow, 20);
            // sd.bonus       = safeCell(formatter, evaluator, dataRow, 21);
            // sd.gross       = safeCell(formatter, evaluator, dataRow, 22);
            // sd.basicE      = safeCell(formatter, evaluator, dataRow, 23);
            // sd.hraE        = safeCell(formatter, evaluator, dataRow, 24);
            // sd.convenieceE = safeCell(formatter, evaluator, dataRow, 25);
            // sd.otE         = safeCell(formatter, evaluator, dataRow, 26);
            // sd.clE         = safeCell(formatter, evaluator, dataRow, 27);
            // sd.plE         = safeCell(formatter, evaluator, dataRow, 28);
            // sd.bonusE      = safeCell(formatter, evaluator, dataRow, 29);
            // sd.totalEarning= safeCell(formatter, evaluator, dataRow, 30);
            // sd.pfD         = safeCell(formatter, evaluator, dataRow, 31);
            // sd.esiD        = safeCell(formatter, evaluator, dataRow, 32);
            // sd.oteD        = safeCell(formatter, evaluator, dataRow, 33);
            // sd.advanceD    = safeCell(formatter, evaluator, dataRow, 34);
            // sd.tea         = safeCell(formatter, evaluator, dataRow, 35);
            // sd.canteen     = safeCell(formatter, evaluator, dataRow, 36);
            // sd.totalDeduction = safeCell(formatter, evaluator, dataRow, 37);
            // sd.netSalary   = safeCell(formatter, evaluator, dataRow, 38);
            // sd.netSalaryWords = safeCell(formatter, evaluator, dataRow, 39);
            // sd.bankName    = safeCell(formatter, evaluator, dataRow, 40);
            // sd.accountNo   = safeCell(formatter, evaluator, dataRow, 41);
            sd.designation = "OPERATOR";
            sd.department  = "PRODUCTION";
            sd.doj         = "01-01-2020";
            sd.pfNo        = "PF123456";
            sd.esiNo       = "ESI789012";
            sd.uanNo       = "UAN345678";
            sd.tpd         = "26";
            sd.nodw        = "24";
            sd.wO          = "4";
            sd.holiday     = "1";
            sd.otH         = "10";
            sd.basic       = "8000";
            sd.hra         = "2000";
            sd.convenience = "1000";
            sd.cl          = "500";
            sd.pl          = "500";
            sd.bonus       = "700";
            sd.gross       = "12700";
            sd.basicE      = "7384";
            sd.hraE        = "1846";
            sd.convenieceE = "923";
            sd.otE         = "500";
            sd.clE         = "0";
            sd.plE         = "0";
            sd.bonusE      = "646";
            sd.totalEarning = "11299";
            sd.pfD         = "1056";
            sd.esiD        = "95";
            sd.oteD        = "4";
            sd.advanceD    = "0";
            sd.tea         = "50";
            sd.canteen     = "100";
            sd.totalDeduction = "1305";
            sd.netSalary   = "9994";
            sd.netSalaryWords = "NINE THOUSAND NINE HUNDRED NINETY FOUR ONLY";
            sd.bankName    = "SBI BHIWADI";
            sd.accountNo   = "XXXX1234";

            workbook.close();
            inputStream.close();

            // ---- Generate PDF ----
            File outputFile = new File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS),
                    sd.punchingNo + "_salary.pdf");

            SalarySlipPdfGenerator generator =
                    new SalarySlipPdfGenerator(requireContext());
            generator.generate(outputFile, sd);

            Log.d(TAG, "PDF saved: " + outputFile.getAbsolutePath());

            // ---- Save to Firestore ----
            Map<String, Object> empData = new HashMap<>();
            empData.put("sn",        safeCell(formatter, evaluator, dataRow, 0));
            empData.put("punchingNo", sd.punchingNo);
            empData.put("name",       sd.name);
            empData.put("fatherName", sd.fatherName);
            empData.put("pdfPath",    outputFile.getAbsolutePath());
            empData.put("mon1",       mon1);
            empData.put("mon2",       mon2);
            empData.put("year",       yearText);

            firestore.collection(collectionName)
                    .document(sd.punchingNo)
                    .set(empData)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Firestore saved");
                        Toast.makeText(getContext(),
                                "PDF Generated: " + outputFile.getName(),
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Firestore save failed", e));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(),
                    "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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