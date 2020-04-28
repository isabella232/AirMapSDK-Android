/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetDate implements Serializable, AirMapBaseModel {

    private int month;
    private int day;

    public TimesheetDate(){

    }

    public TimesheetDate(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public TimesheetDate constructFromJson(JSONObject json) {
        setMonth(json.optInt("month"));
        setDay(json.optInt("day"));
        return this;
    }

    public int getMonth() {
        return month;
    }

    public TimesheetDate setMonth(int month) {
        this.month = month;
        return this;
    }

    public int getDay() {
        return day;
    }

    public TimesheetDate setDay(int day) {
        this.day = day;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetDate{" +
                "month=" + month +
                ", day=" + day +
                '}';
    }
}
