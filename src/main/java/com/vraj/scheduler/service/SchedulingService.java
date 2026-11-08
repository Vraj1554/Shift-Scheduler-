package com.vraj.scheduler.service;

import com.vraj.scheduler.db.AvailabilityDao;
import com.vraj.scheduler.db.CoverageDao;
import com.vraj.scheduler.db.Database;
import com.vraj.scheduler.db.EmployeeDao;
import com.vraj.scheduler.db.ShiftDao;
import com.vraj.scheduler.model.Shift;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Connects the pure {@link ScheduleGenerator} to the database. */
public class SchedulingService {
    private final EmployeeDao employees;
    private final AvailabilityDao availability;
    private final CoverageDao coverage;
    private final ShiftDao shifts;
    private final ScheduleGenerator generator = new ScheduleGenerator(new RuleValidator());

    public SchedulingService(Database database) {
        this.employees = new EmployeeDao(database);
        this.availability = new AvailabilityDao(database);
        this.coverage = new CoverageDao(database);
        this.shifts = new ShiftDao(database);
    }

    /** Generates the week and saves it, replacing any schedule that week already had. */
    public ScheduleGenerator.Result generateWeek(LocalDate weekStart) throws SQLException {
        // The only outside shifts that can break the rest rule are on the day either side.
        List<Shift> surrounding = new ArrayList<>(shifts.findBetween(weekStart.minusDays(1), weekStart));
        surrounding.addAll(shifts.findBetween(weekStart.plusDays(7), weekStart.plusDays(8)));

        ScheduleGenerator.Result result =
                generator.generate(
                        weekStart,
                        employees.findAll(),
                        availability.findAll(),
                        coverage.findAll(),
                        surrounding);
        shifts.replaceWeek(weekStart, result.shifts());
        return result;
    }
}
