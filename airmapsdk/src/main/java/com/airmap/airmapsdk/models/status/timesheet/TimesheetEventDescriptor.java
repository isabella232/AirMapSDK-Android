/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetEventDescriptor implements Serializable, AirMapBaseModel {

    private String name;
    private TimesheetEvent timesheetEvent;

    public TimesheetEventDescriptor(){

    }

    public TimesheetEventDescriptor(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public TimesheetEventDescriptor constructFromJson(JSONObject json) {
        if(json != null){
            setName(json.optString("name"));
            setTimesheetEvent(new TimesheetEvent(json.optString("id")));
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public TimesheetEventDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public TimesheetEvent getTimesheetEvent() {
        return timesheetEvent;
    }

    public TimesheetEventDescriptor setTimesheetEvent(TimesheetEvent timesheetEvent) {
        this.timesheetEvent = timesheetEvent;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetEventDescriptor{" +
                "name='" + name + '\'' +
                ", timesheetEvent=" + timesheetEvent +
                '}';
    }
}
