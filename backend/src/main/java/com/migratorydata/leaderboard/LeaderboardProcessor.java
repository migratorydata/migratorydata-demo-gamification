package com.migratorydata.leaderboard;

import com.migratorydata.client.MigratoryDataClient;
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

    public LeaderboardProcessor(Properties config, MigratoryDataClient producer) {
        topicQuestion = config.getProperty("topic.question");
        topicTop = config.getProperty("topic.top");
        topicResult = config.getProperty("topic.result");

        this.producer = producer;
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
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void handleTopRequest(String playerId) {
        executor.execute(() -> {
            try {
                producer.publish(new MigratoryDataMessage(topicTop + "/" + playerId, encodeResponse(playerId).getBytes(),  "leaderboard-processor-handleTopRequest"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void handleReset() {
        executor.execute(() -> {
            try {
                JSONObject reset = new JSONObject();
                reset.put("reset", true);
                reset.put("message", "Game ended. A new game will start now. Wait for the questions.");
                producer.publish(new MigratoryDataMessage(topicQuestion, reset.toString().getBytes(), "leaderboard-processor-handleReset"));
                leaderBoard.clear();
                playersScore.clear();    
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
