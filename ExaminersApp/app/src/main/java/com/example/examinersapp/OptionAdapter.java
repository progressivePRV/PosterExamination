package com.example.examinersapp;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.ViewHolder> {

    private static final String TAG = "okay";
    ArrayList<ConstraintLayout> rootViews =  new ArrayList<>();
    String[]  options;
    ToInteractWithQuestionActivty interact;
    boolean isClickable = true;

    public OptionAdapter(Context ctx, String[] options) {
        Log.d(TAG, "OptionAdapter: constructor called");
        this.interact = (ToInteractWithQuestionActivty) ctx;
        this.options = options;
    }

    @NonNull
    @Override
    public OptionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.option_list,parent,false);
        rootViews.add(view.findViewById(R.id.root_view_for_options));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: adding holder conainer of position=>"+position);
        //rootViews.add(holder.container);
        holder.option.setText(options[position]);
        holder.option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isClickable){
                    SelectedItem(position);
                    interact.OnOptionSelected(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //ConstraintLayout rootview;
        TextView option;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //rootview = itemView.findViewById(R.id.root_view_for_options);
            option = itemView.findViewById(R.id.tv_option_inOptionList);
        }
    }

    interface ToInteractWithQuestionActivty{
        void OnOptionSelected(int pos);
    }

    void SelectedItem(int x){
        for(int i=0;i<options.length;i++){
            if (x==i){
                rootViews.get(i).setBackgroundResource(R.color.colorAccent);
            }else{
                Log.d(TAG, "SelectedItem: backgroud color is set whilte for "+i+" postion");
                rootViews.get(i).setBackgroundColor(Color.WHITE);
            }
        }
    }

}
