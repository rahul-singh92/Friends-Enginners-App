package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WorkerDetailBottomSheet extends BottomSheetDialogFragment {

    private static final int IMAGE_PICK_REQUEST = 100;
    private ImageView imageView;
    private Uri imageUri;

    private EditText doj, dol;
    private EditText pNo, esiNo, uanNo, pfNo, name, fatherName, contact, department;
    private TextView submitButton, cancelButton, uploadImageButton;
    private ImageView closeButton;

    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

                // Set the bottom sheet to expanded state
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Disable dragging to dismiss
                behavior.setDraggable(false);

                // Set peek height to full screen
                behavior.setPeekHeight(bottomSheet.getHeight());

                // Skip collapsed state
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

        View view = inflater.inflate(R.layout.bottom_sheet_worker_detail, container, false);

        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        imageView = view.findViewById(R.id.worker_image_view);
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        closeButton = view.findViewById(R.id.closeButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        submitButton = view.findViewById(R.id.submit_worker_button);

        pNo = view.findViewById(R.id.p_no);
        esiNo = view.findViewById(R.id.esi_no);
        uanNo = view.findViewById(R.id.uan_no);
        pfNo = view.findViewById(R.id.pf_no);
        name = view.findViewById(R.id.name);
        fatherName = view.findViewById(R.id.father_name);
        contact = view.findViewById(R.id.contact_number);
        doj = view.findViewById(R.id.date_of_joining);
        department = view.findViewById(R.id.department);
        dol = view.findViewById(R.id.date_of_leave);

        // Upload image button
        uploadImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_REQUEST);
        });

        // Close button
        closeButton.setOnClickListener(v -> dismiss());

        // Cancel button
        cancelButton.setOnClickListener(v -> dismiss());

        // Setup date pickers
        setupDatePicker(doj);
        setupDatePicker(dol);

        // Submit button
        submitButton.setOnClickListener(v -> submitWorkerDetails());

        return view;
    }

    private void submitWorkerDetails() {
        String pNoVal = pNo.getText().toString().trim();
        String esiVal = esiNo.getText().toString().trim();
        String uanVal = uanNo.getText().toString().trim();
        String pfVal = pfNo.getText().toString().trim();
        String nameVal = name.getText().toString().trim();
        String fatherVal = fatherName.getText().toString().trim();
        String contactVal = contact.getText().toString().trim();
        String dojVal = doj.getText().toString().trim();
        String deptVal = department.getText().toString().trim();
        String dolVal = dol.getText().toString().trim();

        // Validation
        if (nameVal.isEmpty()) {
            name.setError("Name is required");
            name.requestFocus();
            return;
        }

        if (fatherVal.isEmpty()) {
            fatherName.setError("Father's name is required");
            fatherName.requestFocus();
            return;
        }

        if (contactVal.isEmpty()) {
            contact.setError("Contact number is required");
            contact.requestFocus();
            return;
        }

        if (dojVal.isEmpty()) {
            doj.setError("Date of joining is required");
            doj.requestFocus();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(getContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable submit button to prevent multiple submissions
        submitButton.setEnabled(false);
        submitButton.setTextColor(0xFF606060);

        // Upload image first
        String imageName = "worker_images/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(imageName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    // Now save data with image URL
                    Map<String, Object> workerData = new HashMap<>();
                    workerData.put("PNo", pNoVal);
                    workerData.put("ESINo", esiVal);
                    workerData.put("UANNo", uanVal);
                    workerData.put("PFNo", pfVal);
                    workerData.put("Name", nameVal);
                    workerData.put("FatherName", fatherVal);
                    workerData.put("ContactNumber", contactVal);
                    workerData.put("DateOfJoining", dojVal);
                    workerData.put("Department", deptVal);
                    workerData.put("DateOfLeave", dolVal);
                    workerData.put("ImageURL", imageUrl);
                    workerData.put("Timestamp", System.currentTimeMillis());

                    firestore.collection("Worker_Detail")
                            .add(workerData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(getContext(), "Worker detail saved successfully!", Toast.LENGTH_SHORT).show();
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                submitButton.setEnabled(true);
                                submitButton.setTextColor(0xFF2196F3);
                            });
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                    submitButton.setTextColor(0xFF2196F3);
                });
    }

    private void setupDatePicker(final EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);

        editText.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    R.style.DarkAlertDialog,
                    (DatePicker view1, int selectedYear, int selectedMonth, int selectedDay) -> {
                        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                selectedYear, selectedMonth + 1, selectedDay);
                        editText.setText(date);
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }
}
