/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetData implements Serializable, AirMapBaseModel {

    private int utc_offset;
    private boolean excluded;
    private boolean daylight_saving_adjust;
    private TimesheetDayDescriptor day;
    private TimesheetDayDescriptor dayTil;
    private TimesheetDataMarker start;
    private TimesheetDataMarker end;

    public TimesheetData(){

    }

    public TimesheetData(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public TimesheetData constructFromJson(JSONObject json) {
        if(json != null){
            setUtc_offset(json.optInt("utc_offset"));
            setExcluded(json.optBoolean("excluded"));
            setDaylight_saving_adjust(json.optBoolean("daylight_saving_adjust"));
            setDay(new TimesheetDayDescriptor(json.optJSONObject("day")));
            setDayTil(new TimesheetDayDescriptor(json.optJSONObject("day_til")));
            setStart(new TimesheetDataMarker(json.optJSONObject("start")));
            setEnd(new TimesheetDataMarker(json.optJSONObject("end")));
        }
        return this;
    }

    public int getUtc_offset() {
        return utc_offset;
    }

    public TimesheetData setUtc_offset(int utc_offset) {
        this.utc_offset = utc_offset;
        return this;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public TimesheetData setExcluded(boolean excluded) {
        this.excluded = excluded;
        return this;
    }

    public boolean isDaylight_saving_adjust() {
        return daylight_saving_adjust;
    }

    public TimesheetData setDaylight_saving_adjust(boolean daylight_saving_adjust) {
        this.daylight_saving_adjust = daylight_saving_adjust;
        return this;
    }

    public TimesheetDayDescriptor getDay() {
        return day;
    }

    public TimesheetData setDay(TimesheetDayDescriptor day) {
        this.day = day;
        return this;
    }

    public TimesheetDayDescriptor getDayTil() {
        return dayTil;
    }

    public TimesheetData setDayTil(TimesheetDayDescriptor dayTil) {
        this.dayTil = dayTil;
        return this;
    }

    public TimesheetDataMarker getStart() {
        return start;
    }

    public TimesheetData setStart(TimesheetDataMarker start) {
        this.start = start;
        return this;
    }

    public TimesheetDataMarker getEnd() {
        return end;
    }

    public TimesheetData setEnd(TimesheetDataMarker end) {
        this.end = end;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetData{" +
                "utc_offset=" + utc_offset +
                ", excluded=" + excluded +
                ", daylight_saving_adjust=" + daylight_saving_adjust +
                ", day=" + day +
                ", dayTil=" + dayTil +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
