package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends Activity {
    EditText usernameField, passwordField;
    Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginButton);
        TextView errorText = findViewById(R.id.errorText);

        DatabaseHelper dbHelper = new DatabaseHelper(this); // optional if used later

        // Check if privacy policy was already accepted
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean policyAccepted = prefs.getBoolean("privacy_policy_accepted", false);

        if (!policyAccepted) {
            showPrivacyPolicyDialog();
        }

        loginBtn.setOnClickListener(view -> {
            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                errorText.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Check in credentials_admin first
            db.collection("credentials_admin")
                    .whereEqualTo("Username", username)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            boolean valid = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String storedPassword = document.getString("Password");
                                if (storedPassword != null && storedPassword.equals(password)) {
                                    valid = true;
                                    break;
                                }
                            }
                            if (valid) {
                                // Admin login success
                                getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("isAdmin", true)
                                        .putString("logged_in_username",username)
                                        .apply();

                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                finish();
                            } else {
                                showError(errorText, "Wrong username or password");
                            }
                        } else {
                            // Not found in admin, check in worker
                            checkWorkerCredentials(db, username, password, errorText);
                        }
                    });
        });
    }

    private void showPrivacyPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_privacy_policy, null);

        TextView privacyLink = dialogView.findViewById(R.id.privacy_policy_link);
        CheckBox checkboxAccept = dialogView.findViewById(R.id.checkbox_accept);
        Button buttonOk = dialogView.findViewById(R.id.button_ok);

        builder.setView(dialogView);
        builder.setCancelable(false); // User must accept to continue

        AlertDialog dialog = builder.create();

        // Privacy policy link click
        privacyLink.setOnClickListener(v -> {
            String privacyUrl = "https://github.com/rahul-singh92/Friends-Enginners-App/blob/main/privacy_policy.md"; // Replace with your actual GitHub privacy policy URL
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
            startActivity(intent);
        });

        // Enable/disable OK button based on checkbox
        checkboxAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonOk.setEnabled(isChecked);
            if (isChecked) {
                buttonOk.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1976D2")));
            } else {
                buttonOk.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
            }
        });

        // OK button click
        buttonOk.setOnClickListener(v -> {
            if (checkboxAccept.isChecked()) {
                // Save acceptance in SharedPreferences
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("privacy_policy_accepted", true).apply();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void checkWorkerCredentials(FirebaseFirestore db, String username, String password, TextView errorText) {
        db.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        boolean valid = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String storedPassword = document.getString("Password");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                valid = true;
                                break;
                            }
                        }
                        if (valid) {
                            // Worker login success
                            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("isAdmin", false)
                                    .putString("logged_in_username",username)
                                    .apply();

                            Intent intent = new Intent(LoginActivity.this, WorkerActivity.class);
                            intent.putExtra("username", username);
                            startActivity(intent);
                            finish();
                        } else {
                            showError(errorText, "Wrong username or password");
                        }
                    } else {
                        showError(errorText, "Wrong username or password");
                    }
                });
    }

    private void showError(TextView errorText, String message) {
        errorText.setVisibility(View.VISIBLE);
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
