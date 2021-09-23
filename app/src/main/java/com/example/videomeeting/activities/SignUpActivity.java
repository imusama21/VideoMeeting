package com.example.videomeeting.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.videomeeting.R;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ImageView back;
    private EditText firstName, lastName, email, password, confirmPassword;
    private MaterialButton btnSignUp;
    private TextView signInText;
    private KProgressHUD progress;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        bindViews();
        setData();
        setListener();
    }

    private void setData() {
        //Progress Bar
        progress = KProgressHUD.create(SignUpActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait...")
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        //Shared Preference
        preferenceManager = new PreferenceManager(getApplicationContext());
    }

    private void bindViews() {
        back = findViewById(R.id.back);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        signInText = findViewById(R.id.signInText);
    }

    private void setListener() {
        back.setOnClickListener(view -> onBackPressed());

        btnSignUp.setOnClickListener(view -> validateData());

        signInText.setOnClickListener(view -> onBackPressed());
    }

    private void validateData() {
        if (firstName.getText().toString().trim().isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Enter first name", Toast.LENGTH_SHORT).show();
        } else if (lastName.getText().toString().trim().isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Enter last name", Toast.LENGTH_SHORT).show();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
            Toast.makeText(SignUpActivity.this, "Enter valid email", Toast.LENGTH_SHORT).show();
        } else if (password.getText().toString().trim().isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
        } else if (confirmPassword.getText().toString().trim().isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Enter confirm password", Toast.LENGTH_SHORT).show();
        } else if (!password.getText().toString().equals(confirmPassword.getText().toString())) {
            Toast.makeText(SignUpActivity.this, "Password & confirm password must be same", Toast.LENGTH_SHORT).show();
        } else {
            signUpNewUser();
        }
    }

    private void signUpNewUser() {
        progress.show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, firstName.getText().toString());
        user.put(Constants.KEY_LAST_NAME, lastName.getText().toString());
        user.put(Constants.KEY_EMAIL, email.getText().toString());
        user.put(Constants.KEY_PASSWORD, password.getText().toString());
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    progress.dismiss();
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, firstName.getText().toString());
                    preferenceManager.putString(Constants.KEY_LAST_NAME, lastName.getText().toString());
                    preferenceManager.putString(Constants.KEY_EMAIL, email.getText().toString());
                    Intent intent = new Intent(SignUpActivity.this, HomeScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(SignUpActivity.this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                });
    }
}