package com.migratorydata.questions;

import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataMessage;
import com.migratorydata.leaderboard.LeaderboardProcessor;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class QuestionProducer {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService liveExecutor = Executors.newSingleThreadScheduledExecutor();

    private final List<Question> questions;
    private final Properties config;
    private final String topicQuestion;
    private final String topicLive;
    private final LeaderboardProcessor leaderboardProcessor;

    private final AtomicInteger questionNumber = new AtomicInteger(0);
    private final AtomicInteger seekTimeSeconds = new AtomicInteger(0);

    private final MigratoryDataClient producer;

    public QuestionProducer(List<Question> questions, Properties config, LeaderboardProcessor leaderboardProcessor, MigratoryDataClient producer) {
        this.questions = questions;
        this.config = config;
        this.topicQuestion = config.getProperty("topic.question");
        this.topicLive = config.getProperty("topic.live");

        this.leaderboardProcessor = leaderboardProcessor;

        this.producer = producer;
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
                    } catch (InterruptedException e) { }
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
                MigratoryDataMessage message = new MigratoryDataMessage(topicLive, seekTime.toString().getBytes());

                producer.publish(message);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        producer.disconnect();
        executor.shutdown();
    }

    private void sendQuestion(int qNumber) {
        Question question = questions.get(qNumber);

        MigratoryDataMessage record = new MigratoryDataMessage(topicQuestion, question.toJson().getBytes(), "question-producer-sendQuestion");
        producer.publish(record);
    }
}
