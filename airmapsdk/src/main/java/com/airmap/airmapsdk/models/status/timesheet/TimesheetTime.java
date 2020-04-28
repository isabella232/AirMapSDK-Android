/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetTime implements Serializable, AirMapBaseModel {

    private int hour;
    private int minute;

    public TimesheetTime(){

    }

    public TimesheetTime(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public TimesheetTime constructFromJson(JSONObject json) {
        setHour(json.optInt("hour"));
        setMinute(json.optInt("minute"));
        return this;
    }

    public int getHour() {
        return hour;
    }

    public TimesheetTime setHour(int hour) {
        this.hour = hour;
        return this;
    }

    public int getMinute() {
        return minute;
    }

    public TimesheetTime setMinute(int minute) {
        this.minute = minute;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetTime{" +
                "hour=" + hour +
                ", minute=" + minute +
                '}';
    }
}
