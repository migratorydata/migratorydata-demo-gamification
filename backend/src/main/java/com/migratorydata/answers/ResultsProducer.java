package com.migratorydata.answers;

import org.apache.kafka.clients.producer.*;
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

    private final KafkaProducer<String, byte[]> producer;
    private final int questionInterval;
    private final String topicResult;

    public ResultsProducer(Properties props, StatisticsProcessor statisticsProcessor) {
        this.topicResult = props.getProperty("topic.result");
        this.statisticsProcessor = statisticsProcessor;

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        questionInterval = Integer.valueOf(props.getProperty("question.interval", "20000"));

        producer =  new KafkaProducer<>(props);
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
        producer.close();
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

                    ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicResult, playerId, result.toString().getBytes());

                    record.headers().add("pushVersion", pushVersion);
                    record.headers().add("retained", retained); // 0 - false, 1 - true
                    record.headers().add("compression", compression); // 0 - no compression
                    record.headers().add("qos", qos); // 0 - Standard, 1 - Guaranteed

                    producer.send(record, new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata metadata, Exception exception) {
                            if (exception == null) {
                                statisticsProcessor.incrementResults();
                            }
                        }
                    });
                }
            }
        });
    }
}
