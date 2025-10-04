package com.jitendersingh.friendsengineer;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class WorkerActivity extends AppCompatActivity {

    TextView welcomeText;
    String username;
    String workerName;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);

        // Set up the custom toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.worker_toolbar);
        setSupportActionBar(toolbar);

        // Hide default title since we have custom layout
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        // Change ImageView to LinearLayout for click listeners
        LinearLayout userIcon = findViewById(R.id.user_icon);
        userIcon.setOnClickListener(v -> showUserOptionsDialog());

        LinearLayout messageIcon = findViewById(R.id.message_icon);
        messageIcon.setOnClickListener(v -> {
            Intent intent = new Intent(WorkerActivity.this, MessageViewOnlyActivity.class);
            startActivity(intent);
        });

        welcomeText = findViewById(R.id.welcome_text);
        username = getIntent().getStringExtra("username");

        fetchWorkerNameAndSetupUI(username);
    }


    private void showUserOptionsDialog() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("User Options")
                .setItems(new String[]{"Change Password"}, (dialog, which) -> {
                    if (which == 0) {
                        showChangePasswordDialog();
                    }
                })
                .show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Change Password");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        final EditText inputOldPassword = viewInflated.findViewById(R.id.input_old_password);
        final EditText inputNewPassword = viewInflated.findViewById(R.id.input_new_password);
        final EditText inputConfirmPassword = viewInflated.findViewById(R.id.input_confirm_password);

        builder.setView(viewInflated);

        builder.setPositiveButton("Change", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
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

                updatePassword(oldPassword, newPassword, dialog);
            });
        });

        dialog.show();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

    private void updatePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not logged in properly", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        boolean updated = false;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String storedPassword = doc.getString("Password");

                            if (storedPassword != null && storedPassword.equals(oldPassword)) {
                                db.collection("credentials_worker").document(doc.getId())
                                        .update("Password", newPassword)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                updated = true;
                                break;
                            }
                        }
                        if (!updated) {
                            Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchWorkerNameAndSetupUI(String username) {
        db.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        workerName = doc.getString("Name");
                        if (workerName != null && !workerName.isEmpty()) {
                            String capitalized = Character.toUpperCase(workerName.charAt(0)) + workerName.substring(1);
                            welcomeText.setText("Welcome, " + capitalized);
                        } else {
                            welcomeText.setText("Welcome, Worker");
                        }
                    } else {
                        welcomeText.setText("Welcome, Worker");
                        workerName = null;
                    }
                    setupButtons();
                })
                .addOnFailureListener(e -> {
                    welcomeText.setText("Welcome, Worker");
                    workerName = null;
                    Toast.makeText(this, "Failed to fetch worker name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupButtons();
                });
    }

    private void setupButtons() {
        CardView requestAdvanceButton = findViewById(R.id.request_advance_button);
        requestAdvanceButton.setOnClickListener(v -> checkRequestLimitAndShowDialog());

        CardView totalAdvanceButton = findViewById(R.id.total_advance_button);
        totalAdvanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(WorkerActivity.this, TotalRequestedActivity.class);
            intent.putExtra("workerName", workerName);
            startActivity(intent);
        });

        CardView viewScheduleBtn = findViewById(R.id.view_schedule_button);
        viewScheduleBtn.setOnClickListener(v -> {
            Intent intent = new Intent(WorkerActivity.this, WorkerScheduleActivity.class);
            intent.putExtra("workerName", workerName);
            startActivity(intent);
        });

        CardView viewMyDetail = findViewById(R.id.my_detail);
        viewMyDetail.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("credentials_worker")
                    .whereEqualTo("Username", username)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            String name = doc.getString("Name");
                            String fatherName = doc.getString("FATHER_S_NAME");

                            Intent intent = new Intent(WorkerActivity.this, WorkerDetailsActivity.class);
                            intent.putExtra("name", name != null ? name : "Unknown");
                            intent.putExtra("father_name", fatherName != null ? fatherName : "Unknown");
                            startActivity(intent);
                        } else {
                            Toast.makeText(WorkerActivity.this, "User info not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(WorkerActivity.this, "Error fetching details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        CardView pfPassbookBtn = findViewById(R.id.btn_pf_passbook);
        pfPassbookBtn.setOnClickListener(v -> {
            String url = "https://passbook.epfindia.gov.in/MemberPassBook/login";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        CardView uanPortalBtn = findViewById(R.id.btn_uan_portal);
        uanPortalBtn.setOnClickListener(v -> {
            String url = "https://unifiedportal-mem.epfindia.gov.in/memberinterface/";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }

    private void checkRequestLimitAndShowDialog() {
        if (workerName == null || workerName.isEmpty()) {
            Toast.makeText(this, "Unable to get user name", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date monthStart = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date monthEnd = cal.getTime();

        firestore.collection("Requested_Amount")
                .whereEqualTo("Name", workerName)
                .whereGreaterThanOrEqualTo("RequestTime", monthStart)
                .whereLessThanOrEqualTo("RequestTime", monthEnd)
                .whereIn("Status", java.util.Arrays.asList("Pending", "Accepted", "Rejected"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (count >= 2) {
                        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                                .setTitle("Limit Reached")
                                .setMessage("You have already requested advance 2 times this month. You cannot request more.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        showRequestAdvanceDialog();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to check request limit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showRequestAdvanceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_request_advance, null);
        final EditText amountInput = dialogView.findViewById(R.id.edit_request_amount);
        final EditText reasonInput = dialogView.findViewById(R.id.edit_reason);

        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Request Advance")
                .setView(dialogView)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String amount = amountInput.getText().toString().trim();
                        String reason = reasonInput.getText().toString().trim();

                        if (amount.isEmpty() || reason.isEmpty()) {
                            Toast.makeText(WorkerActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        new AlertDialog.Builder(WorkerActivity.this, R.style.DarkAlertDialog)
                                .setTitle("Confirm Request")
                                .setMessage("Do you really want to request an advance of ₹" + amount + "?")
                                .setPositiveButton("Yes", (confDialog, confWhich) -> insertAdvanceRequest(amount, reason))
                                .setNegativeButton("No", null)
                                .show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void insertAdvanceRequest(String amount, String reason) {
        if (workerName == null) {
            Toast.makeText(this, "Worker name not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String fatherName = "Unknown";

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        com.google.firebase.Timestamp requestTimestamp = com.google.firebase.Timestamp.now();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("Name", workerName);
        requestData.put("FatherName", fatherName);
        requestData.put("Amount", amount);
        requestData.put("Reason", reason);
        requestData.put("RequestTime", requestTimestamp);
        requestData.put("Status", "Pending");

        firestore.collection("Requested_Amount")
                .add(requestData)
                .addOnSuccessListener(docRef -> Toast.makeText(this, "Request stored in Firestore", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to store in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        Toast.makeText(this, "Advance request submitted successfully", Toast.LENGTH_SHORT).show();
    }
}
