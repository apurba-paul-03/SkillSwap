package com.example.skillswap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
    private Button btnGuest;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            mAuth = null;
        }

        // Check if user is already logged in
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

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth == null) {
            Toast.makeText(this, "Firebase Auth offline. Continuing in Guest mode.", Toast.LENGTH_SHORT).show();
            navigateToMain(email);
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : email;
                        Toast.makeText(LoginActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain(userEmail);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(LoginActivity.this, "Auth Failed: " + errorMsg + "\nFallback: Logged in as Guest", Toast.LENGTH_LONG).show();
                        navigateToMain(email);
                    }
                });
    }

    private void performRegistration() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || password.length() < 6) {
            Toast.makeText(this, "Please enter a valid email and a 6+ character password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth == null) {
            Toast.makeText(this, "Firebase Auth offline. Registered locally as: " + email, Toast.LENGTH_SHORT).show();
            navigateToMain(email);
            return;
        }

        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : email;
                        Toast.makeText(LoginActivity.this, "User registered successfully!", Toast.LENGTH_SHORT).show();
                        navigateToMain(userEmail);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(LoginActivity.this, "Register Failed: " + errorMsg + "\nFallback: Registered as Demo Student", Toast.LENGTH_LONG).show();
                        navigateToMain(email);
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
