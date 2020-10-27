package com.example.examinersapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TeamsEvaluationActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_FOR_QRCODE_DETECTOR = 3333;
    private static final String TAG = "okay";
    private static final String GET_SINGLE_TEAM = "SINGLE_TEAM";
    static final String TEAM_KEY = "TEAM";
    private static final int REQUEST_CODE_FOR_QUESTION_ACTIVITY = 4444;
    private static final String GET_ALL_TEAM = "ALL_TEAMS";
    TextView welcome_tv,teamName_tv,avg_score_tv;
    RecyclerView rv;
    RecyclerView.Adapter rv_adapter;
    RecyclerView.LayoutManager rv_layoutManager;
    SharedPreferences preferences;
    ProgressBar pb;
    TextView pb_txt;
    ArrayList<TeamClass> teams =  new ArrayList<>();
    Gson gson =  new Gson();


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.team_evaluation_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.logout_in_teamsEvaluation:
                LogoutFromTheApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams_evaluation);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
        setTitle("Score Board");

        //getting varaiables
        pb = findViewById(R.id.progressBar_inTeamEvaluation);
        pb_txt = findViewById(R.id.pb_txt_inTeamEvaluation);
        avg_score_tv = findViewById(R.id.avg_score_tv_in_teamsEvaluation);
        teamName_tv = findViewById(R.id.team_names_tv_in_teamsEvaluation);
        welcome_tv = findViewById(R.id.welcom_tv_in_TeamEvaluation);
        rv = findViewById(R.id.rv_in_teamEvaluation);
        rv_layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(rv_layoutManager);
        //adapter is set when team details arrive


        // setting datas
        String s = preferences.getString(MainActivity.EXAMINER_KEY_FOR_PREFERENCE,"");
