package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
