package com.example.examinersapp;

import java.io.Serializable;
import java.util.ArrayList;

public class TeamClass implements Serializable {
    String _id;
    String name;
    ArrayList<String> members;
    String averageScore;

    public TeamClass() {
    }

    @Override
    public String toString() {
        return "TeamClass{" +
                "id='" + _id + '\'' +
                ", name='" + name + '\'' +
                ", members=" + members +
                ", averageScore='" + averageScore + '\'' +
                '}';
    }
}
