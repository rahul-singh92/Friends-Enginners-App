package com.jitendersingh.friendsengineer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends Activity {
    private static final int STORAGE_PERMISSION_CODE = 2001;

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
        } else {
            // Privacy policy already accepted, check for storage permission
            requestStoragePermissionIfNeeded();
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
                                        .putString("logged_in_username", username)
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
            String privacyUrl = "https://github.com/rahul-singh92/Friends-Enginners-App/blob/main/privacy_policy.md";
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

                // Request storage permission after privacy policy accepted
                requestStoragePermissionIfNeeded();
            }
        });

        dialog.show();
    }

    private void requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        },
                        STORAGE_PERMISSION_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 to Android 12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted. You can now view PDFs.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied. PDF viewing may not work properly.", Toast.LENGTH_LONG).show();
            }
        }
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
                                    .putString("logged_in_username", username)
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
