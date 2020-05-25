/*
 * Copyright (c) 2020. Vivek Vinodh
 */

package com.airmap.airmapsdk.models.status.timesheet;

import java.io.Serializable;

public class TimesheetDay implements Serializable {

    private String day;

    public TimesheetDay(String day) {
        this.day = day;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    @Override
    public String toString() {
        return "TimesheetDay{" +
                "day='" + day + '\'' +
                '}';
    }
}
