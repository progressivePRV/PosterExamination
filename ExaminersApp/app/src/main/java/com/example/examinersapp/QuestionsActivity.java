package com.example.examinersapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class QuestionsActivity extends AppCompatActivity implements OptionAdapter.ToInteractWithQuestionActivty {

    private static final String TAG = "okay";
    TextView teamName_tv,question_tv,question_count_tv;
    RecyclerView options_rv;
    RecyclerView.LayoutManager layoutManager;
    OptionAdapter optionAdapter;
    Button next_btn,prev_btn;
    private SharedPreferences preferences;
    Gson gson = new Gson();
    ArrayList<Question> questions =  new ArrayList<>();
    TeamScore score =  new TeamScore();
    ProgressBar pb;
    FrameLayout questionConatiner;
    TextView pb_txt;
    int current_question_no=0; // it will the question no. displayed on the screen
    static String[]  options = {"Poor", "Fair", "Good", "Very Good", "Superior"};
    ArrayList<Boolean> isAnswered = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
        setTitle("Team Evaluation");

        //getting varlables
        teamName_tv = findViewById(R.id.team_name_in_QuestionActivity);
        pb = findViewById(R.id.progressBar_inQuestionActivity);
        pb_txt = findViewById(R.id.pb_txt_inQuestionActivity);
        next_btn = findViewById(R.id.next_btn_inQuestionactivity);
        questionConatiner = findViewById(R.id.question_container_inQuestionActivity);
        prev_btn = findViewById(R.id.prev_btn_inQuestionactivity);
        question_tv = findViewById(R.id.question_tv_in_questionLayout);
        question_count_tv = findViewById(R.id.question_count_tv_inQuextionActivity);
        options_rv = findViewById(R.id.rv_options_inQuestionLayout);
        layoutManager =  new LinearLayoutManager(this);
        options_rv.setLayoutManager(layoutManager);
        optionAdapter =  new OptionAdapter(this,options);
        options_rv.setAdapter(optionAdapter);
        optionAdapter.notifyDataSetChanged();


        //getting questions
        new GetQuestions().execute();

        //getting data from intent
        TeamClass t = (TeamClass) getIntent().getSerializableExtra(TeamsEvaluationActivity.TEAM_KEY);
        Log.d(TAG, "onCreate: for from intent teamclass t=>"+t);
        teamName_tv.setText(t.name);
        score.teamId = t._id;


        next_btn.setOnClickListener(v -> {
            Log.d(TAG, "onClick: next button clicked in question activity");
            // if all answered then now user can submit anytime
            if (isAnswered.get(questions.size()-1)  && current_question_no == questions.size()){
                Log.d(TAG, "you will submit score here =>"+gson.toJson(score));
                new SendTheEvaluation().execute(gson.toJson(score));
                //Toast.makeText(this, "you will submit result now", Toast.LENGTH_SHORT).show();
                return;
            }
            // call animation for taking view to left ---> nextQuestion ---> getting view from right
            AnimateLeftNextQRight();
            //NextQuestion();
        });

        prev_btn.setOnClickListener(v -> {
            Log.d(TAG, "onClick: previous button clicked in question activity");
            // call animation for taking view to right ---> PreviousQuestion ---> getting view from left
            AnimateRightPreviousQLeft();
            //PreviousQuestion();
        });

    }

    private void PreviousQuestion() {
        // as user can only use previous if he has answered the question
        // so no need to check for going on previous question
        current_question_no--;
        if(current_question_no == 1){
            //hide prev button
            Log.d(TAG, "NextQuestion: you reached the last question");
            prev_btn.setVisibility(View.INVISIBLE);
        }else {
            next_btn.setText("Next");
            next_btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        // no  matter what when you press previous you will have option to click next
        next_btn.setVisibility(View.VISIBLE);
        // change the question
        if(isAnswered.get(current_question_no-1)){
            int ans = score.scores.get(current_question_no-1).marks;
            optionAdapter.SelectedItem(ans);
        }
        question_tv.setText(questions.get(current_question_no-1).question);
        question_count_tv.setText("Question "+current_question_no+" out of "+questions.size());
        // whenever previous is called user will not be able to submit the data
    }

    private void NextQuestion() {
        if (!isAnswered.get(current_question_no-1)){
            Toast.makeText(this, "first answer the current question", Toast.LENGTH_SHORT).show();
            return;
        } else if(current_question_no == questions.size()){
            Log.d(TAG, "NextQuestion: result to be sent=>"+gson.toJson(score));
            Toast.makeText(this, "you should submit the result now", Toast.LENGTH_SHORT).show();
            return;
        }else if(!isAnswered.get(current_question_no)){
            next_btn.setVisibility(View.GONE);
        } else if (isAnswered.get(current_question_no-1) && current_question_no == questions.size()-1 ){
            next_btn.setText("Submit");
            next_btn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }
        current_question_no++;
        if(current_question_no == questions.size()){
            Log.d(TAG, "NextQuestion: you reached the last question");
        }else{
            // make visible the previous button
            prev_btn.setVisibility(View.VISIBLE);
        }
        // change the question
        if(isAnswered.get(current_question_no-1)){
            int ans = score.scores.get(current_question_no-1).marks;
            optionAdapter.SelectedItem(ans);
        }
        else {
            Log.d(TAG, "NextQuestion: selected item with -1 is called");
            optionAdapter.SelectedItem(-1);
        }
        question_tv.setText(questions.get(current_question_no-1).question);
        question_count_tv.setText("Question "+current_question_no+" out of "+questions.size());
    }

    private void AnimateRightPreviousQLeft() {
        Log.d(TAG, "AnimateRightPreviousQLeft: called");
        /////untill animation next and previous button and even option in rv should not be clickable
        prev_btn.setClickable(false);
        next_btn.setClickable(false);
        optionAdapter.isClickable = false;
        /////
        Float curr_x = questionConatiner.getX();
        int cure_width =questionConatiner.getWidth();
        // animate till full width
        Float animate_to_width = (float) cure_width;
        questionConatiner.animate().x(animate_to_width+20F).withEndAction(new Runnable() {
            @Override
            public void run() {
                PreviousQuestion();
                questionConatiner.setX(-(animate_to_width+20f));
                questionConatiner.animate().x(curr_x).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        /// after animation is finish agian buttons and option in rv are clickable
                        prev_btn.setClickable(true);
                        next_btn.setClickable(true);
                        optionAdapter.isClickable = true;
                    }
                });
            }
        });
    }

    private void AnimateLeftNextQRight() {
        Log.d(TAG, "AnimateLeftNextQRight: called");
        /////untill animation next and previous button and even option in rv should not be clickable
        prev_btn.setClickable(false);
        next_btn.setClickable(false);
        optionAdapter.isClickable = false;
        /////
        Float curr_x = questionConatiner.getX();
        int cure_width =questionConatiner.getWidth();
        // animate till full width
        Float animate_to_width = (float) cure_width;
        questionConatiner.animate().x(-animate_to_width).withEndAction(new Runnable() {
            @Override
            public void run() {
                NextQuestion();
                questionConatiner.setX(animate_to_width+50f);
                questionConatiner.animate().x(curr_x).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        /// after animation is finish agian buttons and option in rv are clickable
                        prev_btn.setClickable(true);
                        next_btn.setClickable(true);
                        optionAdapter.isClickable = true;
                    }
                });
            }
        });
    }

    @Override
    public void OnOptionSelected(int pos) {
        Log.d(TAG, "onItemClick: clicked on=>"+options[pos]);
        //save the response in score
        QuestionMarks qm = new QuestionMarks(current_question_no,pos);
        // this question should be recorded in scores at currrent_question_no -1 as scores start with 0 index
        score.scores.set(current_question_no-1,qm);
        // updating isAnswered varable
        isAnswered.set(current_question_no-1,true);
        if (isAnswered.get(questions.size()-1) && current_question_no==questions.size()){
            next_btn.setVisibility(View.VISIBLE);
            next_btn.setText("Submit");
            next_btn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }else{
            //NextQuestion();
            AnimateLeftNextQRight();
        }
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
                Log.d(TAG, "doInBackground: error in getting questions=>"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (error.isEmpty() && result.isEmpty()){
                Log.d(TAG, "onPostExecute: No response from sever");
                Toast.makeText(QuestionsActivity.this, "No response from sever", Toast.LENGTH_SHORT).show();
                finish();
                //return;
            }else {
                if(error.isEmpty()) {
                    // parse the result
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONArray Qs = root.getJSONArray("results");
                        for(int i=0;i<Qs.length();i++){
                            JSONObject Q = Qs.getJSONObject(i);
                            Question q = gson.fromJson(Q.toString(),Question.class);
                            questions.add(q);
                            QuestionMarks qm = new QuestionMarks(i+1,-1);
                            score.scores.add(i,qm);
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
                        JSONObject error = root.getJSONObject("error");
                        String errorName = error.getString("name");
                        if (errorName.contains("TokenExpired")){
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                        Log.d(TAG, "onPostExecute: error in getting questions=>"+er);
                        //Toast.makeText(QuestionsActivity.this, er, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
        current_question_no = 1;
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
                // return;
            }else{
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
                        JSONObject error = root.getJSONObject("error");
                        String errorName = error.getString("name");
                        Log.d(TAG, "onPostExecute: before checking for token expired string");
                        if (errorName.contains("TokenExpired")){
                            Log.d(TAG, "onPostExecute: token expired check pass");
                            Toast.makeText(QuestionsActivity.this, "Session Expired", Toast.LENGTH_SHORT).show();
//                            Intent i =  new Intent(QuestionsActivity.this,MainActivity.class);
//                            i.putExtra(MainActivity.JUST_FOR_LOGIN,true);
//                            Log.d(TAG, "onPostExecute: set just for login true");
//                            preferences.edit().clear().commit();
//                            startActivity(i);
                            setResult(RESULT_OK);
                            finish();
                        }else{
                            Toast.makeText(QuestionsActivity.this, er, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            pb.setVisibility(View.INVISIBLE);
            pb_txt.setVisibility(View.INVISIBLE);
            questionConatiner.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Do you want to stop Evaluation?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
        //super.onBackPressed();
    }

    //class for question
    class Question{
        int id;
        String question;

        public Question() {

        }
    }

}
