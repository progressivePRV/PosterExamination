package com.example.examinersapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "okay";
    private static final int REQUEST_CODE_FOR_QRCODE_DETECTOR = 2222;
    static final String EXAMINER_KEY_FOR_PREFERENCE = "EXAMINER";
    static final  String TOKEN_KEY_FOR_PREFERENCE = "TOKEN_KEY";
    SharedPreferences preferences;
    SharedPreferences.Editor prefEditor;
    ProgressBar pb;
    TextInputLayout email_TIL,password_TIL;
    TextInputEditText email_TIET,password_TIET;
    MaterialButton login_btn;
    Button login_with_qr_btn;
    Button login_with_password_btn;
    TextView pb_txt;
    MotionLayout motionLayout;
    private static String PASSWORD_LOGIN="PASSWORD";
    private static String QR_CODE_LOGIN="QR_CODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Login");

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
        prefEditor = preferences.edit();
        //// checking if preference has token key
        String token = preferences.getString(TOKEN_KEY_FOR_PREFERENCE,"");
        if(!token.isEmpty()){
            GotoTeamEvaluationActivity();
        }
        ///
        email_TIET = findViewById(R.id.email_TIET);
        password_TIET = findViewById(R.id.password_TIET);
        email_TIL = findViewById(R.id.email_TIL);
        password_TIL = findViewById(R.id.password_TIL);
        login_btn = findViewById(R.id.login_button);
        pb = findViewById(R.id.progressBar_inQuestionActivity);
        login_with_password_btn = findViewById(R.id.login_with_password_btn);
        login_with_qr_btn = findViewById(R.id.scan_qr_code_btn_inMain);
        pb_txt = findViewById(R.id.pb_txt_inQuestionActivity);
        motionLayout = findViewById(R.id.motionLayout_inMain);

        //setting same height width for both buttons
        int height = login_with_qr_btn.getHeight();
        login_with_password_btn.setHeight(height);
        int width = login_with_qr_btn.getWidth();
        login_with_password_btn.setWidth(width);

        login_with_qr_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i =  new Intent(MainActivity.this,CameraPreview.class);
                startActivityForResult(i,REQUEST_CODE_FOR_QRCODE_DETECTOR);
            }
        });

        findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CheckIfEmailAndPasswordAreEmpty()){
                    String emailText = email_TIET.getText().toString().trim();
                    String passwordText = password_TIET.getText().toString().trim();
                    Log.d(TAG,emailText+" "+passwordText);
                    Log.d(TAG, "onClick: calling async");
                    new ExaminerLogin().execute(PASSWORD_LOGIN,emailText, passwordText);
                }
            }
        });

