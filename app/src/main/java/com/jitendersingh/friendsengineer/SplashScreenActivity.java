package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;


public class SplashScreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);

        // Load and start fade-in animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        // Move to main activity after 2.5 seconds
        new Handler().postDelayed(() -> {
            boolean isAdmin = checkIfUserIsAdmin();

            Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
            startActivity(intent);

            finish(); // Finish splash screen activity
        }, 2500);
    }

    private boolean checkIfUserIsAdmin(){
        return getSharedPreferences("UserPrefs",MODE_PRIVATE)
                .getBoolean("isAdmin",true);
    }
}

