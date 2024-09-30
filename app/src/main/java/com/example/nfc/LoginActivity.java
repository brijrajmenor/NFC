package com.example.nfc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    // Shared Preferences key
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private EditText usernameEditText, passwordEditText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
        if (isLoggedIn) {
            // User is logged in, redirect to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Close the login activity
            return;
        }

        setContentView(R.layout.activity_login);

        // UI Elements
        usernameEditText = findViewById(R.id.Email);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        // Handle login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                // Simple validation for demo purposes
                if ("admin".equals(username) && "password123".equals(password)) {
                    // Successful login
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                    // Save login state
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(KEY_IS_LOGGED_IN, true);
                    editor.apply();

                    // Start MainActivity after login
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();  // Close the login activity
                } else {
                    // Login failed
                    Toast.makeText(LoginActivity.this, "Invalid credentials. Try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
