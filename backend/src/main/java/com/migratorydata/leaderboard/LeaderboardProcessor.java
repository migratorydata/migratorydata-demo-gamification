package com.migratorydata.leaderboard;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LeaderboardProcessor {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final TreeSet<PlayerScore> leaderBoard = new TreeSet<>();
    private final Map<String, Integer> playersScore = new HashMap<>();

    private final String topicQuestion;
    private final String topicTop;
    private final String topicResult;

    private final MigratoryDataClient producer;

    public LeaderboardProcessor(Properties config) {
        topicQuestion = config.getProperty("topic.question");
        topicTop = config.getProperty("topic.top");
        topicResult = config.getProperty("topic.result");

        producer =  new MigratoryDataClient();
        producer.setListener(new MigratoryDataListener() {
            @Override
            public void onMessage(MigratoryDataMessage migratoryDataMessage) {
            }

            @Override
            public void onStatus(String s, String s1) {
            }
        });

        producer.setEntitlementToken(config.getProperty("entitlementToken", "some-token"));
        producer.setServers(new String[] { config.getProperty("server", "localhost:8800")} );
        producer.setEncryption(Boolean.valueOf(config.getProperty("encryption", "false")));
        producer.setReconnectPolicy(MigratoryDataClient.CONSTANT_WINDOW_BACKOFF);
        producer.setReconnectTimeInterval(5);

        producer.connect();
    }

    public void stop() {
        producer.disconnect();
    }

    public String encodeResponse(String playerName) {
        JSONObject response = new JSONObject();
        response.put("score", score(playerName));
        response.put("top", getTop());
        return response.toString();
    }

    private int score(String playerName) {
        Integer playerScore = playersScore.get(playerName);
        if (playerScore == null) {
            return 0;
        } else {
            return playerScore.intValue();
        }
    }

    public JSONArray getTop() {
        JSONArray jsonTopArray = new JSONArray();

        int topNumber = 15;
        Iterator<PlayerScore> it = leaderBoard.descendingIterator();
        while (it.hasNext() && topNumber-- > 0) {
            PlayerScore player = it.next();
            JSONObject playerScore = new JSONObject();
            playerScore.put("name", player.getName());
            playerScore.put("score", player.getScore());
            jsonTopArray.put(playerScore);
        }
        return jsonTopArray;
    }

    public void updateScore(String playerName, int points) {
        executor.execute(() -> {
            Integer lastScore = playersScore.get(playerName);
            if (lastScore == null) {
                playersScore.put(playerName, Integer.valueOf(points));
                leaderBoard.add(new PlayerScore(playerName, points));
            } else {
                if (points > 0) {
                    playersScore.put(playerName, Integer.valueOf(points + lastScore));
                    leaderBoard.remove(new PlayerScore(playerName, lastScore));
                    leaderBoard.add(new PlayerScore(playerName, points + lastScore));
                }
            }
        });
    }

    public void handleTopRequest(String playerId) {
        executor.execute(() -> {
            producer.publish(new MigratoryDataMessage(topicTop + "/" + playerId, encodeResponse(playerId).getBytes()));
        });
    }

    public void handleReset() {
        executor.execute(() -> {
            JSONObject reset = new JSONObject();
            reset.put("reset", true);
            reset.put("message", "Game ended. A new game will start now. Wait for the questions.");
            producer.publish(new MigratoryDataMessage(topicQuestion, reset.toString().getBytes()));
            leaderBoard.clear();
            playersScore.clear();
        });
    }
}
