package com.example.lab6_20212529;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmEditText;
    private CircularProgressIndicator progressIndicator;
    private MaterialButton createAccountButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        bindViews();
        setupToolbar();

        createAccountButton.setOnClickListener(v -> attemptRegister());
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.inputLayoutName);
        emailLayout = findViewById(R.id.inputLayoutEmail);
        passwordLayout = findViewById(R.id.inputLayoutPassword);
        confirmLayout = findViewById(R.id.inputLayoutConfirmPassword);
        nameEditText = findViewById(R.id.editTextName);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmEditText = findViewById(R.id.editTextConfirmPassword);
        progressIndicator = findViewById(R.id.progressIndicator);
        createAccountButton = findViewById(R.id.buttonCreateAccount);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarRegister);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_register);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void attemptRegister() {
        String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String confirmPassword = confirmEditText.getText() != null ? confirmEditText.getText().toString() : "";

        boolean valid = validateName(name) & validateEmail(email) & validatePassword(password, confirmPassword);
        if (!valid) {
            return;
        }

        setLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && !TextUtils.isEmpty(name)) {
                            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profile);
                        }
                        Toast.makeText(this, R.string.message_register_success, Toast.LENGTH_SHORT).show();
                        setLoading(false);
                        finish();
                    } else {
                        setLoading(false);
                        Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateName(String name) {
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_name_empty));
            return false;
        } else {
            nameLayout.setError(null);
            return true;
        }
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_email_empty));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email_invalid));
            return false;
        } else {
            emailLayout.setError(null);
            return true;
        }
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_password_empty));
            return false;
        } else if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_password_length));
            return false;
        } else {
            passwordLayout.setError(null);
        }

        if (!TextUtils.equals(password, confirmPassword)) {
            confirmLayout.setError(getString(R.string.error_password_mismatch));
            return false;
        } else {
            confirmLayout.setError(null);
            return true;
        }
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        createAccountButton.setEnabled(!loading);
    }
}

