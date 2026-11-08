package com.vraj.scheduler.model;

import java.time.LocalTime;

/** A weekly window in which an employee can work. Days run 1=Monday to 7=Sunday. */
public class Availability {
    private final int id;
    private final int employeeId;
    private final int dayOfWeek;
    private final LocalTime start;
    private final LocalTime end;

    public Availability(int employeeId, int dayOfWeek, LocalTime start, LocalTime end) {
        this(0, employeeId, dayOfWeek, start, end);
    }

    public Availability(int id, int employeeId, int dayOfWeek, LocalTime start, LocalTime end) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Day of week must be 1 (Mon) to 7 (Sun)");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.id = id;
        this.employeeId = employeeId;
        this.dayOfWeek = dayOfWeek;
        this.start = start;
        this.end = end;
    }

    public int getId() {
        return id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}
