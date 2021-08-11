package com.migratorydata.answers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsProcessor {

    private final AtomicLong answers = new AtomicLong();
    private final AtomicLong results = new AtomicLong();

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public StatisticsProcessor() {
        executor.scheduleAtFixedRate(() -> {
            long answersConsumed = answers.getAndSet(0);
            long resultsSent = results.getAndSet(0);
            System.out.println("Number of answers consumed=" + (double)(answersConsumed / 5) + "/second");
            System.out.println("Number of results sent=    " + (double)(resultsSent / 5) + "/second");
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    public void addAnswers(int count) {
        answers.addAndGet(count);
    }

    public void incrementResults() {
        results.incrementAndGet();
    }
}