package com.migratorydata.questions;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PlayersSimulator {

    private Properties config;

    private Player[] players;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    public PlayersSimulator(Properties config) {
        this.config = config;

        players = new Player[Integer.valueOf(config.getProperty("simulation.clients.number", "10"))];
    }

    public void start() {
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(config, i, executor);
            players[i].start();
        }
    }

    public void stop() {
        for (int i = 0; i < players.length; i++) {
            players[i].stop();
        }
    }

}
