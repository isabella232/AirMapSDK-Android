/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetDayDescriptor implements Serializable, AirMapBaseModel {

    private String name;
    private TimesheetDay timesheetDay;

    public TimesheetDayDescriptor (){

    }

    public TimesheetDayDescriptor(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }
    @Override
    public TimesheetDayDescriptor constructFromJson(JSONObject json) {
        if(json != null){
            setName(json.optString("name"));
            setTimesheetDay(new TimesheetDay(json.optString("id")));
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public TimesheetDayDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public TimesheetDay getTimesheetDay() {
        return timesheetDay;
    }

    public TimesheetDayDescriptor setTimesheetDay(TimesheetDay timesheetDay) {
        this.timesheetDay = timesheetDay;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetDayDescriptor{" +
                "name='" + name + '\'' +
                ", timesheetDay=" + timesheetDay +
                '}';
    }

}
