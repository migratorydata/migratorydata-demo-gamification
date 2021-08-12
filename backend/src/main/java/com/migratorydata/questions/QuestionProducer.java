package com.migratorydata.questions;

import com.migratorydata.leaderboard.LeaderboardProcessor;
import org.apache.kafka.clients.producer.*;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class QuestionProducer {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService liveExecutor = Executors.newSingleThreadScheduledExecutor();

    private final List<Question> questions;
    private final Properties config;
    private final String topicQuestion;
    private final LeaderboardProcessor leaderboardProcessor;

    private final AtomicInteger questionNumber = new AtomicInteger(0);
    private final AtomicInteger seekTimeSeconds = new AtomicInteger(0);

    private final KafkaProducer<String, byte[]> producer;

    public QuestionProducer(List<Question> questions, Properties config, LeaderboardProcessor leaderboardProcessor) {
        this.questions = questions;
        this.config = config;
        this.topicQuestion = config.getProperty("topic.question");
        this.leaderboardProcessor = leaderboardProcessor;

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        this.producer =  new KafkaProducer<>(config);
    }

    public void start() {
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                int qNumber = questionNumber.getAndIncrement();
                sendQuestion(qNumber);

                if (qNumber + 1 >= questions.size()) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    questionNumber.getAndSet(0);
                    seekTimeSeconds.getAndSet(0);
                    if (leaderboardProcessor != null)
                        leaderboardProcessor.handleReset();
                }
            }
        }, Integer.valueOf(config.getProperty("question.interval", "20000")), Integer.valueOf(config.getProperty("question.interval", "20000")), TimeUnit.MILLISECONDS);

        liveExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                JSONObject seekTime = new JSONObject();
                seekTime.put("seek", seekTimeSeconds.getAndIncrement());
                ProducerRecord<String, byte[]> record = new ProducerRecord<>("live",0, "time", seekTime.toString().getBytes());

                producer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception == null) {
                            System.out.println("Published seek time: " + seekTime.toString());
                        }
                    }
                });
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        producer.close();
        executor.shutdown();
    }

    private void sendQuestion(int qNumber) {
        Question question = questions.get(qNumber);

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicQuestion,0, null, question.toJson().getBytes());

        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    System.out.println("Published question: " + question);
                }
            }
        });
    }
}
