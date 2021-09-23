package com.example.videomeeting.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.videomeeting.R;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaopiz.kprogresshud.KProgressHUD;

public class SignInActivity extends AppCompatActivity {

    private EditText email, password;
    private MaterialButton btnSignIn;
    private TextView signUpText;
    private KProgressHUD progress;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        bindViews();
        setData();
        setListener();
    }

    private void bindViews() {
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.btnSignIn);
        signUpText = findViewById(R.id.signUpText);
    }

    private void setData() {
        //Progress Bar
        progress = KProgressHUD.create(SignInActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait...")
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        //Shared Preference
        preferenceManager = new PreferenceManager(getApplicationContext());
    }

    private void setListener() {

        //Button Sign In
        btnSignIn.setOnClickListener(view -> {
            validateData();
        });

        //Sign Up Text
        signUpText.setOnClickListener(view -> startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
    }

    private void validateData() {
        if (email.getText().toString().trim().isEmpty()) {
            Toast.makeText(SignInActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
        } else if (password.getText().toString().trim().isEmpty()) {
            Toast.makeText(SignInActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
        } else {
            signInNewUser();
        }
    }

    private void signInNewUser() {
        progress.show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, password.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        preferenceManager.putString(Constants.KEY_PASSWORD, documentSnapshot.getString(Constants.KEY_PASSWORD));
                        Intent intent = new Intent(SignInActivity.this, HomeScreenActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        progress.dismiss();
                        Toast.makeText(SignInActivity.this, "Unable to sign in", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}