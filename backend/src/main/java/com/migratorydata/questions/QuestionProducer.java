package com.migratorydata.questions;

import org.apache.kafka.clients.producer.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class QuestionProducer {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final List<Question> questions;
    private final Properties config;
    private final String topicQuestion;

    private final AtomicInteger questionNumber = new AtomicInteger(0);

    private final KafkaProducer<String, byte[]> producer;

    public QuestionProducer(List<Question> questions, Properties config) {
        this.questions = questions;
        this.config = config;
        this.topicQuestion = config.getProperty("topic.question");

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

        this.producer =  new KafkaProducer<>(config);
    }

    public void start() {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (questionNumber.get() >= questions.size()) {
                    questionNumber.set(0);
                }

                Question question = questions.get(questionNumber.getAndIncrement());

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
        }, 5000, Integer.valueOf(config.getProperty("question.interval", "20000")), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        producer.close();
        executor.shutdown();
    }
}
