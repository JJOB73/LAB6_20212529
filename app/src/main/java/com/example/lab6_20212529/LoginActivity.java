package com.example.lab6_20212529;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private CircularProgressIndicator progressIndicator;
    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private MaterialButton googleButton;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            navigateToMain();
            finish();
            return;
        }

        setupGoogleClient();
        bindViews();
        setupGoogleLauncher();
        setupListeners();
    }

    private void bindViews() {
        emailLayout = findViewById(R.id.inputLayoutEmail);
        passwordLayout = findViewById(R.id.inputLayoutPassword);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        progressIndicator = findViewById(R.id.progressIndicator);
        loginButton = findViewById(R.id.buttonLogin);
        registerButton = findViewById(R.id.buttonRegister);
        googleButton = findViewById(R.id.buttonGoogle);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
        googleButton.setOnClickListener(v -> launchGoogleSignIn());
    }

    private void setupGoogleClient() {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
        try {
            builder.requestIdToken(getString(R.string.default_web_client_id));
        } catch (Exception ignored) {
            // If the client ID is not configured yet, Google Sign-In will not work
        }
        googleSignInClient = GoogleSignIn.getClient(this, builder.build());
    }

    private void setupGoogleLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleGoogleResult(result.getData());
                    } else {
                        setLoading(false);
                    }
                }
        );
    }

    private void launchGoogleSignIn() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);
            if (account == null) {
                showMessage(getString(R.string.error_google_sign_in));
                setLoading(false);
                return;
            }
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            showMessage(getString(R.string.error_google_sign_in));
            setLoading(false);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        navigateToMain();
                        finish();
                    } else {
                        showMessage(getString(R.string.error_generic));
                    }
                });
    }

    private void attemptLogin() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";

        boolean valid = validateEmail(email) & validatePassword(password);
        if (!valid) {
            return;
        }

        setLoading(true);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            navigateToMain();
                            finish();
                        }
                    } else {
                        showMessage(getString(R.string.error_authentication));
                    }
                });
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

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_password_empty));
            return false;
        } else {
            passwordLayout.setError(null);
            return true;
        }
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        registerButton.setEnabled(!loading);
        googleButton.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}

