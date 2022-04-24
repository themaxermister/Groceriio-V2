package com.example.newgroceriio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {


    private static final String TAG = "LoginActivity";
    final int REQUEST_CHECK_SETTING = 1001;
    private TextInputEditText mLoginEmailInput, mLoginPasswordInput;
    private Button mLoginBtn, mLoginRegisterBtn;
    private Handler handler = new Handler();

    private FirebaseAuth fAuth;
    private FirebaseDatabase database;
    private DatabaseReference mDatabase;

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate LoginActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        checkDeviceLocationOn();

        mLoginEmailInput = findViewById(R.id.loginEmailInput);
        mLoginPasswordInput = findViewById(R.id.loginPasswordInput);
        mLoginBtn = findViewById(R.id.loginBtn);
        mLoginRegisterBtn = findViewById(R.id.loginRegisterBtn);
        fAuth = FirebaseAuth.getInstance();

        final String[] name = new String[1];

        // User clicks on Log In button
        // Log in prohibited if inputs are empty
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "mLoginBtn clicked");
                String email = mLoginEmailInput.getText().toString().trim();
                String password = mLoginPasswordInput.getText().toString().trim();


                if (TextUtils.isEmpty(email)){
                    mLoginEmailInput.setError("Email required.");
                    return;
                }

                if (TextUtils.isEmpty(password)){
                    mLoginPasswordInput.setError("Password required.");
                    return;
                }

                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {


                        // to prevent the error where there is a null reference which occurs when the account does not exist (unregistered)
                        if(fAuth.getCurrentUser() == null){
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Please register an account",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                        else{
                            // CASE 1: User log in details correct and account has been verified
                            if (task.isSuccessful() && fAuth.getCurrentUser().isEmailVerified()) {

                                Toast.makeText(
                                        LoginActivity.this,
                                        "User logged in successfully",
                                        Toast.LENGTH_SHORT)
                                        .show();

                                fAuth = FirebaseAuth.getInstance();
                                database = FirebaseDatabase.getInstance();
                                mDatabase = database.getReference("users_data"); // reference to users_data parent in our Firebase

                                String uid = fAuth.getCurrentUser().getUid();

                                // Retrieve User name from database
                                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for(DataSnapshot s: snapshot.getChildren()){
                                            Log.e(TAG, "onDataChange");
                                            String email = s.child("email").getValue(String.class);
                                            if(email.equals(fAuth.getCurrentUser().getEmail())){
                                                name[0] = s.child("name").getValue(String.class);
                                            }

                                        }
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        // uid and name of respective user is carried over to be used in MainActivity
                                        intent.putExtra("uid", uid);
                                        intent.putExtra("name", name[0]);
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.i(TAG,"Retrieved Data Cancelled");
                                    }
                                });
                            }
                            // CASE 2: User log in details correct and account has NOT been verified
                            // user will be redirected to RegisterFailActivity where there is an option to resend verification link
                            else if (task.isSuccessful() && !fAuth.getCurrentUser().isEmailVerified() ){

                                Toast.makeText(
                                        LoginActivity.this,
                                        "User email not verified",
                                        Toast.LENGTH_SHORT)
                                        .show();

                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        startActivity(new Intent(LoginActivity.this, RegisterFailActivity.class));
                                    }
                                }, 1000);   //1 seconds before starting activity
                            }
                            else {
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Error! " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                });
            }
        });

        mLoginRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            }
        });
    }

    private void checkDeviceLocationOn() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(LoginActivity.this, "Gps is on", Toast.LENGTH_SHORT).show();
                } catch (ApiException e) {
                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(LoginActivity.this, REQUEST_CHECK_SETTING);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                                Log.d(TAG, "Send intent exception");
                            }
                            break;

                        //when user device does not have location functionality
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }

            }
        });
    }
}