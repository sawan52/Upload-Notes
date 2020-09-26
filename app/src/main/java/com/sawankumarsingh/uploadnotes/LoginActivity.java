package com.sawankumarsingh.uploadnotes;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText email, password;
    private Button button;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);

        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signInUser();
            }
        });

    }

    private void signInUser() {

        String emailId = email.getText().toString();
        String passwordIs = password.getText().toString();

        mAuth.signInWithEmailAndPassword(emailId, passwordIs).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    sendUserToMainActivity();
                    Toast.makeText(LoginActivity.this, "You are logged in successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    if (task.getException() instanceof FirebaseNetworkException) {
                        Toast.makeText(LoginActivity.this, "Check Your internet Connection", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Something went wrong, Please try again!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    private void sendUserToMainActivity() {

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
