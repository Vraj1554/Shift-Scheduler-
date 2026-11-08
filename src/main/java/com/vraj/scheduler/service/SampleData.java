package com.vraj.scheduler.service;

import com.vraj.scheduler.db.AvailabilityDao;
import com.vraj.scheduler.db.CoverageDao;
import com.vraj.scheduler.db.Database;
import com.vraj.scheduler.db.EmployeeDao;
import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.CoverageRequirement;
import com.vraj.scheduler.model.Employee;
import java.sql.SQLException;
import java.time.LocalTime;

/** A small fictional store, so the app can be demonstrated without typing everything in. */
public final class SampleData {
    private static final LocalTime WEEKDAY_OPEN = LocalTime.of(9, 0);
    private static final LocalTime SHIFT_CHANGE = LocalTime.of(15, 0);
    private static final LocalTime WEEKDAY_CLOSE = LocalTime.of(21, 0);
    private static final LocalTime WEEKEND_OPEN = LocalTime.of(10, 0);
    private static final LocalTime WEEKEND_CLOSE = LocalTime.of(18, 0);

    private SampleData() {}

    public static void load(Database database) throws SQLException {
        EmployeeDao employees = new EmployeeDao(database);
        AvailabilityDao availability = new AvailabilityDao(database);
        CoverageDao coverage = new CoverageDao(database);

        addEmployee(employees, availability, "Alex Morgan", "Shift Lead", 40, 1, 5, 8, 22);
        addEmployee(employees, availability, "Sam Rivera", "Cashier", 32, 1, 5, 8, 16);
        addEmployee(employees, availability, "Jordan Lee", "Cashier", 24, 3, 7, 9, 22);
        addEmployee(employees, availability, "Priya Shah", "Stock", 40, 1, 7, 14, 22);
        addEmployee(employees, availability, "Chris Nguyen", "Cashier", 20, 5, 7, 9, 21);
        addEmployee(employees, availability, "Taylor Brooks", "Stock", 16, 6, 7, 10, 18);

        for (int day = 1; day <= 5; day++) {
            coverage.save(new CoverageRequirement(day, WEEKDAY_OPEN, SHIFT_CHANGE, 2));
            coverage.save(new CoverageRequirement(day, SHIFT_CHANGE, WEEKDAY_CLOSE, 2));
        }
        for (int day = 6; day <= 7; day++) {
            coverage.save(new CoverageRequirement(day, WEEKEND_OPEN, WEEKEND_CLOSE, 3));
        }
    }

    private static void addEmployee(
            EmployeeDao employees,
            AvailabilityDao availability,
            String name,
            String role,
            int maxHours,
            int firstDay,
            int lastDay,
            int startHour,
            int endHour)
            throws SQLException {
        Employee employee = employees.add(new Employee(name, role, maxHours));
        for (int day = firstDay; day <= lastDay; day++) {
            availability.add(
                    new Availability(
                            employee.getId(),
                            day,
                            LocalTime.of(startHour, 0),
                            LocalTime.of(endHour, 0)));
        }
    }
}
