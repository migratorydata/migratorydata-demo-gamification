package com.migratorydata;

import com.migratorydata.answers.AnswersConsumer;
import com.migratorydata.answers.ResultsProducer;
import com.migratorydata.client.MigratoryDataClient;
import com.migratorydata.client.MigratoryDataListener;
import com.migratorydata.client.MigratoryDataMessage;
import com.migratorydata.questions.PlayersSimulator;
import com.migratorydata.questions.Question;
import com.migratorydata.questions.QuestionLoader;
import com.migratorydata.questions.QuestionProducer;
import com.migratorydata.leaderboard.ScoreConsumer;
import com.migratorydata.leaderboard.LeaderboardProcessor;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class Main {

    private static LeaderboardProcessor leaderboardProcessor;
    private static ScoreConsumer scoreConsumer;
    private static PlayersSimulator playersSimulator;
    private static QuestionProducer questionProducer;

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("USAGE: java -jar backend.jar server_address token subjects_prefix");
            System.exit(1);
        }

        Properties config = loadConfigProperties(args[0], args[1], args[2]);

        MigratoryDataClient producer = new MigratoryDataClient();

        producer.setListener(new MigratoryDataListener() {
            @Override
            public void onMessage(MigratoryDataMessage migratoryDataMessage) {
            }

            @Override
            public void onStatus(String s, String s1) {
                System.out.println("Gamification-" + s + " - " + s1);
            }
        });

        producer.setEntitlementToken(config.getProperty("entitlementToken", "some-token"));
        producer.setServers(new String[] { config.getProperty("server", "localhost:8800")} );
        producer.setEncryption(Boolean.valueOf(config.getProperty("encryption", "false")));
        producer.setReconnectPolicy(MigratoryDataClient.CONSTANT_WINDOW_BACKOFF);
        producer.setReconnectTimeInterval(5);

        producer.connect();

        leaderboardProcessor = new LeaderboardProcessor(config, producer);

        scoreConsumer = new ScoreConsumer(leaderboardProcessor, config);
        scoreConsumer.start();

        ResultsProducer resultsProducers = new ResultsProducer(config, scoreConsumer, producer);
        resultsProducers.start();
        AnswersConsumer answersConsumers = new AnswersConsumer(resultsProducers, config);
        answersConsumers.start();

        // Questions generator start
        playersSimulator = new PlayersSimulator(config);
        playersSimulator.start();

        List<Question> questions = QuestionLoader.loadQuestion();


        questionProducer = new QuestionProducer(questions, config, leaderboardProcessor, producer);
        questionProducer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                playersSimulator.stop();
                questionProducer.stop();
                scoreConsumer.stop();
                leaderboardProcessor.stop();

                answersConsumers.stop();
                resultsProducers.close();
            }
        });
    }

    public static Properties loadConfigProperties(String server, String token, String subjectPrefix) {
        Properties props = new Properties();
        props.put("server", server);
        props.put("entitlementToken", token);
        props.put("simulation.clients.number", System.getProperty("simulation.clients.number", "10"));
        props.put("question.interval", System.getProperty("question.interval", "20000"));
        props.put("topic.question", System.getProperty("topic.question", subjectPrefix + "/question"));
        props.put("topic.answer", System.getProperty("topic.answer", subjectPrefix + "/answer"));
        props.put("topic.result", System.getProperty("topic.result", subjectPrefix + "/result"));
        props.put("topic.top", System.getProperty("topic.top", subjectPrefix + "/top"));
        props.put("topic.gettop", System.getProperty("topic.gettop", subjectPrefix + "/gettop"));
        props.put("topic.live", System.getProperty("topic.live", subjectPrefix + "/live/time"));

        return props;
    }
}
