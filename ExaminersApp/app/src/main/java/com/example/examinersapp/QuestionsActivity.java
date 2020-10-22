package com.example.examinersapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QuestionsActivity extends AppCompatActivity {

    private static final String TAG = "okay";
    TextView teamName_tv,question_tv,question_count_tv;
    ListView options_lv;
    Button next_btn,prev_btn;
    private SharedPreferences preferences;
    Gson gson = new Gson();
    ArrayList<Question> questions =  new ArrayList<>();
    TeamScore score =  new TeamScore();
    ProgressBar pb;
    FrameLayout questionConatiner;
    TextView pb_txt;
    //int no_of_Q_answered = 0;
    int current_question_no=0;
    static String[]  options = {"Poor", "Fair", "Good", "Very Good", "Superior"};
    ArrayList<Boolean> isAnswered = new ArrayList<>();
//    RecyclerView rv;
//    QuestionAdapter rv_adapter;
//    RecyclerView.LayoutManager rv_layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);

        //getting varlables
        teamName_tv = findViewById(R.id.team_name_in_QuestionActivity);
        pb = findViewById(R.id.progressBar_inQuestionActivity);
        pb_txt = findViewById(R.id.pb_txt_inQuestionActivity);
        next_btn = findViewById(R.id.next_btn_inQuestionactivity);
        questionConatiner = findViewById(R.id.question_container_inQuestionActivity);
        prev_btn = findViewById(R.id.prev_btn_inQuestionactivity);
        question_tv = findViewById(R.id.question_tv_in_questionLayout);
        options_lv = findViewById(R.id.list_view_options_inQuestionLayout);
        question_count_tv = findViewById(R.id.question_count_tv_inQuextionActivity);


        //getting questions
        new GetQuestions().execute();

        //getting data from intent
        TeamClass t = (TeamClass) getIntent().getSerializableExtra(TeamsEvaluationActivity.TEAM_KEY);
        Log.d(TAG, "onCreate: for from intent teamclass t=>"+t);
        teamName_tv.setText(t.name);
        score.teamId = t._id;


        //setting options in listview
        ArrayAdapter<String> adapter;
        adapter =  new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1, options);
        options_lv.setAdapter(adapter);
        options_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //calling animation
                AnimateContainer();
                Log.d(TAG, "onItemClick: clicked on=>"+options[position]);
                //no_of_Q_answered++;
//                isAnswered.add(current_question_no,true);
                current_question_no++;
                Log.d(TAG, "onClick: after option selection current question no.=>"+current_question_no);
                //save the response in score
                QuestionMarks qm = new QuestionMarks(current_question_no,position);
                score.scores.add(qm);
//                if (current_question_no>=1){
//                    prev_btn.setVisibility(View.VISIBLE);
//                }
//                if (current_question_no==6){
//                    next_btn.setText("Submit");
//                }
                if (current_question_no==7){
                    Log.d(TAG, "onItemClick: score to be sent=>"+gson.toJson(score));//.toString());
                    new SendTheEvaluation().execute(gson.toJson(score));
                    Toast.makeText(QuestionsActivity.this, "done, good job prabhav", Toast.LENGTH_SHORT).show();
                }else{
                    // change to second question
                    question_tv.setText(questions.get(current_question_no).question);
                    question_count_tv.setText("Question "+(current_question_no+1)+" out of "+questions.size());
                }

            }
        });

//        next_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if(isAnswered.get(current_question_no)){
//                    current_question_no++;
//                }else{
//                    Toast.makeText(QuestionsActivity.this, "First Answer this question", Toast.LENGTH_SHORT).show();
//                }
//                Log.d(TAG, "onClick: after next btn current question no.=>"+current_question_no);
//            }
//        });
//
//        prev_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                current_question_no--;
//                if (current_question_no==0){
//                    prev_btn.setVisibility(View.GONE);
//                }
//                question_tv.setText(questions.get(current_question_no).question);
//                Log.d(TAG, "onClick: after prev btn current question no.=>"+current_question_no);
//            }
//        });

    }

    private void AnimateContainer() {
        Log.d(TAG, "AnimateContainer: current question constainer postion=>"+questionConatiner.getX());
        Float curr_x = questionConatiner.getX();
        int cure_width =questionConatiner.getWidth();
        // animate till full width
        Float animate_to_width = (float) cure_width;
        questionConatiner.animate().x(-animate_to_width).withEndAction(new Runnable() {
            @Override
            public void run() {
                questionConatiner.setX(animate_to_width+50f);
                questionConatiner.animate().x(curr_x).withEndAction(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(QuestionsActivity.this, "second Animation done", Toast.LENGTH_SHORT).show();
                    }
                });
//                Toast.makeText(QuestionsActivity.this, "animate done", Toast.LENGTH_SHORT).show();
            }
        });
        //questionConatiner.animate().
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
                        isAnswered.add(false); // setting that this question is not answered
                    }
                    //start showing the question
                    SetTheFirstQuestion();
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

    private void SetTheFirstQuestion() {
        questionConatiner.setVisibility(View.VISIBLE);
        question_tv.setText(questions.get(0).question);
        question_count_tv.setText("Question 1 out of "+questions.size());
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
            questionConatiner.setVisibility(View.INVISIBLE);
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
            //rv.setVisibility(View.VISIBLE);
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
