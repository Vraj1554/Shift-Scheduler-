package com.vraj.scheduler.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** One employee working one time range on one date. Shifts do not cross midnight. */
public class Shift {
    private final int id;
    private final int employeeId;
    private final LocalDate date;
    private final LocalTime start;
    private final LocalTime end;

    public Shift(int employeeId, LocalDate date, LocalTime start, LocalTime end) {
        this(0, employeeId, date, start, end);
    }

    public Shift(int id, int employeeId, LocalDate date, LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.id = id;
        this.employeeId = employeeId;
        this.date = date;
        this.start = start;
        this.end = end;
    }

    public int getId() {
        return id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public long getDurationMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}
