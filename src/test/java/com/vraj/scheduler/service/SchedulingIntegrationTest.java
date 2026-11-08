package com.vraj.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vraj.scheduler.db.AvailabilityDao;
import com.vraj.scheduler.db.CoverageDao;
import com.vraj.scheduler.db.Database;
import com.vraj.scheduler.db.EmployeeDao;
import com.vraj.scheduler.db.ShiftDao;
import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.CoverageRequirement;
import com.vraj.scheduler.model.Employee;
import com.vraj.scheduler.model.Shift;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the DAOs, generator and SQL reports together against a real in-memory H2 database. */
class SchedulingIntegrationTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    private Database database;
    private EmployeeDao employees;
    private AvailabilityDao availability;
    private CoverageDao coverage;
    private ShiftDao shifts;
    private SchedulingService scheduling;
    private ReportService reports;

    private static LocalTime at(int hour) {
        return LocalTime.of(hour, 0);
    }

    @BeforeEach
    void openDatabase() throws SQLException {
        database = new Database("jdbc:h2:mem:" + UUID.randomUUID());
        employees = new EmployeeDao(database);
        availability = new AvailabilityDao(database);
        coverage = new CoverageDao(database);
        shifts = new ShiftDao(database);
        scheduling = new SchedulingService(database);
        reports = new ReportService(database);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        database.close();
    }

    private Employee addEmployeeAvailableMonday(String name) throws SQLException {
        Employee employee = employees.add(new Employee(name, "Cashier", 40));
        availability.add(new Availability(employee.getId(), 1, at(8), at(22)));
        return employee;
    }

    @Test
    void savesAndFindsEmployees() throws SQLException {
        Employee saved = employees.add(new Employee("Ann", "Lead", 35));

        assertTrue(saved.getId() > 0);
        assertEquals("Ann", employees.findById(saved.getId()).orElseThrow().getName());
        assertEquals(35, employees.findAll().get(0).getMaxWeeklyHours());
        assertFalse(employees.findById(9999).isPresent());
    }

    @Test
    void treatsSqlInInputAsPlainText() throws SQLException {
        String hostile = "x'); DROP TABLE employee; --";
        Employee saved = employees.add(new Employee(hostile, "Lead", 35));

        assertEquals(hostile, employees.findById(saved.getId()).orElseThrow().getName());
    }

    @Test
    void savingTheSameSlotUpdatesItsMinimumStaff() throws SQLException {
        coverage.save(new CoverageRequirement(1, at(9), at(17), 2));
        coverage.save(new CoverageRequirement(1, at(9), at(17), 4));

        assertEquals(1, coverage.findAll().size());
        assertEquals(4, coverage.findAll().get(0).getMinStaff());
    }

    @Test
    void generatedScheduleIsSavedAndReportedInHours() throws SQLException {
        addEmployeeAvailableMonday("Ann");
        coverage.save(new CoverageRequirement(1, at(9), at(17), 1));

        scheduling.generateWeek(MONDAY);

        assertEquals(1, shifts.findBetween(MONDAY, MONDAY.plusDays(7)).size());
        ReportService.EmployeeHours row = reports.hoursPerEmployee(MONDAY).get(0);
        assertEquals("Ann", row.name());
        assertEquals(8.0, row.hours(), 0.001);
        assertEquals(40, row.maxHours());
    }

    @Test
    void hoursReportIncludesUnscheduledEmployeesAndOnlyThatWeek() throws SQLException {
        Employee ann = addEmployeeAvailableMonday("Ann");
        employees.add(new Employee("Ben", "Cashier", 40));
        shifts.replaceWeek(MONDAY, List.of(new Shift(ann.getId(), MONDAY, at(9), at(13))));
        shifts.replaceWeek(
                MONDAY.plusDays(7),
                List.of(new Shift(ann.getId(), MONDAY.plusDays(7), at(9), at(17))));

        List<ReportService.EmployeeHours> report = reports.hoursPerEmployee(MONDAY);

        assertEquals(2, report.size());
        assertEquals("Ann", report.get(0).name());
        assertEquals(4.0, report.get(0).hours(), 0.001);
        assertEquals("Ben", report.get(1).name());
        assertEquals(0.0, report.get(1).hours(), 0.001);
    }

    @Test
    void gapReportMatchesTheGenerator() throws SQLException {
        addEmployeeAvailableMonday("Ann");
        coverage.save(new CoverageRequirement(1, at(9), at(17), 2));
        coverage.save(new CoverageRequirement(2, at(9), at(17), 1));

        var result = scheduling.generateWeek(MONDAY);

        assertEquals(2, result.gaps().size());
        assertEquals(result.gaps(), reports.coverageGaps(MONDAY));
    }

    @Test
    void regeneratingAWeekReplacesItsShifts() throws SQLException {
        addEmployeeAvailableMonday("Ann");
        coverage.save(new CoverageRequirement(1, at(9), at(17), 1));

        scheduling.generateWeek(MONDAY);
        scheduling.generateWeek(MONDAY);

        assertEquals(1, shifts.findBetween(MONDAY, MONDAY.plusDays(7)).size());
    }

    @Test
    void honorsRestBeforeAlreadyScheduledNextWeek() throws SQLException {
        Employee ann = employees.add(new Employee("Ann", "Cashier", 40));
        availability.add(new Availability(ann.getId(), 7, at(14), at(23)));
        coverage.save(new CoverageRequirement(7, at(14), at(23), 1));
        LocalDate nextMonday = MONDAY.plusDays(7);
        shifts.replaceWeek(nextMonday, List.of(new Shift(ann.getId(), nextMonday, at(6), at(12))));

        var result = scheduling.generateWeek(MONDAY);

        assertTrue(result.shifts().isEmpty());
        assertEquals(1, result.gaps().size());
    }

    @Test
    void sampleDataProducesASchedule() throws SQLException {
        SampleData.load(database);

        var result = scheduling.generateWeek(MONDAY);

        assertFalse(result.shifts().isEmpty());
        assertEquals(result.gaps(), reports.coverageGaps(MONDAY));
    }
}
