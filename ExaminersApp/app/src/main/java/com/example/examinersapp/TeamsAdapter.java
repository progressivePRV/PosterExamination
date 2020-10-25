package com.example.examinersapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.zip.Inflater;

public class TeamsAdapter extends RecyclerView.Adapter<TeamsAdapter.ViewHolder> {

    private static final String TAG = "okay";
    ArrayList<TeamClass> teams = new ArrayList<>();

    public TeamsAdapter(ArrayList<TeamClass> teams) {
        this.teams = teams;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: called for teams list");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.team_list_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TeamClass t = teams.get(position);
        Log.d(TAG, "onBindViewHolder: binding teams=>" + t);
        holder.teamName.setText(t.name);
        holder.avgScore.setText(t.averageScore+"");
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView teamName, avgScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            teamName = itemView.findViewById(R.id.team_name_teamList);
            avgScore = itemView.findViewById(R.id.avg_score_teamList);
        }
    }

}
