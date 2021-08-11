package com.migratorydata.answers;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnswersConsumer implements Runnable {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final StatisticsProcessor statisticsProcessor;

    private final KafkaConsumer<String, byte[]> consumer;
    private final Thread thread;
    private final String topicAnswer;
    private final String topicQuestion;

    private ResultsProducer resultsProducer;
    private ResultsProducer[] resultsHandlersQuestionUpdate;

    public AnswersConsumer(ResultsProducer resultsProducer, ResultsProducer[] resultsHandlersQuestionUpdate, Properties props, int index, StatisticsProcessor statisticsProcessor) {
        this.resultsProducer = resultsProducer;
        this.resultsHandlersQuestionUpdate = resultsHandlersQuestionUpdate;
        this.statisticsProcessor = statisticsProcessor;

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, props.getProperty(ConsumerConfig.GROUP_ID_CONFIG));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, props.getProperty("max.poll.records", "500"));

        consumer = new KafkaConsumer<>(config);

        topicQuestion = props.getProperty("topic.question");
        topicAnswer = props.getProperty("topic.answer");

        consumer.subscribe(Arrays.asList(topicQuestion, topicAnswer));

        this.thread = new Thread(this);
        this.thread.setName("AnswersConsumer-" + index);
        this.thread.setDaemon(true);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        closed.set(true);
        consumer.wakeup();
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                List<Answer> answers = new ArrayList<>();
                for (ConsumerRecord<String, byte[]> record : records) {
                    //System.out.printf("%s-%d-%d, key = %s, value = %s --- %d %n", record.topic(), record.partition(), record.offset(), record.key(), new String(record.value()), record.timestamp());

                    JSONObject content = new JSONObject(new String(record.value()));

                    if (record.topic().equals(topicAnswer)) {
                        answers.add(new Answer(content, record.key()));
                    } else {
                        for (ResultsProducer r : resultsHandlersQuestionUpdate) {
                            r.updateQuestion(content);
                        }
                    }
                }
                if (answers.size() > 0) {
                    resultsProducer.processAnswer(answers);
                    statisticsProcessor.addAnswers(answers.size());
                }
            }

        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!closed.get()) throw e;
        } finally {
            consumer.close();
        }
    }
}
