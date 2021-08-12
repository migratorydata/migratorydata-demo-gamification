package com.migratorydata.leaderboard;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScoreConsumer implements Runnable {

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final LeaderboardProcessor leaderboardProcessor;
    private final KafkaConsumer<String, byte[]> consumer;

    private final Thread thread;
    private final String topicGettop;

    public ScoreConsumer(LeaderboardProcessor leaderboardProcessor, Properties props) {
        this.leaderboardProcessor = leaderboardProcessor;

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString().substring(0, 10));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, props.getProperty("max.poll.records", "500"));

        this.consumer = new KafkaConsumer<>(config);

        String topicResult = props.getProperty("topic.result");
        topicGettop = props.getProperty("topic.gettop");

        consumer.subscribe(Arrays.asList(topicResult, topicGettop));

        this.thread = new Thread(this);
        this.thread.setName("ScoreConsumer-" + thread.getId());
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

                for (ConsumerRecord<String, byte[]> record : records) {
//                    System.out.printf("%s-%d-%d, key = %s, value = %s --- %d %n", record.topic(), record.partition(), record.offset(), record.key(), new String(record.value()), record.timestamp());

                    JSONObject data = new JSONObject(new String(record.value()));

                    if (record.topic().equals(topicGettop)) {
                        leaderboardProcessor.handleTopRequest(data.getString("user_id"));
                    } else {
                        leaderboardProcessor.updateScore(record.key(), data.getInt("points"));
                    }
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
