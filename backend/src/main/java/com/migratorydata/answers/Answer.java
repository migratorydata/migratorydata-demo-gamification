package com.migratorydata.answers;

import org.json.JSONObject;

public class Answer {

    public final JSONObject answer;
    public final String playerId;

    public Answer(JSONObject content, String playerId) {
        this.answer = content;
        this.playerId = playerId;
    }
}
