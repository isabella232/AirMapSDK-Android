package com.airmap.airmapsdk.models;

import java.util.Date;

public class TemporalFilter {

    //Not Timezone Aware
    public enum Type {
        NOW,
        CUSTOM
    }

    public enum Range {
        ONE_HOUR,
        FOUR_HOUR,
        EIGHT_HOUR,
        TWELVE_HOUR
    }

    private Type type;
    private Range range;
    private Date futureDate;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    public TemporalFilter(Range range) {
        this.range = range;
        type = Type.NOW;
    }

    public TemporalFilter(Date futureDate, int startHour, int startMinute, int endHour, int endMinute) {
        this.futureDate = futureDate;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        type = Type.CUSTOM;
    }

    public Type getType() {
        return type;
    }

    public Range getRange() {
        return range;
    }

    public Date getFutureDate() {
        return futureDate;
    }

    public void setFutureDate(Date futureDate) {
        this.futureDate = futureDate;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    @Override
    public String toString() {
        return "TemporalFilter{" +
                "type=" + type +
                ", range=" + range +
                ", futureDate=" + futureDate +
                ", startHour=" + startHour +
                ", startMinute=" + startMinute +
                ", endHour=" + endHour +
                ", endMinute=" + endMinute +
                '}';
    }
}
