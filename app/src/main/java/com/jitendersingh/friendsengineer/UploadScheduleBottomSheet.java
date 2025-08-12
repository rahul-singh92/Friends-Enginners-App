package com.jitendersingh.friendsengineer;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UploadScheduleBottomSheet extends BottomSheetDialogFragment {

    private static final int FILE_SELECT_CODE = 1001;
    private Uri selectedPdfUri = null;

    private Button btnSelectFile, btnStartDate, btnEndDate, btnSubmit;
    private Spinner spinnerScheduleType;

    private String selectedStartDate = null;
    private String selectedEndDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.upload_schedule_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnStartDate = view.findViewById(R.id.btn_select_start_date);
        btnEndDate = view.findViewById(R.id.btn_select_end_date);
        btnSubmit = view.findViewById(R.id.btn_submit);
        spinnerScheduleType = view.findViewById(R.id.spinner_options);

        // Setup Spinner adapter with values from values/arrays.xml (make sure branch_array exists)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.branch_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScheduleType.setAdapter(adapter);

        btnSelectFile.setOnClickListener(v -> openPdfFileChooser());

        btnStartDate.setOnClickListener(v -> showDatePicker(true));

        btnEndDate.setOnClickListener(v -> showDatePicker(false));

        btnSubmit.setOnClickListener(v -> {
            if (selectedPdfUri == null) {
                Toast.makeText(getContext(), "Please select a PDF file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedStartDate == null) {
                Toast.makeText(getContext(), "Please select a start date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedEndDate == null) {
                Toast.makeText(getContext(), "Please select an end date", Toast.LENGTH_SHORT).show();
                return;
            }

            String spinnerValue = spinnerScheduleType.getSelectedItem().toString();

            uploadPdfAndSaveData(spinnerValue, selectedStartDate, selectedEndDate, selectedPdfUri);
        });
    }

    private void openPdfFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), FILE_SELECT_CODE);
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(getContext(),
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    String dateFormatted = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    if (isStartDate) {
                        selectedStartDate = dateFormatted;
                        btnStartDate.setText("📅 " + dateFormatted);
                    } else {
                        selectedEndDate = dateFormatted;
                        btnEndDate.setText("📅 " + dateFormatted);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && getActivity() != null && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                selectedPdfUri = data.getData();
                String fileName = getFileNameFromUri(selectedPdfUri);
                btnSelectFile.setText("📄 " + fileName);
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "selected_file.pdf";
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    result = path.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void uploadPdfAndSaveData(String spinnerValue, String startDate, String endDate, Uri pdfUri) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        String fileName = "schedule_" + spinnerValue.replaceAll("\\s+","") + "_" + System.currentTimeMillis() + ".pdf";
        StorageReference storageRef = storage.getReference().child("schedules").child(fileName);

        UploadTask uploadTask = storageRef.putFile(pdfUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                progressDialog.dismiss();
                String downloadUrl = uri.toString();
                saveDataToFirestore(spinnerValue, startDate, endDate, downloadUrl);
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressDialog.setMessage("Uploading PDF... " + (int) progress + "%");
        });
    }

    private void saveDataToFirestore(String spinnerValue, String startDate, String endDate, String pdfUrl) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("spinnerOption", spinnerValue);
        dataMap.put("startDate", startDate);
        dataMap.put("endDate", endDate);
        dataMap.put("pdfUrl", pdfUrl);
        dataMap.put("timestamp", System.currentTimeMillis());

        String docName = spinnerValue.replaceAll("\\s+","") + "_" + startDate.replace("/","") + "_" + endDate.replace("/","");

        firestore.collection("Schedules")
                .document(docName)
                .set(dataMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Schedule saved successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
