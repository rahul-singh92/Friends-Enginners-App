package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditCredentialBottomSheet extends BottomSheetDialogFragment {

    private EditText etUsername, etPassword, etName, etFatherName;
    private Button btnSave;

    private String docId;
    private String collectionName;

    private FirebaseFirestore firestore;

    public static EditCredentialBottomSheet newInstance(String collectionName, String docId,
                                                        String username, String password,
                                                        String name, String fatherName) {
        EditCredentialBottomSheet fragment = new EditCredentialBottomSheet();
        Bundle args = new Bundle();
        args.putString("collectionName", collectionName);
        args.putString("docId", docId);
        args.putString("username", username);
        args.putString("password", password);
        args.putString("name", name);
        args.putString("fatherName", fatherName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_edit_credential, container, false);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        etName = view.findViewById(R.id.etName);
        etFatherName = view.findViewById(R.id.etFatherName);
        btnSave = view.findViewById(R.id.btnSave);

        firestore = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            collectionName = getArguments().getString("collectionName");
            docId = getArguments().getString("docId");

            etUsername.setText(getArguments().getString("username"));
            etPassword.setText(getArguments().getString("password"));
            etName.setText(getArguments().getString("name"));
            etFatherName.setText(getArguments().getString("fatherName"));
        }

        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String fatherName = etFatherName.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Username and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("Username", username);
            updatedData.put("Password", password);
            updatedData.put("Name", name);
            updatedData.put("FATHER_S_NAME", fatherName);

            firestore.collection(collectionName)
                    .document(docId)
                    .update(updatedData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Updated successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                        if (getActivity() instanceof CredentialsDetailsActivity) {
                            ((CredentialsDetailsActivity) getActivity()).loadCredentials();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        return view;
    }
}
