package com.migratorydata.leaderboard;

import java.util.Objects;

public class PlayerScore implements Comparable {
    private String name;
    private int score;

    public PlayerScore(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerScore user = (PlayerScore) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, score);
    }

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", score=" + score +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        int result = this.score - ((PlayerScore) o).score;
        if (result == 0) {
            return this.name.compareTo(((PlayerScore) o).name);
        }
        return result;
    }

}
