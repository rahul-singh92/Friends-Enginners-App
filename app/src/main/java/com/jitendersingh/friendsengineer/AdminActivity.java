package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Set up the custom toolbar
        Toolbar toolbar = findViewById(R.id.admin_toolbar);
        setSupportActionBar(toolbar);

        // Hide default title since we have custom layout
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        // User icon change password functionality
        LinearLayout userIcon = findViewById(R.id.user_icon);
        userIcon.setOnClickListener(v -> showUserOptionsDialog());

        // Message Icon
        LinearLayout messageIcon = findViewById(R.id.message_icon);
        messageIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, MessageActivity.class);
            startActivity(intent);
        });

        // Upload Excel Button
        CardView uploadButton = findViewById(R.id.admin_button);
        uploadButton.setOnClickListener(v -> {
            UploadExcelBottomSheet bottomSheet = new UploadExcelBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "UploadExcelBottomSheet");
        });

        // View Details Button
        CardView viewDetailsButton = findViewById(R.id.admin_button2);
        viewDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, TableListActivity.class);
            startActivity(intent);
        });

        // Upload PDF Button
        CardView uploadPdfButton = findViewById(R.id.admin_button_pdf);
        uploadPdfButton.setOnClickListener(v -> {
            UploadPdfBottomSheet pdfBottomSheet = new UploadPdfBottomSheet();
            pdfBottomSheet.show(getSupportFragmentManager(), "UploadPdfBottomSheet");
        });

        // View PDF Button
        CardView viewPdfButton = findViewById(R.id.view_pdf_button);
        viewPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, WageCollectionsActivity.class);
            startActivity(intent);
        });

        // Advance Request Button
        CardView advanceRequestButton = findViewById(R.id.advance_request_button);
        advanceRequestButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdvanceRequestActivity.class);
            startActivity(intent);
        });

        // Total Advance Button
        CardView totalAdvanceButton = findViewById(R.id.total_advance_button);
        totalAdvanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, TotalAdvanceActivity.class);
            startActivity(intent);
        });

        // Worker Schedule Upload
        CardView uploadScheduleButton = findViewById(R.id.worker_schedule_upload_button);
        uploadScheduleButton.setOnClickListener(v -> {
            UploadScheduleBottomSheet bottomSheet = new UploadScheduleBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "WorkerScheduleBottomSheet");
        });

        // View Worker Schedule
        CardView viewScheduleButton = findViewById(R.id.view_schedule_button);
        viewScheduleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchListActivity.class);
            startActivity(intent);
        });

        // Upload Credentials
        CardView uploadCredentialButton = findViewById(R.id.upload_credentials_button);
        uploadCredentialButton.setOnClickListener(v -> {
            UploadCredentialsBottomSheet bottomSheet = new UploadCredentialsBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "CredentialBottomScheet");
        });

        // View Credentials
        CardView viewCredentialButton = findViewById(R.id.view_credentials_button);
        viewCredentialButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, CredentialsTablesActivity.class);
            startActivity(intent);
        });

        // Enter Worker Details
        CardView enterWorkerDetails = findViewById(R.id.enter_worker_details_button);
        enterWorkerDetails.setOnClickListener(v -> {
            WorkerDetailBottomSheet bottomSheet = new WorkerDetailBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "WorkerDetailBottomScheet");
        });

        // View Worker Details
        CardView viewWorkerDetailsButton = findViewById(R.id.view_worker_details_button);
        viewWorkerDetailsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewWorkerDetailsActivity.class));
        });
    }

    private void showUserOptionsDialog() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("User Options")
                .setItems(new String[]{"Change Password"}, (dialog, which) -> {
                    if (which == 0) {
                        showChangePasswordDialog();
                    }
                }).show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Change Password");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        final EditText inputOldPassword = viewInflated.findViewById(R.id.input_old_password);
        final EditText inputNewPassword = viewInflated.findViewById(R.id.input_new_password);
        final EditText inputConfirmPassword = viewInflated.findViewById(R.id.input_confirm_password);

        builder.setView(viewInflated);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String oldPassword = inputOldPassword.getText().toString().trim();
            String newPassword = inputNewPassword.getText().toString().trim();
            String confirmPassword = inputConfirmPassword.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "New Password and Confirm Password do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(newPassword)) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePassword(oldPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

    private void updatePassword(String oldPassword, String newPassword) {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            Toast.makeText(this, "No logged in user", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] collections = {"credentials_admin", "credentials_worker"};
        final boolean[] passwordUpdated = {false};
        final boolean[] userFoundInAnyCollection = {false};

        for (String collection : collections) {
            db.collection(collection)
                    .whereEqualTo("Username", currentUsername)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            userFoundInAnyCollection[0] = true;
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                String storedPassword = doc.getString("Password");
                                if (storedPassword != null && storedPassword.equals(oldPassword)) {
                                    if (!passwordUpdated[0]) {
                                        db.collection(collection).document(doc.getId())
                                                .update("Password", newPassword)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                                    passwordUpdated[0] = true;
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }
                                    return;
                                } else {
                                    if (!passwordUpdated[0]) {
                                        Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                return;
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error fetching user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private String getCurrentUsername() {
        return getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("logged_in_username", null);
    }
}
