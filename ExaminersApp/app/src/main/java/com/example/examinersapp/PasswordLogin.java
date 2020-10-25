package com.example.examinersapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class PasswordLogin extends AppCompatActivity {

    private static final String TAG = "okay";
    TextInputLayout email_TIL,password_TIL;
    TextInputEditText email_TIET,password_TIET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_login);

        setTitle("Login");

        email_TIET = findViewById(R.id.email_TIET);
        password_TIET = findViewById(R.id.password_TIET);
        email_TIL = findViewById(R.id.email_TIL);
        password_TIL = findViewById(R.id.password_TIL);

        findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CheckIfEmailAndPasswordAreEmpty()){
                    String loginText = email_TIET.getText().toString().trim();
                    String passwordText = password_TIET.getText().toString().trim();
                    Log.d("demo",loginText+" "+passwordText);
                    Log.d(TAG, "onClick: calling async");
                    //new getTokeyAsync(loginText, passwordText).execute();
                }
            }
        });

    }

    private boolean CheckIfEmailAndPasswordAreEmpty() {
        if(email_TIET.getText().toString().equals("")){
            email_TIL.setError("Cannot be empty");
            return false;
        }else{
            email_TIL.setError("");
        }
        if(password_TIET.getText().toString().equals("")){
            password_TIL.setError("Cannot be empty");
            return false;
        }else{
            password_TIL.setError("");
        }
        return true;
    }
}