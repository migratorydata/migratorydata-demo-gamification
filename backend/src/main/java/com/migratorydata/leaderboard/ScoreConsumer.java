package com.migratorydata.leaderboard;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class ScoreConsumer implements MigratoryDataListener {

    private final LeaderboardProcessor leaderboardProcessor;
    private final MigratoryDataClient consumer;

    private final String topicGetTop;
    private final String topicResult;

    public ScoreConsumer(LeaderboardProcessor leaderboardProcessor, Properties config) {
        this.leaderboardProcessor = leaderboardProcessor;

        this.consumer = new MigratoryDataClient();

        this.consumer.setListener(this);

        this.consumer.setEntitlementToken(config.getProperty("entitlementToken", "some-token"));
        this.consumer.setServers(new String[] { config.getProperty("server", "localhost:8800")} );
        this.consumer.setEncryption(Boolean.valueOf(config.getProperty("encryption", "false")));
        this.consumer.setReconnectPolicy(MigratoryDataClient.CONSTANT_WINDOW_BACKOFF);
        this.consumer.setReconnectTimeInterval(5);

        topicResult = config.getProperty("topic.result");
        topicGetTop = config.getProperty("topic.gettop");

        consumer.subscribe(Arrays.asList(topicResult, topicGetTop));
    }

    public void start() {
        consumer.connect();
    }

    public void stop() {
        consumer.disconnect();
    }

//    @Override
//    public void run() {
//        try {
//            while (!closed.get()) {
//                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
//
//                for (ConsumerRecord<String, byte[]> record : records) {
////                    System.out.printf("%s-%d-%d, key = %s, value = %s --- %d %n", record.topic(), record.partition(), record.offset(), record.key(), new String(record.value()), record.timestamp());
//
//                    JSONObject data = new JSONObject(new String(record.value()));
//
//                    if (record.topic().equals(topicGettop)) {
//                        leaderboardProcessor.handleTopRequest(data.getString("user_id"));
//                    } else {
//                        try {
//                            leaderboardProcessor.updateScore(record.key(), data.getInt("points"));
//                        } catch (JSONException e) {}
//                    }
//                }
//            }
//        } catch (WakeupException e) {
//            // Ignore exception if closing
//            if (!closed.get()) throw e;
//        } finally {
//            consumer.close();
//        }
//    }

    @Override
    public void onMessage(MigratoryDataMessage migratoryDataMessage) {
        if (migratoryDataMessage.getMessageType() == MigratoryDataMessage.MessageType.UPDATE) {
            JSONObject data = new JSONObject(new String(migratoryDataMessage.getContent()));

            if (migratoryDataMessage.getSubject().equals(topicGetTop)) {
                leaderboardProcessor.handleTopRequest(data.getString("user_id"));
            } else {
                try {
                    String playerName = migratoryDataMessage.getSubject().substring(migratoryDataMessage.getSubject().indexOf("/", 1) + 1);
                    leaderboardProcessor.updateScore(playerName, data.getInt("points"));
                } catch (JSONException e) {
                }
            }
        }
    }

    @Override
    public void onStatus(String s, String s1) {

    }
}