//        login_with_password_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Intent i =  new Intent(MainActivity.this,PasswordLogin.class);
////                startActivityForResult(i,REQUEST_CODE_FOR_PasswordLogin);
//            }
//        });

    }

    void GotoTeamEvaluationActivity(){
        Intent i =  new Intent(MainActivity.this,TeamsEvaluationActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_FOR_QRCODE_DETECTOR && resultCode == RESULT_OK){
            String result = data.getStringExtra("result");
            Log.d(TAG, "onActivityResult: what i got in result=>"+result);
            // call login here
            new ExaminerLogin().execute(QR_CODE_LOGIN,result);
        }
        else if (requestCode == REQUEST_CODE_FOR_QRCODE_DETECTOR && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "QR-code Scanner Cancled", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    class ExaminerLogin extends AsyncTask<String, Void, String >{

        String result = "",error = "";

        @Override
        protected void onPreExecute() {
            if (motionLayout.getCurrentState() == R.id.start){
                pb.setVisibility(View.VISIBLE);
                login_with_password_btn.setVisibility(View.GONE);
                login_with_qr_btn.setVisibility(View.GONE);
                pb_txt.setVisibility(View.VISIBLE);
            }else if (motionLayout.getCurrentState() == R.id.end){
                pb_txt.setVisibility(View.VISIBLE);
                pb.setVisibility(View.VISIBLE);
                email_TIL.setVisibility(View.INVISIBLE);
                password_TIL.setVisibility(View.INVISIBLE);
                login_btn.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            Request request = null;
            String ip = getResources().getString(R.string.ip);
            if(strings[0].equals(QR_CODE_LOGIN)){
                Log.d(TAG, "doInBackground: came to qr code login if block\n making request");
                String token_tobe_passed = strings[1];
                request = new Request.Builder()
                        .url(ip+"api/v1/users/login")
                        .header("Authorization", "Bearer "+ token_tobe_passed)
                        .build();

            }else if(strings[0].equals(PASSWORD_LOGIN)){

                String plainText = strings[1]+":"+strings[2];//username+":"+password;
                byte[] encodedValue = new byte[0];
                try {
                    encodedValue = plainText.getBytes("UTF-8");
                    String encodedString = Base64.encodeToString(encodedValue, Base64.NO_WRAP);
                    request = new Request.Builder()
                            .url(ip+"api/v1/users/login")
                            .header("Authorization", "Basic " + encodedString)
                            .build();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.d(TAG, "doInBackground: exception in encodeing authentication values");
                }

            }

            final OkHttpClient client = new OkHttpClient();

            try (Response response = client.newCall(request).execute()) {
                result = response.body().string();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            } catch (IOException e) {
                e.printStackTrace();
                error = result;
                Log.d(TAG, "doInBackground: error in login using qr code=>"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (error.isEmpty() && result.isEmpty()){
                Log.d(TAG, "onPostExecute: No response from sever");
                Toast.makeText(MainActivity.this, "No response from sever", Toast.LENGTH_SHORT).show();
                return;
            }
            if(error.isEmpty()){
                // parse the result
                try {
                    JSONObject root =  new JSONObject(result);
                    String role = root.getString("role");
                    if(!role.equals("examiner")){
                        Toast.makeText(MainActivity.this, "other than examiner no one is allowed in the App", Toast.LENGTH_SHORT).show();
                        return;
                        //finish();
                    }
                    prefEditor.putString(TOKEN_KEY_FOR_PREFERENCE,root.getString("token"));
                    prefEditor.putString(EXAMINER_KEY_FOR_PREFERENCE,result);
                    prefEditor.commit();
                    Log.d(TAG, "onPostExecute: was sucedfull in puting whole string in prefrences=>"+result);
                    GotoTeamEvaluationActivity();
                } catch (JSONException e) {
                    Log.d(TAG, "onPostExecute: error in parsing json from result for login");
                    e.printStackTrace();
                }
            }else{
                // parse the error
                Log.d(TAG, "onPostExecute: there was some error while login using qr code");
                Log.d(TAG, "onPostExecute: error=>"+error);
                try {
                    JSONObject root =  new JSONObject(result);
                    String er = root.getString("error");
                    if(er.contains("ReferenceError")){
                        Toast.makeText(MainActivity.this, "QR-code expired", Toast.LENGTH_SHORT).show();
                    }
                    //Toast.makeText(MainActivity.this, er, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (motionLayout.getCurrentState() == R.id.start){
                pb.setVisibility(View.INVISIBLE);
                login_with_password_btn.setVisibility(View.VISIBLE);
                login_with_qr_btn.setVisibility(View.VISIBLE);
                pb_txt.setVisibility(View.INVISIBLE);
            }else if (motionLayout.getCurrentState() == R.id.end){
                pb_txt.setVisibility(View.INVISIBLE);
                pb.setVisibility(View.INVISIBLE);
                email_TIL.setVisibility(View.VISIBLE);
                password_TIL.setVisibility(View.VISIBLE);
                login_btn.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (R.id.end == motionLayout.getCurrentState()){
            motionLayout.transitionToStart();
        }else {
            super.onBackPressed();
        }
    }
}