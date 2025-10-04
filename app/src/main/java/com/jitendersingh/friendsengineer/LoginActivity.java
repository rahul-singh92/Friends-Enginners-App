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
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends Activity {
    private static final int STORAGE_PERMISSION_CODE = 2001;

    EditText usernameField, passwordField;
    Button loginBtn;
    CardView loginCard;
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        usernameField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginButton);
        errorText = findViewById(R.id.errorText);

        // Get the login card (parent of logo)
        View logoView = findViewById(R.id.logo);
        loginCard = (CardView) logoView.getParent().getParent();

        // Start animations
        startAnimations();

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Check if privacy policy was already accepted
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean policyAccepted = prefs.getBoolean("privacy_policy_accepted", false);

        if (!policyAccepted) {
            showPrivacyPolicyDialog();
        } else {
            requestStoragePermissionIfNeeded();
        }

        loginBtn.setOnClickListener(view -> {
            // Hide error text
            errorText.setVisibility(View.GONE);

            // Button press animation
            view.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();

            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showErrorWithAnimation("Please enter both username and password");
                return;
            }

            // Show loading state
            loginBtn.setEnabled(false);
            loginBtn.setText("Signing in...");

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

                                // Success animation
                                loginCard.animate()
                                        .alpha(0f)
                                        .scaleX(0.9f)
                                        .scaleY(0.9f)
                                        .setDuration(300)
                                        .withEndAction(() -> {
                                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                            finish();
                                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                        })
                                        .start();
                            } else {
                                resetLoginButton();
                                showErrorWithAnimation("Wrong username or password");
                            }
                        } else {
                            // Not found in admin, check in worker
                            checkWorkerCredentials(db, username, password);
                        }
                    })
                    .addOnFailureListener(e -> {
                        resetLoginButton();
                        showErrorWithAnimation("Connection error. Please try again.");
                    });
        });
    }

    private void startAnimations() {
        // Fade in and slide up animation for login card
        loginCard.setAlpha(0f);
        loginCard.setTranslationY(50f);
        loginCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(100)
                .start();

        // Fade in for input fields
        usernameField.setAlpha(0f);
        passwordField.setAlpha(0f);
        loginBtn.setAlpha(0f);

        usernameField.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(400)
                .start();

        passwordField.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(500)
                .start();

        loginBtn.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(600)
                .start();
    }

    private void showErrorWithAnimation(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        errorText.setAlpha(0f);
        errorText.setTranslationX(-20f);

        errorText.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .start();

        // Shake animation for card
        loginCard.animate()
                .translationX(20f)
                .setDuration(50)
                .withEndAction(() ->
                        loginCard.animate()
                                .translationX(-20f)
                                .setDuration(50)
                                .withEndAction(() ->
                                        loginCard.animate()
                                                .translationX(20f)
                                                .setDuration(50)
                                                .withEndAction(() ->
                                                        loginCard.animate()
                                                                .translationX(0f)
                                                                .setDuration(50)
                                                                .start()
                                                )
                                                .start()
                                )
                                .start()
                )
                .start();

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void resetLoginButton() {
        loginBtn.setEnabled(true);
        loginBtn.setText("Sign In");
    }

    private void showPrivacyPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_privacy_policy, null);

        TextView privacyLink = dialogView.findViewById(R.id.privacy_policy_link);
        CheckBox checkboxAccept = dialogView.findViewById(R.id.checkbox_accept);
        Button buttonOk = dialogView.findViewById(R.id.button_ok);

        builder.setView(dialogView);
        builder.setCancelable(false);

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
                buttonOk.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64B5F6")));
            } else {
                buttonOk.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#404040")));
            }
        });

        // OK button click
        buttonOk.setOnClickListener(v -> {
            if (checkboxAccept.isChecked()) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("privacy_policy_accepted", true).apply();
                dialog.dismiss();
                requestStoragePermissionIfNeeded();
            }
        });

        dialog.show();
    }

    private void requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkWorkerCredentials(FirebaseFirestore db, String username, String password) {
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

                            // Success animation
                            loginCard.animate()
                                    .alpha(0f)
                                    .scaleX(0.9f)
                                    .scaleY(0.9f)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        Intent intent = new Intent(LoginActivity.this, WorkerActivity.class);
                                        intent.putExtra("username", username);
                                        startActivity(intent);
                                        finish();
                                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    })
                                    .start();
                        } else {
                            resetLoginButton();
                            showErrorWithAnimation("Wrong username or password");
                        }
                    } else {
                        resetLoginButton();
                        showErrorWithAnimation("Wrong username or password");
                    }
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    showErrorWithAnimation("Connection error. Please try again.");
                });
    }
}
