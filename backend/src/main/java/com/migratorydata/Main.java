package com.migratorydata;

import com.migratorydata.questions.Question;
import com.migratorydata.questions.QuestionLoader;
import com.migratorydata.questions.QuestionProducer;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class Main {

    private static QuestionProducer questionProducer;

    public static void main(String[] args) throws IOException {
        Properties config = loadConfigProperties();

        List<Question> questions = QuestionLoader.loadQuestion();

        questionProducer = new QuestionProducer(questions, config);
        questionProducer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                questionProducer.stop();
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
