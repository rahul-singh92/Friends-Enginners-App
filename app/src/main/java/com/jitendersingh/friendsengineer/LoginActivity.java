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
import android.view.WindowInsetsController;
import android.view.Window;

public class LoginActivity extends Activity {

    // Auto-login session length: 30 days in milliseconds.
    private static final long SESSION_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000;

    EditText usernameField, passwordField;
    Button loginBtn;
    CardView loginCard;
    TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If there's a still-valid saved session, skip the login screen
        // entirely and go straight to Admin/Worker.
        if (tryAutoLogin()) {
            return;
        }

        setContentView(R.layout.activity_login);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0, // 0 = light icons (i.e. NOT light status bar)
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            // For API 23–29
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // clear the "light status bar" flag
            decorView.setSystemUiVisibility(flags);
        }

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

            // Check in credentials_admin first.
            // Source.SERVER forces a live network check — without this,
            // Firestore's offline cache can validate a login using
            // previously-cached credentials even with no internet connection,
            // which is a security gap for a login screen specifically.
            db.collection("credentials_admin")
                    .whereEqualTo("Username", username)
                    .get(com.google.firebase.firestore.Source.SERVER)
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
                                        .putLong("login_timestamp", System.currentTimeMillis())
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
                        } else if (task.isSuccessful()) {
                            // Admin query succeeded but found no matching username — check worker instead.
                            checkWorkerCredentials(db, username, password);
                        }
                        // If task.isSuccessful() is false (e.g. offline / network error),
                        // do nothing here — the addOnFailureListener below handles it
                        // with a proper "Connection error" message instead of
                        // incorrectly falling through to a worker-credential check
                        // that would just fail the same way.
                    })
                    .addOnFailureListener(e -> {
                        resetLoginButton();
                        showErrorWithAnimation("Connection error. Please try again.");
                    });
        });
    }

    /**
     * Checks for a saved session (username + role + login timestamp) that's
     * still within the 30-day validity window. If found, navigates straight
     * to AdminActivity/WorkerActivity and returns true (caller should skip
     * the rest of onCreate). Returns false if no valid session exists,
     * meaning the normal login screen should be shown.
     */
    private boolean tryAutoLogin() {
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String savedUsername = userPrefs.getString("logged_in_username", null);
        long loginTimestamp = userPrefs.getLong("login_timestamp", 0L);

        if (savedUsername == null || loginTimestamp == 0L) {
            return false; // never logged in / already logged out
        }

        long elapsed = System.currentTimeMillis() - loginTimestamp;
        if (elapsed > SESSION_VALIDITY_MS) {
            // Session expired — clear it so the user must log in again.
            userPrefs.edit()
                    .remove("logged_in_username")
                    .remove("isAdmin")
                    .remove("login_timestamp")
                    .apply();
            return false;
        }

        boolean isAdmin = userPrefs.getBoolean("isAdmin", false);

        Intent intent = new Intent(this, isAdmin ? AdminActivity.class : WorkerActivity.class);
        if (!isAdmin) {
            intent.putExtra("username", savedUsername);
        }
        startActivity(intent);
        finish();
        return true;
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
            }
        });

        dialog.show();
    }

    private void checkWorkerCredentials(FirebaseFirestore db, String username, String password) {
        db.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get(com.google.firebase.firestore.Source.SERVER)
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
                                    .putLong("login_timestamp", System.currentTimeMillis())
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