//        Log.d(TAG, "onCreate: s from preferences=>"+s);
        Examiner examiner = gson.fromJson(s,Examiner.class);
        welcome_tv.setText("Welcome "+ examiner.firstName+" "+examiner.lastName);

        findViewById(R.id.evaluate_btn_in_teamEvaluation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i =  new Intent(TeamsEvaluationActivity.this,CameraPreview.class);
                startActivityForResult(i,REQUEST_CODE_FOR_QRCODE_DETECTOR);
            }
        });

        //getting all teams details
        CallingGetTeamDetails(GET_ALL_TEAM,"");

    }


    protected void CallingGetTeamDetails(String decision,String token) {
        // showing progress bar
        teamName_tv.setVisibility(View.INVISIBLE);
        avg_score_tv.setVisibility(View.INVISIBLE);
        rv.setVisibility(View.INVISIBLE);
        pb.setVisibility(View.VISIBLE);
        pb_txt.setVisibility(View.VISIBLE);
        new GetTeamsDetails(decision).execute(token);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FOR_QRCODE_DETECTOR && resultCode == RESULT_OK){
            String result = data.getStringExtra("result");
            Log.d(TAG, "onActivityResult: result from calling qr code detector for teams");
            // call team id authentication here
            CallingGetTeamDetails(GET_SINGLE_TEAM,result);
        }else if (requestCode == REQUEST_CODE_FOR_QRCODE_DETECTOR && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "QR-code Scanner Cancled", Toast.LENGTH_SHORT).show();
        }else if(REQUEST_CODE_FOR_QUESTION_ACTIVITY == requestCode && resultCode == RESULT_OK){
            //get all team details as it got updated for a team
            CallingGetTeamDetails(GET_ALL_TEAM,"");
        }else if (REQUEST_CODE_FOR_QUESTION_ACTIVITY == requestCode && resultCode == RESULT_CANCELED){
            //Toast.makeText(TeamsEvaluationActivity.this, "Session expired", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onActivityResult: it came back from question activity with result cancel");
            //LogoutFromTheApp();
        }
    }

    class GetTeamsDetails extends AsyncTask<String, Void, String>{

        String result = "",error = "",decision = "";

        public GetTeamsDetails(String decision) {
            this.decision = decision;
            Log.d(TAG, "GetTeamsDetails: decision is=>"+decision);
        }

        @Override
        protected String doInBackground(String... strings) {
            String ip = getResources().getString(R.string.ip);
            String token = preferences.getString(MainActivity.TOKEN_KEY_FOR_PREFERENCE,"");

            Request.Builder requestbuilder = new Request.Builder()
                    .url(ip+"api/v1/examiner/teams")
                    .header("Authorization", "Bearer "+ token);
            if(decision.equals(GET_SINGLE_TEAM)){
                requestbuilder.addHeader("teamToken",strings[0]);
            }
            Request request = requestbuilder.build();
            final OkHttpClient client = new OkHttpClient();
            try (Response response = client.newCall(request).execute()) {
                result = response.body().string();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            } catch (IOException e) {
                e.printStackTrace();
                error = result;
                Log.d(TAG, "doInBackground: error in geting teams detail=>"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (error.isEmpty() && result.isEmpty()){
                Log.d(TAG, "onPostExecute: No response from sever");
                Toast.makeText(TeamsEvaluationActivity.this, "No response from sever", Toast.LENGTH_SHORT).show();
                //return;
            }
            if(error.isEmpty()){
                // parse the result
                if (decision.equals(GET_SINGLE_TEAM)){
                    Log.d(TAG, "onPostExecute: will pare the result for a single team=>"+result);
                    TeamClass t = gson.fromJson(result,TeamClass.class);
                    ///////////// got to questions activity
                    Log.d(TAG, "onPostExecute: calling question activity");
                    Intent i =  new Intent(TeamsEvaluationActivity.this,QuestionsActivity.class);
                    i.putExtra(TEAM_KEY,t);
                    startActivityForResult(i,REQUEST_CODE_FOR_QUESTION_ACTIVITY);
                    /////////////
                }else if(decision.equals(GET_ALL_TEAM)){
                    Log.d(TAG, "onPostExecute: will parse the result for team detaisl=>"+result);
                    Type teamsType = new TypeToken<ArrayList<TeamClass>>(){}.getType();
                    teams = gson.fromJson(result, teamsType);
                    // sort teams first
                    Collections.sort(teams, new Comparator<TeamClass>() {
                        @Override
                        public int compare(TeamClass o1, TeamClass o2) {
                            return o2.averageScore.compareTo(o1.averageScore);
                        }
                    });

                    rv_adapter =  new TeamsAdapter(teams);
                    rv.setAdapter(rv_adapter);
                }else{
                    Log.d(TAG, "onPostExecute: it didn't went for get all teams or get single team");
                }

            }else{
                // parse the error
                Log.d(TAG, "onPostExecute: there was some error while getting teams ");
                Log.d(TAG, "onPostExecute: error=>"+error);
                try {
                    JSONObject root =  new JSONObject(result);
                    String er = root.getString("error");
                    if (root.has("errorOn")){
                        Toast.makeText(TeamsEvaluationActivity.this, "Team QR-code expired", Toast.LENGTH_SHORT).show();
                    }else if(er.contains("TokenExpired")){
                            Toast.makeText(TeamsEvaluationActivity.this, "Session expired", Toast.LENGTH_SHORT).show();
                            LogoutFromTheApp();
                    }else{
                        Toast.makeText(TeamsEvaluationActivity.this, er, Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "onPostExecute: error in getting team=>"+er);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            teamName_tv.setVisibility(View.VISIBLE);
            avg_score_tv.setVisibility(View.VISIBLE);
            rv.setVisibility(View.VISIBLE);
            pb.setVisibility(View.INVISIBLE);
            pb_txt.setVisibility(View.INVISIBLE);
        }
    }

    void LogoutFromTheApp(){
        preferences.edit().clear().commit();
        //preferences.edit().commit();
        Intent i =  new Intent(this,MainActivity.class);
        startActivity(i);
        finish();
    }
}