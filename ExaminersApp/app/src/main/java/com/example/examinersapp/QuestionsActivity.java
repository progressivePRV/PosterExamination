package com.example.examinersapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QuestionsActivity extends AppCompatActivity {

    private static final String TAG = "okay";
    TextView teamName_tv;
    private SharedPreferences preferences;
    Gson gson = new Gson();
    ArrayList<Question> questions =  new ArrayList<>();
    TeamScore score =  new TeamScore();
    ProgressBar pb;
    TextView pb_txt;
    RecyclerView rv;
    QuestionAdapter rv_adapter;
    RecyclerView.LayoutManager rv_layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);

        //getting varlables
        teamName_tv = findViewById(R.id.team_name_in_QuestionActivity);
        pb = findViewById(R.id.progressBar_in_teamsEvaluation);
        pb_txt = findViewById(R.id.pb_txt_in_teamsEvaluation);
        rv = findViewById(R.id.rv_in_QuestionsActivity);
        rv_layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(rv_layoutManager);


        //getting data from intent

        TeamClass t = (TeamClass) getIntent().getSerializableExtra(TeamsEvaluationActivity.TEAM_KEY);
        Log.d(TAG, "onCreate: for from intent teamclass t=>"+t);
        teamName_tv.setText(t.name);
        score.teamId = t._id;

        //getting questions
        new GetQuestions().execute();

        findViewById(R.id.submit_score_btn_in_QuestionActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rv_adapter.CheckIfEveryQuestionIsAnswered()){
                    Log.d(TAG, "onClick: now you can send the score");
                    String to_send_the_score = gson.toJson(rv_adapter.getScore());
                    Log.d(TAG, "onClick: score to be sent is=>"+to_send_the_score);
                    new SendTheEvaluation().execute(to_send_the_score);
                }else {
                    Toast.makeText(QuestionsActivity.this, "Please give all "+questions.size()+" scores", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //get questions
    class GetQuestions extends AsyncTask<String, Void,String > {

        String result = "",error = "";//decision = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
            pb_txt.setVisibility(View.VISIBLE);
            pb_txt.setText("Getting Questions.....");
            //questionCardView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            String ip = getResources().getString(R.string.ip);
            String token = preferences.getString(MainActivity.TOKEN_KEY_FOR_PREFERENCE,"");
            Request request = new Request.Builder()
                    .url(ip+"api/v1/examiner/questions")
                    .header("Authorization", "Bearer "+ token)
                    .build();
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
                Toast.makeText(QuestionsActivity.this, "No response from sever", Toast.LENGTH_SHORT).show();
                return;
            }
            if(error.isEmpty()) {
                // parse the result
                try {
                    JSONObject root = new JSONObject(result);
                    JSONArray Qs = root.getJSONArray("results");
                    for(int i=0;i<Qs.length();i++){
                        JSONObject Q = Qs.getJSONObject(i);
                        Question q = gson.fromJson(Q.toString(),Question.class);
                        questions.add(q);
                    }
                    //start showing the question
                    rv_adapter =  new QuestionAdapter(score,questions);
                    rv.setAdapter(rv_adapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onPostExecute: error in parsing questions");
                }

            }else{
                Log.d(TAG, "onPostExecute: error=>"+error);
                try {
                    JSONObject root =  new JSONObject(result);
                    String er = root.getString("error");
                    Log.d(TAG, "onPostExecute: error in getting questions=>"+er);
                    Toast.makeText(QuestionsActivity.this, er, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            pb.setVisibility(View.INVISIBLE);
            pb_txt.setVisibility(View.INVISIBLE);
        }
    }


    // send the questions
    class SendTheEvaluation extends AsyncTask<String ,Void,String>{

        String result = "",error = "";//decision = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
            pb_txt.setVisibility(View.VISIBLE);
            pb_txt.setText("Sending Evaluation.....");
            rv.setVisibility(View.INVISIBLE);
            //questionCardView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            String ip = getResources().getString(R.string.ip);
            String token = preferences.getString(MainActivity.TOKEN_KEY_FOR_PREFERENCE,"");


            MediaType MEDIA_TYPE_JSON
                    = MediaType.parse("application/json");

            Request request = new Request.Builder()
                    .url(ip+"api/v1/examiner/score")
                    .header("Authorization", "Bearer "+ token)
                    .post(RequestBody.create(strings[0],MEDIA_TYPE_JSON))
                    .build();
            final OkHttpClient client = new OkHttpClient();
            try (Response response = client.newCall(request).execute()) {
                result = response.body().string();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            } catch (IOException e) {
                e.printStackTrace();
                error = result;
                Log.d(TAG, "doInBackground: error in sending scores=>"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (error.isEmpty() && result.isEmpty()){
                Log.d(TAG, "onPostExecute: No response from sever");
                Toast.makeText(QuestionsActivity.this, "No response from sever", Toast.LENGTH_SHORT).show();
                return;
            }
            if(error.isEmpty()) {
                // parse the result
                Log.d(TAG, "onPostExecute: after sending the scores result=>"+result);
                Toast.makeText(QuestionsActivity.this, "Evaluation Sent", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }else{
                Log.d(TAG, "onPostExecute: error=>"+error);
                try {
                    JSONObject root =  new JSONObject(result);
                    String er = root.getString("error");
                    Log.d(TAG, "onPostExecute: error in sending scores=>"+er);
                    Toast.makeText(QuestionsActivity.this, er, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            pb.setVisibility(View.INVISIBLE);
            pb_txt.setVisibility(View.INVISIBLE);
            rv.setVisibility(View.VISIBLE);
        }
    }


    //class for question
    class Question{
        int id;
        String question;

        public Question() {

        }
    }

}
