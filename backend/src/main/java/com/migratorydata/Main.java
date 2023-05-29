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
        boolean enablePlayersSimulator = Boolean.valueOf(config.getProperty("enable.playerssimulator", "true"));
        boolean enableQuestionProducer = Boolean.valueOf(config.getProperty("enable.questionproducer", "true"));

        StatisticsProcessor statisticsProcessor = new StatisticsProcessor();

        int instances = Integer.valueOf(config.getProperty("answers.threads", "1"));

        ResultsProducer[] resultsProducers = new ResultsProducer[instances];
        AnswersConsumer[] answersConsumers = new AnswersConsumer[instances];


        leaderboardProcessor = new LeaderboardProcessor(config);

        scoreConsumer = new ScoreConsumer(leaderboardProcessor, config);
        scoreConsumer.start();

        for (int i = 0; i < instances; i++) {
            resultsProducers[i] = new ResultsProducer(config, statisticsProcessor, scoreConsumer);
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
            questionProducer = new QuestionProducer(questions, config, leaderboardProcessor);
            questionProducer.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (enablePlayersSimulator) playersSimulator.stop();
                if (enableQuestionProducer) questionProducer.stop();
                scoreConsumer.stop();
                leaderboardProcessor.stop();

                for (AnswersConsumer answersConsumer : answersConsumers) {
                    answersConsumer.stop();
                }
                for (ResultsProducer resultsProducer : resultsProducers) {
                    resultsProducer.close();
                }
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
