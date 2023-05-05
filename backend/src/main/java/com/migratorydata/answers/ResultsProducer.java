package com.migratorydata.answers;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;
import org.json.JSONObject;

import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ResultsProducer extends Thread {

    private final byte[] pushVersion = "6.0.0".getBytes();
    private final byte[] retained = new byte[]{(byte) 0};
    private final byte[] compression = new byte[]{(byte) 0};
    private final byte[] qos = new byte[]{(byte) 0};

    private final Queue<Runnable> executor = new ConcurrentLinkedQueue<>();
    private final StatisticsProcessor statisticsProcessor;

    private JSONObject currentQuestion;
    private long currentQuestionTimestamp;

    private final MigratoryDataClient producer;
    private final int questionInterval;
    private final String topicResult;

    public ResultsProducer(Properties config, StatisticsProcessor statisticsProcessor) {
        this.topicResult = config.getProperty("topic.result");
        this.statisticsProcessor = statisticsProcessor;

        questionInterval = Integer.valueOf(config.getProperty("question.interval", "20000"));

        producer =  new MigratoryDataClient();

        producer.setListener(new MigratoryDataListener() {
            @Override
            public void onMessage(MigratoryDataMessage migratoryDataMessage) {
            }

            @Override
            public void onStatus(String s, String s1) {
                if (s.equals(MigratoryDataClient.NOTIFY_PUBLISH_OK)) {
                    statisticsProcessor.incrementResults();
                }
            }
        });

        producer.setEntitlementToken(config.getProperty("entitlementToken", "some-token"));
        producer.setServers(new String[] { config.getProperty("server", "localhost:8800")} );
        producer.setEncryption(Boolean.valueOf(config.getProperty("encryption", "false")));
        producer.setReconnectPolicy(MigratoryDataClient.CONSTANT_WINDOW_BACKOFF);
        producer.setReconnectTimeInterval(5);

        producer.connect();
    }

    public void run() {
        while (true) {
            Runnable r = executor.poll();
            if (r != null) {
                r.run();
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

                    MigratoryDataMessage record = new MigratoryDataMessage(topicResult, result.toString().getBytes(), "results-producer-closure");

                    producer.publish(record);
                }
            }
        });
    }
}
