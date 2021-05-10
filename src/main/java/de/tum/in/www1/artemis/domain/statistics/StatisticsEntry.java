package de.tum.in.www1.artemis.domain.statistics;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * A data entry used by the statistics pages to calculate the graph data
 * day is the the timeslot in which the data is created
 * amount is the number of entries in the same timeslot
 * username is for specific purposes (Logged-in users, active user, active tutors) where duplicated user entries need to
 * be filtered out in java and therefore not the amount is needed, but the name so the amount can be calculated afterwards
 */
public class StatisticsEntry {

    private TemporalAccessor day;

    private long amount;

    private String username;

    private String date;

    public StatisticsEntry(TemporalAccessor day, long amount) {
        this.day = day;
        this.amount = amount;
        this.username = "";
    }

    public StatisticsEntry(TemporalAccessor day, String username) {
        this.day = day;
        this.amount = 1L;
        this.username = username;
    }

    public StatisticsEntry(String date, String username) {
        this.amount = 1L;
        this.username = username;
        var zone = ZonedDateTime.now().getZone();
        String dateOfElement = date + " 10:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        this.day = LocalDateTime.parse(dateOfElement, formatter).atZone(zone);
    }

    public StatisticsEntry(String date, long amount) {
        var zone = ZonedDateTime.now().getZone();
        String dateOfElement = date + " 10:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        this.day = LocalDateTime.parse(dateOfElement, formatter).atZone(zone);
        this.amount = amount;
    }

    public StatisticsEntry() {
        this.amount = 0L;
        this.username = "";
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public TemporalAccessor getDay() {
        return day;
    }

    public void setDay(TemporalAccessor day) {
        this.day = day;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
