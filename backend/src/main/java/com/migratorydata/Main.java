package com.migratorydata;

import com.migratorydata.answers.AnswersConsumer;
import com.migratorydata.answers.ResultsProducer;
import com.migratorydata.answers.StatisticsProcessor;
import com.migratorydata.questions.PlayersSimulator;
import com.migratorydata.questions.Question;
import com.migratorydata.questions.QuestionLoader;
import com.migratorydata.questions.QuestionProducer;
import com.migratorydata.leaderboard.ScoreConsumer;
import com.migratorydata.leaderboard.LeaderboardProcessor;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class Main {

    private static LeaderboardProcessor leaderboardProcessor;
    private static ScoreConsumer scoreConsumer;
    private static PlayersSimulator playersSimulator;
    private static QuestionProducer questionProducer;

    public static void main(String[] args) throws IOException {
        Properties config = loadConfigProperties();
        boolean enableLeaderboard = Boolean.valueOf(config.getProperty("enable.leaderboard", "true"));
        boolean enablePlayersSimulator = Boolean.valueOf(config.getProperty("enable.playerssimulator", "true"));
        boolean enableQuestionProducer = Boolean.valueOf(config.getProperty("enable.questionproducer", "true"));

        StatisticsProcessor statisticsProcessor = new StatisticsProcessor();

        // Answers processor start
        config.setProperty("group.id", UUID.randomUUID().toString().substring(0, 10));

        int instances = Integer.valueOf(config.getProperty("answers.threads", "1"));

        ResultsProducer[] resultsProducers = new ResultsProducer[instances];
        AnswersConsumer[] answersConsumers = new AnswersConsumer[instances];

        for (int i = 0; i < instances; i++) {
            resultsProducers[i] = new ResultsProducer(config, statisticsProcessor);
            resultsProducers[i].start();

            answersConsumers[i] = new AnswersConsumer(resultsProducers[i], resultsProducers, config, i, statisticsProcessor);
            answersConsumers[i].start();
        }

        // Questions generator start
        if (enablePlayersSimulator) {
            playersSimulator = new PlayersSimulator(config);
            playersSimulator.start();
        }

        List<Question> questions = QuestionLoader.loadQuestion();
        if (enableQuestionProducer) {
            questionProducer = new QuestionProducer(questions, config);
            questionProducer.start();
        }

        // Leaderboard processor start
        if (enableLeaderboard) {
            leaderboardProcessor = new LeaderboardProcessor(config, questions.size());

            scoreConsumer = new ScoreConsumer(leaderboardProcessor, config);
            scoreConsumer.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (enablePlayersSimulator) playersSimulator.stop();
                if (enableQuestionProducer) questionProducer.stop();
                if (enableLeaderboard) {
                    scoreConsumer.stop();
                    leaderboardProcessor.stop();
                }

                for (AnswersConsumer answersConsumer : answersConsumers) {
                    answersConsumer.stop();
                }
                for (ResultsProducer resultsProducer : resultsProducers) {
                    resultsProducer.close();
                }
            }
        });
    }

    public static Properties loadConfigProperties() {
        Properties props = readPropertiesFile("./config/config.properties");
        if (props == null) {
            System.err.println("Please configure ./config/config.properties config file");
            System.exit(99);
        }
        return props;
    }

    public static Properties readPropertiesFile(String fileName) {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(fileName)){
            props.load(input);
        } catch (IOException e) {
            return null;
        }
        return props;
    }

}
