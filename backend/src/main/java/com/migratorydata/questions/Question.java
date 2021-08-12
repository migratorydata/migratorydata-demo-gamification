package com.migratorydata.questions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class Question {

    String question;
    List<String> answers;
    String correctAnswer;

    public Question(String question, List<String> answers, String correctAnswer) {
        this.question = question;
        this.answers = answers;
        this.correctAnswer = correctAnswer;
    }

    public String toJson(int seekSeconds) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", UUID.randomUUID().toString().substring(0, 5));
        jsonObject.put("question", question);

        JSONArray answersArray = new JSONArray();
        for (String answer : answers) {
            answersArray.put(answer);
        }
        jsonObject.put("answers", answers);

        jsonObject.put("answer", correctAnswer);

        jsonObject.put("points", 100);

        jsonObject.put("seek", seekSeconds);

        return jsonObject.toString();
    }

    @Override
    public String toString() {
        return "Question{" +
                "question='" + question + '\'' +
                ", answers=" + answers +
                ", correctAnswer='" + correctAnswer + '\'' +
                '}';
    }
}
