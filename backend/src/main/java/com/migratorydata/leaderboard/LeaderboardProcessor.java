package com.migratorydata.leaderboard;

import org.apache.kafka.clients.producer.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LeaderboardProcessor {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final TreeSet<PlayerScore> leaderBoard = new TreeSet<>();
    private final Map<String, Integer> playersScore = new HashMap<>();

    private final String topicTop;
    private final String topicResult;

    private final KafkaProducer<String, byte[]> producer;

    public LeaderboardProcessor(Properties props) {
        topicTop = props.getProperty("topic.top");
        topicResult = props.getProperty("topic.result");

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer =  new KafkaProducer<>(props);
    }

    public void stop() {
        producer.close();
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
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicTop, playerId, encodeResponse(playerId).getBytes());
            producer.send(record);
        });
    }

    public void handleReset() {
        executor.execute(() -> {
            JSONObject reset = new JSONObject();
            reset.put("reset", true);
            reset.put("message", "Game ended. A new game will start now. Wait for the questions.");
            for (String player : playersScore.keySet()) {
                ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicResult, player, reset.toString().getBytes());
                producer.send(record);
            }
            leaderBoard.clear();
            playersScore.clear();
        });
    }
}
