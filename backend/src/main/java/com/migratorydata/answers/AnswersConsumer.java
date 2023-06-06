package com.migratorydata.answers;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;
import org.json.JSONObject;

import java.util.*;

public class AnswersConsumer implements MigratoryDataListener {

    private final MigratoryDataClient consumer;
    private final String topicAnswer;
    private final String topicQuestion;

    private ResultsProducer resultsProducer;

    public AnswersConsumer(ResultsProducer resultsProducer, Properties config) {
        this.resultsProducer = resultsProducer;

        consumer = new MigratoryDataClient();

        this.consumer.setListener(this);

        this.consumer.setEntitlementToken(config.getProperty("entitlementToken", "some-token"));
        this.consumer.setServers(new String[] { config.getProperty("server", "localhost:8800")} );
        this.consumer.setEncryption(Boolean.valueOf(config.getProperty("encryption", "false")));
        this.consumer.setReconnectPolicy(MigratoryDataClient.CONSTANT_WINDOW_BACKOFF);
        this.consumer.setReconnectTimeInterval(5);

        topicQuestion = config.getProperty("topic.question");
        topicAnswer = config.getProperty("topic.answer");

        consumer.subscribe(Arrays.asList(topicQuestion, topicAnswer));
    }

    public void start() {
        consumer.connect();
    }

    public void stop() {
        consumer.disconnect();
    }

    @Override
    public void onMessage(MigratoryDataMessage migratoryDataMessage) {
        if (migratoryDataMessage.getMessageType() == MigratoryDataMessage.MessageType.UPDATE) {
            List<Answer> answers = new ArrayList<>();

            try {
                JSONObject content = new JSONObject(new String(migratoryDataMessage.getContent()));

                if (migratoryDataMessage.getSubject().equals(topicAnswer)) {
                    String playerId = content.getString("user_id");
    
                    answers.add(new Answer(content, playerId));
                } else {
                    resultsProducer.updateQuestion(content);
                }    
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (answers.size() > 0) {
                resultsProducer.processAnswer(answers);
            }
        }
    }

    @Override
    public void onStatus(String s, String s1) {

    }
}
