package com.migratorydata.answers;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataMessage;
import com.migratorydata.leaderboard.ScoreConsumer;
import org.json.JSONObject;

import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ResultsProducer extends Thread {

    private final Queue<Runnable> executor = new ConcurrentLinkedQueue<>();

    private JSONObject currentQuestion;
    private long currentQuestionTimestamp;

    private final MigratoryDataClient producer;
    private final int questionInterval;
    private final String topicResult;

    private final ScoreConsumer scoreConsumer;

    public ResultsProducer(Properties config, ScoreConsumer scoreConsumer, MigratoryDataClient producer) {
        this.topicResult = config.getProperty("topic.result");
        this.scoreConsumer = scoreConsumer;

        questionInterval = Integer.valueOf(config.getProperty("question.interval", "20000"));

        this.producer = producer; new MigratoryDataClient();
    }

    public void run() {
        while (true) {
            Runnable r = executor.poll();
            if (r != null) {
                try {
                    r.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        producer.disconnect();
    }

    public void updateQuestion(JSONObject question) {
        executor.offer(() -> {
            currentQuestion = question;
            currentQuestionTimestamp = System.currentTimeMillis();
        });
    }

    public void processAnswer(List<Answer> answers) {
        executor.offer(() -> {
            if (currentQuestion == null) {
                return;
            }

            for (Answer a : answers) {
                JSONObject answer = a.answer;
                String playerId = a.playerId;

                String questionID = currentQuestion.getString("id");
                if ((System.currentTimeMillis() - currentQuestionTimestamp) > questionInterval) {
                    //System.out.println("Received an older answer:" + answer);
                    return;
                }

                if (questionID.equals(answer.getString("question_id"))) {
                    String questionAnswer = currentQuestion.getString("answer");
                    JSONObject result = new JSONObject();
                    result.put("user_id", playerId);
                    result.put("question_id", questionID);
                    if (questionAnswer.equals(answer.getString("answer"))) {
                        result.put("points", currentQuestion.get("points"));
                    } else {
                        result.put("points", 0);
                    }
                    result.put("answer", questionAnswer);

                    MigratoryDataMessage record = new MigratoryDataMessage(topicResult + "/" + playerId, result.toString().getBytes(), "results-producer-closure");

                    producer.publish(record);

                    scoreConsumer.onMessage(record);
                }
            }
        });
    }
}
