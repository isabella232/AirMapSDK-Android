/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import java.io.Serializable;

public class TimesheetEvent implements Serializable {

    private String event;

    public TimesheetEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public String toString() {
        return "TimesheetEvent{" +
                "event='" + event + '\'' +
                '}';
    }
}
