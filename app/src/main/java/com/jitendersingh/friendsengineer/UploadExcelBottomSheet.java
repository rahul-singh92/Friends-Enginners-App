package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.ProgressDialog;

public class UploadExcelBottomSheet extends BottomSheetDialogFragment {

    private static final int FILE_SELECT_CODE = 100;
    private Uri selectedFileUri;
    private Button btnSelectFile, btnUpload;
    private DatabaseHelper databaseHelper;
    private android.app.AlertDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_upload, container, false);

        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnUpload = view.findViewById(R.id.btn_submit);

        btnSelectFile.setOnClickListener(v -> openFileChooser());
        btnUpload.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                ProgressDialog progressDialog = new ProgressDialog(getContext());
                progressDialog.setMessage("Uploading...");
                progressDialog.setCancelable(false); // not cancellable by touching outside
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

                        Spinner spinnerSheetType = getView().findViewById(R.id.spinner_sheet_type);
                        Spinner spinnerYear = getView().findViewById(R.id.spinner_year);
                        Spinner spinnerMonth = getView().findViewById(R.id.spinner_month);
                        String selectedSheetType = spinnerSheetType.getSelectedItem().toString();
                        String selectedYear = spinnerYear.getSelectedItem().toString();
                        String selectedMonth = spinnerMonth.getSelectedItem().toString();

                        databaseHelper.createTableIfNotExists(columnNames, selectedSheetType, selectedYear, selectedMonth);
                        databaseHelper.insertDynamicData(columnNames, extractedData);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Data inserted successfully", Toast.LENGTH_SHORT).show();
                                dismiss(); // dismiss bottom sheet after upload
                            });
                        }
                    } catch (Exception e) {
                        Log.e("ExcelDebug", "Error processing file: ", e);
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
        });


        databaseHelper = new DatabaseHelper(getContext());

        return view;
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
                btnSelectFile.setText(fileName + " selected");
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