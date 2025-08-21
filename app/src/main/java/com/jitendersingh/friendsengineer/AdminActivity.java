package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin); // Ensure this matches your XML filename

        // Set up the custom toolbar
        Toolbar toolbar = findViewById(R.id.admin_toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();

        // User icon change password functionality
        ImageView userIcon = findViewById(R.id.user_icon);
        userIcon.setOnClickListener(v -> showUserOptionsDialog());

        //Message Icon
        ImageView messageIcon = findViewById(R.id.message_icon);
        messageIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, MessageActivity.class);
            startActivity(intent);
        });


        // Find the button and set a click listener
        Button uploadButton = findViewById(R.id.admin_button);
        uploadButton.setOnClickListener(v -> {
            UploadExcelBottomSheet bottomSheet = new UploadExcelBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "UploadExcelBottomSheet");
        });

        Button viewDetailsButton = findViewById(R.id.admin_button2);
        viewDetailsButton.setOnClickListener(v -> {
            // Launch the TableListActivity to show available data tables
            Intent intent = new Intent(AdminActivity.this, TableListActivity.class);
            startActivity(intent);
        });

        Button uploadPdfButton = findViewById(R.id.admin_button_pdf);
        uploadPdfButton.setOnClickListener(v -> {
            UploadPdfBottomSheet pdfBottomSheet = new UploadPdfBottomSheet();
            pdfBottomSheet.show(getSupportFragmentManager(), "UploadPdfBottomSheet");
        });

        Button viewPdfButton = findViewById(R.id.view_pdf_button);
        viewPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, WageCollectionsActivity.class);
            startActivity(intent);
        });

        Button advanceRequestButton = findViewById(R.id.advance_request_button);
        advanceRequestButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdvanceRequestActivity.class);
            startActivity(intent);
        });

        //Total Advance Button
        Button totalAdvanceButton = findViewById(R.id.total_advance_button);
        totalAdvanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, TotalAdvanceActivity.class);
            startActivity(intent);
        });

        //Worker Schedule
        Button uploadScheduleButton = findViewById(R.id.worker_schedule_upload_button);
        uploadScheduleButton.setOnClickListener(v -> {
            UploadScheduleBottomSheet bottomSheet = new UploadScheduleBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "WorkerScheduleBottomSheet");
        });

        // View Worker Schedule
        findViewById(R.id.view_schedule_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchListActivity.class);
            startActivity(intent);
        });

        // Upload Credentials button
        Button uploadCredentialButton = findViewById(R.id.upload_credentials_button);
        uploadCredentialButton.setOnClickListener(v -> {
            UploadCredentialsBottomSheet bottomSheet = new UploadCredentialsBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "CredentialBottomScheet");
        });

        Button viewCredentialButton = findViewById(R.id.view_credentials_button);
        viewCredentialButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, CredentialsTablesActivity.class);
            startActivity(intent);
        });

        Button enterWorkerDetails = findViewById(R.id.enter_worker_details_button);
        enterWorkerDetails.setOnClickListener(v -> {
            WorkerDetailBottomSheet bottomSheet = new WorkerDetailBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "WorkerDetailBottomScheet");
        });

        // View Worker Details
        findViewById(R.id.view_worker_details_button).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewWorkerDetailsActivity.class));
        });

    }

    private void showUserOptionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("User Options")
                .setItems(new String[]{"Change Password"}, (dialog, which) -> {
                    if (which == 0) {
                        showChangePasswordDialog();
                    }
                }).show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        // Inflate custom layout with EditTexts
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        final EditText inputOldPassword = viewInflated.findViewById(R.id.input_old_password);
        final EditText inputNewPassword = viewInflated.findViewById(R.id.input_new_password);
        final EditText inputConfirmPassword = viewInflated.findViewById(R.id.input_confirm_password);

        builder.setView(viewInflated);

        builder.setPositiveButton("OK", (dialog, which) -> {
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

        builder.show();
    }

    private boolean isValidPassword(String password) {
        // Basic validation - you can enhance this logic
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
                                    if (!passwordUpdated[0]) {  // Prevent multiple updates
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
                                return; // exit after first match found
                            }
                        } else if (!userFoundInAnyCollection[0]) {
                            // If username not found in any collections after all async calls complete
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error fetching user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private String getCurrentUsername() {
        return getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("logged_in_username", null);
    }
}
