package com.example.skillswap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        Button btnGuest = findViewById(R.id.btnGuest);

        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            mAuth = null;
        }

        // Check if user is already logged in with Firebase
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            navigateToMain(mAuth.getCurrentUser().getEmail());
            return;
        }

        btnLogin.setOnClickListener(v -> performLogin());
        btnRegister.setOnClickListener(v -> performRegistration());
        btnGuest.setOnClickListener(v -> navigateToMain("Guest Student"));
    }

    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address (e.g. name@university.edu)");
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth == null) {
            Toast.makeText(this, "Firebase Auth offline. Use Demo Guest mode.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : email;
                        Toast.makeText(LoginActivity.this, "Sign In successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain(userEmail);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(LoginActivity.this, "Sign In Failed: " + errorMsg + "\nTip: If you don't have an account, tap 'Create Account (Register)' first.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performRegistration() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address (e.g. name@university.edu)");
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth == null) {
            Toast.makeText(this, "Firebase Auth offline. Use Demo Guest mode.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : email;
                        Toast.makeText(LoginActivity.this, "Account created & registered successfully!", Toast.LENGTH_SHORT).show();
                        navigateToMain(userEmail);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(LoginActivity.this, "Register Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMain(String userEmail) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_EMAIL", userEmail);
        startActivity(intent);
        finish();
    }
}
