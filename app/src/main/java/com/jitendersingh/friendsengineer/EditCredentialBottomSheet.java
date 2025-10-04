package com.jitendersingh.friendsengineer;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.HashMap;
import java.util.Map;

public class EditCredentialBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_COLLECTION = "collection";
    private static final String ARG_DOC_ID = "docId";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_PASSWORD = "password";
    private static final String ARG_NAME = "name";
    private static final String ARG_FATHER_NAME = "fatherName";

    private String collectionName;
    private String documentId;
    private String username;
    private String password;
    private String name;
    private String fatherName;

    private EditText editName;
    private EditText editFatherName;
    private EditText editUsername;
    private EditText editPassword;
    private TextView saveButton;
    private TextView cancelButton;
    private ImageView closeButton;

    public static EditCredentialBottomSheet newInstance(String collectionName, String docId,
                                                        String username, String password,
                                                        String name, String fatherName) {
        EditCredentialBottomSheet fragment = new EditCredentialBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_COLLECTION, collectionName);
        args.putString(ARG_DOC_ID, docId);
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_NAME, name);
        args.putString(ARG_FATHER_NAME, fatherName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            collectionName = getArguments().getString(ARG_COLLECTION);
            documentId = getArguments().getString(ARG_DOC_ID);
            username = getArguments().getString(ARG_USERNAME);
            password = getArguments().getString(ARG_PASSWORD);
            name = getArguments().getString(ARG_NAME);
            fatherName = getArguments().getString(ARG_FATHER_NAME);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                // Make background transparent to remove white spots
                bottomSheet.setBackground(null);

                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(false);
                behavior.setSkipCollapsed(true);
            }
        });

        // Make dialog window background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
        }

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_edit_credential_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        editName = view.findViewById(R.id.editName);
        editFatherName = view.findViewById(R.id.editFatherName);
        editUsername = view.findViewById(R.id.editUsername);
        editPassword = view.findViewById(R.id.editPassword);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        closeButton = view.findViewById(R.id.closeButton);

        // Set current values
        editName.setText(name != null ? name : "");
        editFatherName.setText(fatherName != null ? fatherName : "");
        editUsername.setText(username != null ? username : "");
        editPassword.setText(password != null ? password : "");

        // Save button click
        saveButton.setOnClickListener(v -> saveCredentials());

        // Cancel button click
        cancelButton.setOnClickListener(v -> dismiss());

        // Close button click
        closeButton.setOnClickListener(v -> dismiss());
    }

    private void saveCredentials() {
        // Get updated values
        String updatedName = editName.getText().toString().trim();
        String updatedFatherName = editFatherName.getText().toString().trim();
        String updatedUsername = editUsername.getText().toString().trim();
        String updatedPassword = editPassword.getText().toString().trim();

        // Validate
        if (updatedName.isEmpty()) {
            editName.setError("Name is required");
            editName.requestFocus();
            return;
        }

        if (updatedFatherName.isEmpty()) {
            editFatherName.setError("Father's name is required");
            editFatherName.requestFocus();
            return;
        }

        if (updatedUsername.isEmpty()) {
            editUsername.setError("Username is required");
            editUsername.requestFocus();
            return;
        }

        if (updatedPassword.isEmpty()) {
            editPassword.setError("Password is required");
            editPassword.requestFocus();
            return;
        }

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("Name", updatedName);
        updates.put("FATHER_S_NAME", updatedFatherName);
        updates.put("Username", updatedUsername);
        updates.put("Password", updatedPassword);

        FirebaseFirestore.getInstance()
                .collection(collectionName)
                .document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Credentials updated successfully", Toast.LENGTH_SHORT).show();

                    // Refresh the parent activity
                    if (getActivity() instanceof CredentialsDetailsActivity) {
                        ((CredentialsDetailsActivity) getActivity()).loadCredentials();
                    }

                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
