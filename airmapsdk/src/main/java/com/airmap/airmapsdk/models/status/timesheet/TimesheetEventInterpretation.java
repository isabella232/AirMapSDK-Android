/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import java.io.Serializable;

public class TimesheetEventInterpretation implements Serializable {

    private String eventIntepretation;

    public TimesheetEventInterpretation(String eventIntepretation) {
        this.eventIntepretation = eventIntepretation;
    }

    public String getEventIntepretation() {
        return eventIntepretation;
    }

    public void setEventIntepretation(String eventIntepretation) {
        this.eventIntepretation = eventIntepretation;
    }

    @Override
    public String toString() {
        return "TimesheetEventInterpretation{" +
                "eventIntepretation='" + eventIntepretation + '\'' +
                '}';
    }
}
