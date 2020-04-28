/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class Timesheet implements Serializable, AirMapBaseModel {

    private boolean active;
    private TimesheetData data;

    public Timesheet () {
    }

    public Timesheet (JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public Timesheet constructFromJson(JSONObject json) {
        setActive(json.optBoolean("active"));
        setData(new TimesheetData(json.optJSONObject("data")));
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public Timesheet setActive(boolean active) {
        this.active = active;
        return this;
    }

    public TimesheetData getData() {
        return data;
    }

    public Timesheet setData(TimesheetData data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "Timesheet{" +
                "active=" + active +
                ", data=" + data +
                '}';
    }
}
