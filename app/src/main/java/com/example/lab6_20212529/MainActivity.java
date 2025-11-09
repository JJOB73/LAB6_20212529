package com.example.lab6_20212529;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        BottomNavigationView navigationBarView = findViewById(R.id.bottomNavigation);
        navigationBarView.setOnItemSelectedListener(this::onNavigationItemSelected);
        navigationBarView.setSelectedItemId(R.id.menu_vehicles);

        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
        try {
            builder.requestIdToken(getString(R.string.default_web_client_id));
        } catch (Exception ignored) {
        }
        googleSignInClient = GoogleSignIn.getClient(this, builder.build());
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_logout) {
            showLogoutConfirmation();
            return false;
        }

        Fragment fragment;
        if (itemId == R.id.menu_vehicles) {
            fragment = new VehiclesFragment();
            setTitle(R.string.menu_vehicles);
        } else if (itemId == R.id.menu_records) {
            fragment = new RecordsFragment();
            setTitle(R.string.menu_records);
        } else {
            fragment = new SummaryFragment();
            setTitle(R.string.menu_summary);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        return true;
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_logout)
                .setMessage(R.string.message_logout_confirmation)
                .setPositiveButton(R.string.action_logout, (dialog, which) -> signOut())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void signOut() {
        firebaseAuth.signOut();
        googleSignInClient.signOut();
        navigateToLogin();
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}