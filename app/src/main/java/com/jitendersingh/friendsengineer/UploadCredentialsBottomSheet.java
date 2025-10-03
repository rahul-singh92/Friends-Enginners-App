package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class UploadCredentialsBottomSheet extends BottomSheetDialogFragment {

    private static final int FILE_SELECT_CODE = 100;
    private Uri selectedFileUri;
    private TextView btnSelectFile, btnSubmit, cancelButton, selectedFileName;
    private ImageView closeButton;
    private Spinner spinnerUserType;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_upload_credentials, container, false);

        // Initialize views
        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnSubmit = view.findViewById(R.id.btn_submit);
        cancelButton = view.findViewById(R.id.cancelButton);
        closeButton = view.findViewById(R.id.closeButton);
        selectedFileName = view.findViewById(R.id.selectedFileName);
        spinnerUserType = view.findViewById(R.id.spinner_user_type);

        // Setup dark theme spinner
        setupSpinner();

        // Close button
        closeButton.setOnClickListener(v -> dismiss());

        // Cancel button
        cancelButton.setOnClickListener(v -> dismiss());

        // Select file button
        btnSelectFile.setOnClickListener(v -> openFileChooser());

        // Upload button
        btnSubmit.setOnClickListener(v -> uploadFile());

        return view;
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.user_type_array,
                R.layout.spinner_item_dark
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        spinnerUserType.setAdapter(adapter);
    }

    private void uploadFile() {
        if (selectedFileUri != null) {
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext(), R.style.DarkAlertDialog);
            progressDialog.setMessage("Uploading to Firebase...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                try {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(selectedFileUri);
                    if (inputStream == null) throw new Exception("Unable to open file stream");

                    Workbook workbook = selectedFileUri.toString().endsWith(".xls")
                            ? new HSSFWorkbook(inputStream)
                            : new XSSFWorkbook(inputStream);
                    Sheet sheet = workbook.getSheetAt(0);
                    if (sheet == null) throw new Exception("Sheet is null");

                    List<String> columnNames = new ArrayList<>();
                    List<List<String>> extractedData = new ArrayList<>();

                    boolean isHeader = true;
                    for (Row row : sheet) {
                        if (isHeader) {
                            for (Cell cell : row) {
                                columnNames.add(cell.getStringCellValue().trim().replaceAll("[^a-zA-Z0-9_]", "_"));
                            }
                            isHeader = false;
                        } else {
                            List<String> rowData = new ArrayList<>();
                            for (int i = 0; i < columnNames.size(); i++) {
                                rowData.add(getCellValue(row.getCell(i)));
                            }
                            extractedData.add(rowData);
                        }
                    }

                    workbook.close();
                    inputStream.close();

                    String selectedSheetType = spinnerUserType.getSelectedItem().toString().toLowerCase(Locale.ROOT);
                    String collectionName = "credentials_" + selectedSheetType;

                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

                    for (List<String> row : extractedData) {
                        Map<String, Object> doc = new HashMap<>();
                        for (int i = 0; i < columnNames.size(); i++) {
                            doc.put(columnNames.get(i), row.get(i));
                        }

                        firestore.collection(collectionName)
                                .add(doc)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("FirestoreUpload", "Uploaded: " + documentReference.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreUpload", "Upload error", e);
                                });
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Uploaded to Firebase successfully", Toast.LENGTH_SHORT).show();
                            dismiss();
                        });
                    }

                } catch (Exception e) {
                    Log.e("ExcelUpload", "Error: ", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            }).start();
        } else {
            Toast.makeText(getContext(), "Please select a file first", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.ms-excel");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select Excel File"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                String fileName = getFileName(selectedFileUri);
                selectedFileName.setText(fileName);
                selectedFileName.setTextColor(0xFFFFFFFF); // White
            }
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "Selected File";
        if (getContext() != null && uri.getScheme() != null) {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                }
            } else if (uri.getScheme().equals("file")) {
                fileName = uri.getLastPathSegment();
            }
        }
        return fileName;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    return dateFormat.format(cell.getDateCellValue());
                } else {
                    return String.format(Locale.US, "%.0f", cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case STRING:
                            return cellValue.getStringValue();
                        case NUMERIC:
                            return String.format(Locale.US, "%.0f", cellValue.getNumberValue());
                        case BOOLEAN:
                            return String.valueOf(cellValue.getBooleanValue());
                        default:
                            return null;
                    }
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
}
