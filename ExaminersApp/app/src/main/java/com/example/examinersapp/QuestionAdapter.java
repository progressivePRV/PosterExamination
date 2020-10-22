//package com.example.examinersapp;
//
//import android.graphics.Color;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.RadioGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.ArrayList;
//
//public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {
//
//    private static final String TAG = "okay";
//    TeamScore score =  new TeamScore();
//    ArrayList<QuestionsActivity.Question> questions =  new ArrayList<>();
//    ArrayList<ConstraintLayout> rootViews =  new ArrayList<ConstraintLayout>();
//    ArrayList<Boolean> isAnswered = new ArrayList<>();
//
//    public QuestionAdapter(TeamScore score, ArrayList<QuestionsActivity.Question> questions) {
//        this.score = score;
//        Log.d(TAG, "QuestionAdapter: constructor score=>"+score);
//        this.questions = questions;
//        for (int i=0;i<7;i++){
//            isAnswered.add(false);
//            QuestionMarks qm = new QuestionMarks(i+1,0);
//            score.scores.add(qm);
//        }
//    }
//
//    @NonNull
//    @Override
//    public QuestionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_list_layout_for_rv,parent,false);
//        rootViews.add(view.findViewById(R.id.container_for_question_in_question_list));
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull QuestionAdapter.ViewHolder holder, int position) {
//        Log.d(TAG, "onBindViewHolder: adding holder conainer of position=>"+position);
//        //rootViews.add(holder.container);
//        QuestionsActivity.Question q = questions.get(position);
//        holder.question_tv.setText(q.question);
//        holder.marks.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                isAnswered.set(position,true);
//                QuestionMarks qm;
//                switch (checkedId){
//                    case R.id.radio_btn0:
//                        qm = new QuestionMarks(position+1,0);
//                        score.scores.set(position,qm);
//                        break;
//                    case R.id.radio_btn1:
//                        qm = new QuestionMarks(position+1,1);
//                        score.scores.set(position,qm);
//                        break;
//                    case R.id.radio_btn2:
//                        qm = new QuestionMarks(position+1,2);
//                        score.scores.set(position,qm);
//                        break;
//                    case R.id.radio_btn3:
//                        qm = new QuestionMarks(position+1,3);
//                        score.scores.set(position,qm);
//                        break;
//                    case R.id.radio_btn4:
//                        qm = new QuestionMarks(position+1,4);
//                        score.scores.set(position,qm);
//                        break;
//                }
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return questions.size();
//    }
//
//    public class ViewHolder extends RecyclerView.ViewHolder {
//        RadioGroup marks;
//        TextView question_tv;
//        ConstraintLayout container;
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            question_tv = itemView.findViewById(R.id.question_tv_in_questionLayout);
//            marks =itemView.findViewById(R.id.radio_group_inquestionLayout);
//            container = itemView.findViewById(R.id.container_for_question_in_question_list);
//        }
//    }
//
//    boolean CheckIfEveryQuestionIsAnswered(){
//        if (rootViews.size() != isAnswered.size() || rootViews.size()!=questions.size()){
//            Log.d(TAG, "CheckIfEveryQuestionIsAnswered: rootviews Size=>"+rootViews.size());
//            Log.d(TAG, "CheckIfEveryQuestionIsAnswered: isAnswered size=>"+isAnswered.size());
//            Log.d(TAG, "CheckIfEveryQuestionIsAnswered: questions size=>"+questions.size());
//            Log.d(TAG, "CheckIfEveryQuestionIsAnswered: came in first if in check_if_every_question_is_answered");
//            return false;
//        }
//        for(int i=0;i<isAnswered.size();i++){
//            if (!isAnswered.get(i)){
//                rootViews.get(i).setBackgroundColor(Color.RED);
//                return false;
//            }else{
//                rootViews.get(i).setBackgroundResource(R.color.colorAccent);
//            }
//        }
//        return true;
//    }
//
//    TeamScore getScore(){
//        return score;
//    }
//}
