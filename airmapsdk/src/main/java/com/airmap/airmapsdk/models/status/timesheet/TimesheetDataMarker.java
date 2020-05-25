/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class TimesheetDataMarker implements Serializable, AirMapBaseModel {

    private TimesheetEventDescriptor eventDescriptor;
    private TimesheetEventInterpretationDescriptor eventInterpretationDescriptor;
    private int eventOffset;
    private TimesheetTime timesheetTime;
    private TimesheetDate timesheetDate;

    public TimesheetDataMarker(){

    }

    public TimesheetDataMarker(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public TimesheetDataMarker constructFromJson(JSONObject json) {
        if(json != null){
            setEventDescriptor(new TimesheetEventDescriptor(json.optJSONObject("event")));
            setEventInterpretationDescriptor(new TimesheetEventInterpretationDescriptor(json.optJSONObject("event_interpretation")));
            setEventOffset(json.optInt("event_offset"));
            setTimesheetTime(new TimesheetTime(json.optJSONObject("time")));
            setTimesheetDate(new TimesheetDate(json.optJSONObject("date")));
        }
        return this;
    }

    public TimesheetEventDescriptor getEventDescriptor() {
        return eventDescriptor;
    }

    public TimesheetDataMarker setEventDescriptor(TimesheetEventDescriptor eventDescriptor) {
        this.eventDescriptor = eventDescriptor;
        return this;
    }

    public TimesheetEventInterpretationDescriptor getEventInterpretationDescriptor() {
        return eventInterpretationDescriptor;
    }

    public TimesheetDataMarker setEventInterpretationDescriptor(TimesheetEventInterpretationDescriptor eventInterpretationDescriptor) {
        this.eventInterpretationDescriptor = eventInterpretationDescriptor;
        return this;
    }

    public int getEventOffset() {
        return eventOffset;
    }

    public TimesheetDataMarker setEventOffset(int eventOffset) {
        this.eventOffset = eventOffset;
        return this;
    }

    public TimesheetTime getTimesheetTime() {
        return timesheetTime;
    }

    public TimesheetDataMarker setTimesheetTime(TimesheetTime timesheetTime) {
        this.timesheetTime = timesheetTime;
        return this;
    }

    public TimesheetDate getTimesheetDate() {
        return timesheetDate;
    }

    public TimesheetDataMarker setTimesheetDate(TimesheetDate timesheetDate) {
        this.timesheetDate = timesheetDate;
        return this;
    }

    @Override
    public String toString() {
        return "TimesheetDataMarker{" +
                "eventDescriptor=" + eventDescriptor +
                ", eventInterpretationDescriptor=" + eventInterpretationDescriptor +
                ", eventOffset=" + eventOffset +
                ", timesheetTime=" + timesheetTime +
                ", timesheetDate=" + timesheetDate +
                '}';
    }
}
