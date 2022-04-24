package com.example.newgroceriio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

// User will be brought here if they have registered successfully
public class RegisterSuccessActivity extends AppCompatActivity {
    Button mRegisterSuccessBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_success);

        mRegisterSuccessBtn = findViewById(R.id.registerSuccessBtn);

        // Clicking the "Back to Login" button brings the user back to the login page
        mRegisterSuccessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }
        });
    }
}