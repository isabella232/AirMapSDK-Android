/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetEventInterpretationDescriptor implements Serializable, AirMapBaseModel {

    private String name;
    private TimesheetEventInterpretation timesheetEventInterpretation;

    public TimesheetEventInterpretationDescriptor (){

    }

    public TimesheetEventInterpretationDescriptor(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public TimesheetEventInterpretationDescriptor constructFromJson(JSONObject json) {
        if(json != null){
            setName(json.optString("name"));
            setTimesheetEventInterpretation(new TimesheetEventInterpretation(json.optString("id")));
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public TimesheetEventInterpretationDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public TimesheetEventInterpretation getTimesheetEventInterpretation() {
        return timesheetEventInterpretation;
    }

    public TimesheetEventInterpretationDescriptor setTimesheetEventInterpretation(TimesheetEventInterpretation timesheetEventInterpretation) {
        this.timesheetEventInterpretation = timesheetEventInterpretation;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetEventInterpretationDescriptor{" +
                "name='" + name + '\'' +
                ", timesheetEventInterpretation=" + timesheetEventInterpretation +
                '}';
    }
}
