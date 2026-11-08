package com.vraj.scheduler.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** A dated coverage slot that did not reach its minimum staff. */
public record CoverageGap(LocalDate date, LocalTime start, LocalTime end, int required, int assigned) {
    public int shortfall() {
        return required - assigned;
    }
}
