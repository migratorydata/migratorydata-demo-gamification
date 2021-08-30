package com.migratorydata.questions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionLoader {

    public static List<Question> loadQuestion() throws IOException {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader
                (new InputStreamReader(new FileInputStream("./config/quiz.txt"), "UTF-8"))) {

            String question = null;
            List<String> answers = new ArrayList<>();
            int currentReading = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.isEmpty()) {
                    if (question != null && answers.size() > 0) {
                        String correctAnswer = answers.remove(answers.size() - 1);
                        questions.add(new Question(question, answers, correctAnswer));
                    }
                    question = null;
                    answers = new ArrayList<>();
                    currentReading = 0;
                } else {
                    if (currentReading == 0) {
                        question = line.trim();
                        currentReading = 1;
                    } else if (currentReading == 1) {
                        answers.add(line.trim());
                    }
                }
            }
        }

        return questions;
    }

}
