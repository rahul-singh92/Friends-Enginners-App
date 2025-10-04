package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashScreenActivity extends Activity {

    private ImageView logo;
    private TextView appName, appTagline, loadingText;
    private ProgressBar loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.app_name);
        appTagline = findViewById(R.id.app_tagline);
        loadingBar = findViewById(R.id.loading_bar);
        loadingText = findViewById(R.id.loading_text);

        // Start animations
        startAnimations();

        // Move to next activity after 3 seconds
        new Handler().postDelayed(() -> {
            // Fade out animation
            logo.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .start();

            appName.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .start();

            appTagline.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .start();

            loadingBar.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .start();

            loadingText.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    })
                    .start();

        }, 3000);
    }

    private void startAnimations() {
        // Logo animation - fade in, scale up, and slight bounce
        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new BounceInterpolator())
                .start();

        // App name animation - fade in and slide up
        appName.setAlpha(0f);
        appName.setTranslationY(30f);

        appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Tagline animation - fade in and slide up
        appTagline.setAlpha(0f);
        appTagline.setTranslationY(30f);

        appTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(700)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Loading bar animation - fade in
        loadingBar.setAlpha(0f);
        loadingBar.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(1000)
                .start();

        // Loading text animation - fade in
        loadingText.setAlpha(0f);
        loadingText.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(1100)
                .start();
    }
}
