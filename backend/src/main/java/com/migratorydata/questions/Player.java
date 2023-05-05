package com.migratorydata.questions;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Player implements MigratoryDataListener {

    private final MigratoryDataClient client;
    private final ScheduledExecutorService executor;
    private final Random random = new Random();

    private final String playerId;

    private final String questionSubject;
    private final String resultSubject;
    private final String answerSubject;

    public Player(Properties config, int index, ScheduledExecutorService executor) {
        this.executor = executor;

        this.playerId = "player-bot-" + index;
        this.questionSubject = config.getProperty("topic.question");
        this.resultSubject = config.getProperty("topic.result") + "/" + playerId;
        this.answerSubject = config.getProperty("topic.answer");

        client = new MigratoryDataClient();

        client.setListener(this);

        client.setEntitlementToken(config.getProperty("entitlementToken", "some-token"));
        client.setServers(new String[] { config.getProperty("server", "localhost:8800")} );
        client.setEncryption(Boolean.valueOf(config.getProperty("encryption", "false")));
        client.setReconnectPolicy(MigratoryDataClient.CONSTANT_WINDOW_BACKOFF);
        client.setReconnectTimeInterval(5);

        client.subscribe(Arrays.asList(questionSubject, resultSubject));
    }

    public void start() {
        client.connect();
    }

    public void stop() {
        client.disconnect();
    }

    @Override
    public void onMessage(MigratoryDataMessage msg) {
        if (msg.getSubject().equals(questionSubject) && msg.getMessageType() == MigratoryDataMessage.MessageType.UPDATE) {
            JSONObject messageJson = new JSONObject(new String(msg.getContent()));

            String questionId = (String) messageJson.get("id");
            JSONArray answers = (JSONArray) messageJson.get("answers");
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("question_id", questionId);
            responseJSON.put("user_id", playerId);
            responseJSON.put("answer", String.valueOf(answers.get(random.nextInt(answers.length()))));

            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    client.publish(new MigratoryDataMessage(answerSubject, responseJSON.toString().getBytes()));
                }
            }, random.nextInt(10000), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onStatus(String status, String info) {
        System.out.println("Player with id: " + playerId + " ------ Status : " + status + " " + info);
    }
}
