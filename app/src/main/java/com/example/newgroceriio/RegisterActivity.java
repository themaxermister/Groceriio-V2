package com.example.newgroceriio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.newgroceriio.Models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText mRegisterNameInput, mRegisterEmailInput, mRegisterPasswordInput, mRegisterPasswordInput2;
    Button mRegisterBtn, mRegisterBackBtn;

    FirebaseAuth fAuth;
    User mUserObj;
    boolean mVerify; //verification boolean status
    FirebaseDatabase database;
    DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mRegisterNameInput = findViewById(R.id.registerNameInput);
        mRegisterEmailInput = findViewById(R.id.registerEmailInput);
        mRegisterPasswordInput = findViewById(R.id.registerPasswordInput);
        mRegisterPasswordInput2 = findViewById(R.id.registerPasswordInput2);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mRegisterBackBtn = findViewById(R.id.registerBackBtn);

        fAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference("users_data");

        Handler handler = new Handler();

        // Back button clicked, user brought back Login page (LoginActivity)
        mRegisterBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }
        });

        // Register button clicked
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // obtaining respective inputs from the user --> used to be checked if they are valid for creating an account
                String name = mRegisterNameInput.getText().toString().trim();
                String email = mRegisterEmailInput.getText().toString().trim();
                String password1 = mRegisterPasswordInput.getText().toString().trim();
                String password2 = mRegisterPasswordInput2.getText().toString().trim();

                // User object from User.java
                mUserObj = new User(name, email, mVerify);


                // check for validity of password below:
                // inputs cannot be empty, password matching, password at least 6 character etc.
                if (TextUtils.isEmpty(email)){
                    mRegisterEmailInput.setError("Email required.");
                    return;
                }

                if (TextUtils.isEmpty(password1)){
                    mRegisterPasswordInput.setError("Password required.");
                    return;
                }

                if (TextUtils.isEmpty(password2)){
                    mRegisterPasswordInput2.setError("Please re-enter password.");
                    return;
                }

                if (password1.length() < 6) {
                    mRegisterPasswordInput.setError("Password needs to be at least 6 characters.");
                    return;
                }

                if (!(password1.equals(password2))) {
                    mRegisterPasswordInput2.setError("Passwords do not match. Please re-enter your password.");
                    return;
                }

                // Account creation
                fAuth.createUserWithEmailAndPassword(email, password1).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(
                                    RegisterActivity.this,
                                    "User Created",
                                    Toast.LENGTH_SHORT)
                                    .show();

                            FirebaseUser mUserObj = fAuth.getCurrentUser();
                            updateUI(mUserObj);

                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    startActivity(new Intent(getApplicationContext(), RegisterSuccessActivity.class));
                                }
                            }, 1000);

                            // Send VERIFICATION LINK
                            fAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    //Toast message below that informs user that verification email sent
                                    Toast.makeText(RegisterActivity.this, "Verification email has been sent.", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
//                                    log.d(TAG,"onFailure: Email not sent" + e.getMessage());
                                    Log.d("this is a tag?", "onFailure: Email not sent" + e.getMessage());
                                }
                            });
                        }
                        else {
                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Error! " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

            }
        });
    }
    // Adding user information to database and redirect to login screen
    public void updateUI(FirebaseUser currentUser) {
        String keyId = mDatabase.push().getKey();
        mDatabase.child(keyId).setValue(mUserObj); //adding user info to database
    }
}