package com.example.examinersapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    Button login_with_qr_btn;
    Button login_with_password_btn;
    TextView pb_txt;
    private static String PASSWORD_LOGIN="PASSWORD";
    private static String QR_CODE_LOGIN="QR_CODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
        prefEditor = preferences.edit();
        //// checking if preference has token key
        String token = preferences.getString(TOKEN_KEY_FOR_PREFERENCE,"");
        if(!token.isEmpty()){
            GotoTeamEvaluationActivity();
        }
        ///
        pb = findViewById(R.id.progressBar_inQuestionActivity);
        login_with_password_btn = findViewById(R.id.login_with_password_btn);
        login_with_qr_btn = findViewById(R.id.scan_qr_code_btn_inMain);
        pb_txt = findViewById(R.id.pb_txt_inQuestionActivity);

        login_with_qr_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i =  new Intent(MainActivity.this,CameraPreview.class);
                startActivityForResult(i,REQUEST_CODE_FOR_QRCODE_DETECTOR);
            }
        });

        login_with_password_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

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
            pb.setVisibility(View.VISIBLE);
            login_with_password_btn.setVisibility(View.GONE);
            login_with_qr_btn.setVisibility(View.GONE);
            pb_txt.setVisibility(View.VISIBLE);
            new ExaminerLogin().execute(QR_CODE_LOGIN,result);
        }
        else if (requestCode == REQUEST_CODE_FOR_QRCODE_DETECTOR && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "QR-code Scanner Cancled", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    class ExaminerLogin extends AsyncTask<String, Void, String >{

        String result = "",error = "";

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
                        finish();
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
                    Toast.makeText(MainActivity.this, er, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            pb.setVisibility(View.GONE);
            login_with_qr_btn.setVisibility(View.VISIBLE);
            login_with_password_btn.setVisibility(View.VISIBLE);
            pb_txt.setVisibility(View.GONE);
            //ReferenceError: res is not defined
        }
    }
}