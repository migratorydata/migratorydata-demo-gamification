package com.migratorydata.leaderboard;

import org.apache.kafka.clients.producer.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderboardProcessor {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final TreeSet<PlayerScore> leaderBoard = new TreeSet<>();
    private final Map<String, Integer> playersScore = new HashMap<>();

    private final String topicTop;

    private final KafkaProducer<String, byte[]> producer;

    public LeaderboardProcessor(Properties props) {
        topicTop = props.getProperty("topic.top");

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
        response.put("top", top10());
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

    public JSONArray top10() {
        JSONArray jsonTopArray = new JSONArray();

        int topTen = 10;
        Iterator<PlayerScore> it = leaderBoard.descendingIterator();
        while (it.hasNext() && topTen-- > 0) {
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
}
