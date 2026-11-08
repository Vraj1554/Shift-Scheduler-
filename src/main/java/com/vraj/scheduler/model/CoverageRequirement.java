package com.vraj.scheduler.model;

import java.time.LocalTime;

/** The minimum number of staff needed for a time slot on a given day of every week. */
public class CoverageRequirement {
    private final int id;
    private final int dayOfWeek;
    private final LocalTime start;
    private final LocalTime end;
    private final int minStaff;

    public CoverageRequirement(int dayOfWeek, LocalTime start, LocalTime end, int minStaff) {
        this(0, dayOfWeek, start, end, minStaff);
    }

    public CoverageRequirement(int id, int dayOfWeek, LocalTime start, LocalTime end, int minStaff) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Day of week must be 1 (Mon) to 7 (Sun)");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (minStaff < 1) {
            throw new IllegalArgumentException("Minimum staff must be at least 1");
        }
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.start = start;
        this.end = end;
        this.minStaff = minStaff;
    }

    public int getId() {
        return id;
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

    public int getMinStaff() {
        return minStaff;
    }
}
