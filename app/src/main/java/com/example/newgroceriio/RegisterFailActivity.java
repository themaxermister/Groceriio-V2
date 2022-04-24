package com.example.newgroceriio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

// User brought here only if user attempts to log in  with an unverified account
public class RegisterFailActivity extends AppCompatActivity {
    Button mRegFailSendBtn, mRegFailBackBtn;
    FirebaseAuth fAuth;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_fail);

        mRegFailSendBtn = findViewById(R.id.regFailResendBtn); // "Resend Verification Email" button
        mRegFailBackBtn = findViewById(R.id.regFailBackBtn); // BACK button
        fAuth = FirebaseAuth.getInstance();

        // Back button sends user back to login page (LoginActivity)
        mRegFailBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterFailActivity.this, LoginActivity.class));
            }
        });

        // When "Resend Verification Email" button clicked, Firebase will send another verification link to user email
        mRegFailSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRegFailSendBtn.setEnabled(false);

                // Send VERIFICATION LINK code below:
                mHandler.postDelayed(mToastRunnable, 5000); // "Resend Verification Email" button unclickable for 5 seconds to prevent spamming of email
                fAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Toast message below that informs user that verification email sent
                        Toast.makeText(RegisterFailActivity.this, "Verification email has been sent.", Toast.LENGTH_SHORT).show();
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                                    log.d(TAG,"onFailure: Email not sent" + e.getMessage());
                        Log.d("this is a tag?", "onFailure: Email not sent" + e.getMessage());
                    }
                });
            }
        });

    }
    private Runnable mToastRunnable = new Runnable() {
        @Override
        public void run() {
            // "Resend Verification Email" button unclickable for 5 seconds to prevent spamming of email
            mRegFailSendBtn.setEnabled(true);
        }
    };
}