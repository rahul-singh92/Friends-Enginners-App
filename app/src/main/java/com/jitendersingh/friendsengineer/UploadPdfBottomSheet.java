package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UploadPdfBottomSheet extends BottomSheetDialogFragment {

    private static final int PICK_PDF_FILE = 1001;
    private static final int PICK_EXCEL_FILE = 1002;

    private Spinner spinnerBranch, spinnerYear, spinnerMonth;
    private TextView btnSelectPdf, btnSelectExcel, btnSubmit, cancelButton;
    private TextView selectedPdfFileName, selectedExcelFileName;
    private ImageView closeButton;
    private Uri selectedPdfUri;
    private Uri selectedExcelUri;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore firestore;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(false);
                behavior.setSkipCollapsed(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_pdf_upload, container, false);

        // Initialize views
        spinnerBranch = view.findViewById(R.id.spinner_branch_pdf);
        spinnerYear = view.findViewById(R.id.spinner_year_pdf);
        spinnerMonth = view.findViewById(R.id.spinner_month_pdf);
        btnSelectPdf = view.findViewById(R.id.btn_select_pdf);
        btnSelectExcel = view.findViewById(R.id.btn_select_excel);
        btnSubmit = view.findViewById(R.id.btn_submit_pdf);
        cancelButton = view.findViewById(R.id.cancelButton);
        closeButton = view.findViewById(R.id.closeButton);
        selectedPdfFileName = view.findViewById(R.id.selectedPdfFileName);
        selectedExcelFileName = view.findViewById(R.id.selectedExcelFileName);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        firestore = FirebaseFirestore.getInstance();

        // Setup dark theme spinners
        setupSpinners();

        // Close button
        closeButton.setOnClickListener(v -> dismiss());

        // Cancel button
        cancelButton.setOnClickListener(v -> dismiss());

        // Select PDF button
        btnSelectPdf.setOnClickListener(v -> openFilePicker("application/pdf", PICK_PDF_FILE));

        // Select Excel button
        btnSelectExcel.setOnClickListener(v -> openFilePicker(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", PICK_EXCEL_FILE));

        // Submit button
        btnSubmit.setOnClickListener(v -> uploadFilesAndSaveData());

        return view;
    }

    private void setupSpinners() {
        // Create custom adapters with dark theme
        ArrayAdapter<CharSequence> branchAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.branch_array,
                R.layout.spinner_item_dark
        );
        branchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerBranch.setAdapter(branchAdapter);

        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.years_array,
                R.layout.spinner_item_dark
        );
        yearAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.sheet_month,
                R.layout.spinner_item_dark
        );
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerMonth.setAdapter(monthAdapter);
    }

    private void openFilePicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (requestCode == PICK_PDF_FILE) {
                selectedPdfUri = selectedUri;
                selectedPdfFileName.setText("PDF selected");
                selectedPdfFileName.setTextColor(0xFFFFFFFF); // White
                Toast.makeText(getContext(), "PDF Selected!", Toast.LENGTH_SHORT).show();
            } else if (requestCode == PICK_EXCEL_FILE) {
                selectedExcelUri = selectedUri;
                selectedExcelFileName.setText("Excel selected");
                selectedExcelFileName.setTextColor(0xFFFFFFFF); // White
                Toast.makeText(getContext(), "Excel Selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadFilesAndSaveData() {
        if (selectedPdfUri == null) {
            Toast.makeText(getContext(), "Please select a PDF file", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedExcelUri == null) {
            Toast.makeText(getContext(), "Please select an Excel file", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setTextColor(0xFF606060); // Gray when disabled

        String branch = spinnerBranch.getSelectedItem().toString().trim().replaceAll("\\s+", "_");
        String year = spinnerYear.getSelectedItem().toString().trim();
        String month = spinnerMonth.getSelectedItem().toString().trim();

        String pdfFileName = "wage_slip_" + branch + "_" + month + "_" + year + ".pdf";
        String excelFileName = "wage_" + branch + "_" + month + "_" + year + ".xlsx";

        StorageReference pdfRef = storageRef.child("wage_slips/" + pdfFileName);
        StorageReference excelRef = storageRef.child("wage_excels/" + excelFileName);

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(getContext(), R.style.DarkAlertDialog);
        progressDialog.setMessage("Uploading files...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Upload PDF first
        pdfRef.putFile(selectedPdfUri)
                .addOnSuccessListener(taskSnapshot -> pdfRef.getDownloadUrl()
                        .addOnSuccessListener(pdfDownloadUri -> {
                            // PDF uploaded & URL obtained
                            // Now upload Excel
                            excelRef.putFile(selectedExcelUri)
                                    .addOnSuccessListener(taskSnapshot1 -> {
                                        // Excel uploaded - now parse and save data to Firestore
                                        parseExcelAndSaveData(branch, month, year, pdfDownloadUri.toString(), progressDialog);
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        btnSubmit.setEnabled(true);
                                        btnSubmit.setTextColor(0xFF2196F3);
                                        Toast.makeText(getContext(), "Excel upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setTextColor(0xFF2196F3);
                    Toast.makeText(getContext(), "PDF upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void parseExcelAndSaveData(String branch, String month, String year, String pdfUrl, ProgressDialog progressDialog) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedExcelUri);
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            String collectionName = "wage_" + branch + "_" + month + "_" + year;

            // Save PDF URL in special metadata document
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pdfUrl", pdfUrl);
            firestore.collection(collectionName).document("_metadata").set(metadata);

            // Also save collection name in "wage_collections"
            Map<String, Object> collectionEntry = new HashMap<>();
            collectionEntry.put("name", collectionName);
            firestore.collection("wage_collections").document(collectionName).set(collectionEntry);

            DataFormatter formatter = new DataFormatter();

            int lastRowNum = sheet.getLastRowNum();
            final int[] savedCount = {0};

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    savedCount[0]++;
                    if (savedCount[0] == lastRowNum) {
                        workbookCloseAndFinish(workbook, inputStream, progressDialog);
                    }
                    continue;
                }

                String id = formatter.formatCellValue(row.getCell(0));
                String name = formatter.formatCellValue(row.getCell(1));
                String fatherName = formatter.formatCellValue(row.getCell(2));

                if (!id.isEmpty() && !name.isEmpty()) {
                    Map<String, Object> personData = new HashMap<>();
                    personData.put("id", id);
                    personData.put("name", name);
                    personData.put("fatherName", fatherName);
                    personData.put("pdfPage", i);
                    personData.put("pdfUrl", pdfUrl);

                    firestore.collection(collectionName).document(id).set(personData)
                            .addOnCompleteListener(task -> {
                                savedCount[0]++;
                                if (savedCount[0] == lastRowNum) {
                                    workbookCloseAndFinish(workbook, inputStream, progressDialog);
                                }
                            });
                } else {
                    savedCount[0]++;
                    if (savedCount[0] == lastRowNum) {
                        workbookCloseAndFinish(workbook, inputStream, progressDialog);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            progressDialog.dismiss();
            btnSubmit.setEnabled(true);
            btnSubmit.setTextColor(0xFF2196F3);
            Toast.makeText(getContext(), "Error parsing Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void workbookCloseAndFinish(Workbook workbook, InputStream inputStream, ProgressDialog progressDialog) {
        try {
            workbook.close();
            inputStream.close();
        } catch (Exception ignored) {
        }
        progressDialog.dismiss();
        btnSubmit.setEnabled(true);
        btnSubmit.setTextColor(0xFF2196F3);
        Toast.makeText(getContext(), "Upload & data saved successfully!", Toast.LENGTH_LONG).show();
        dismiss();
    }
}
