package de.tum.in.www1.artemis.web.rest.dto;

public class StudentLeaderboardDTO {

    private long userId;

    private String name;

    private Long score;

    private double points;

    public StudentLeaderboardDTO(long userId, String name, Long score, double points) {
        this.userId = userId;
        this.name = name;
        this.score = score;
        this.points = points;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }
}
