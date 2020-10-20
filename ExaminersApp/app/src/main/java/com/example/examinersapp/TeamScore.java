package com.example.examinersapp;

import java.util.ArrayList;

public class TeamScore {
    String teamId;
    ArrayList<QuestionMarks> scores =  new ArrayList<>();

    public TeamScore() {
    }

    @Override
    public String toString() {
        return "TeamScore{" +
                "teamId='" + teamId + '\'' +
                ", scores=" + scores +
                '}';
    }

    public String getTeamId() {
        return teamId;
    }

    public ArrayList<QuestionMarks> getScores() {
        return scores;
    }
}

class QuestionMarks{
    int id;
    int marks;

    public QuestionMarks() {
    }

    public QuestionMarks(int id, int marks) {
        this.id = id;
        this.marks = marks;
    }

    @Override
    public String toString() {
        return "QuestionMarks{" +
                "id='" + id + '\'' +
                ", marks='" + marks + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public int getMarks() {
        return marks;
    }
}
