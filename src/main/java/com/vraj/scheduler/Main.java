package com.vraj.scheduler;

import com.vraj.scheduler.db.AvailabilityDao;
import com.vraj.scheduler.db.CoverageDao;
import com.vraj.scheduler.db.Database;
import com.vraj.scheduler.db.EmployeeDao;
import com.vraj.scheduler.db.ShiftDao;
import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.CoverageGap;
import com.vraj.scheduler.model.CoverageRequirement;
import com.vraj.scheduler.model.Employee;
import com.vraj.scheduler.model.Shift;
import com.vraj.scheduler.service.ReportService;
import com.vraj.scheduler.service.SampleData;
import com.vraj.scheduler.service.ScheduleGenerator;
import com.vraj.scheduler.service.SchedulingService;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Command-line menu for the scheduler. */
public class Main {
    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private final Scanner in = new Scanner(System.in);
    private final Database database;
    private final EmployeeDao employees;
    private final AvailabilityDao availability;
    private final CoverageDao coverage;
    private final ShiftDao shifts;
    private final SchedulingService scheduling;
    private final ReportService reports;

    private Main(Database database) {
        this.database = database;
        this.employees = new EmployeeDao(database);
        this.availability = new AvailabilityDao(database);
        this.coverage = new CoverageDao(database);
        this.shifts = new ShiftDao(database);
        this.scheduling = new SchedulingService(database);
        this.reports = new ReportService(database);
    }

    public static void main(String[] args) {
        try (Database database = new Database()) {
            new Main(database).run();
        } catch (SQLException e) {
            System.err.println("Could not open the database: " + e.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        try {
            while (true) {
                printMenu();
                String choice = in.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> addEmployee();
                        case "2" -> setAvailability();
                        case "3" -> setCoverage();
                        case "4" -> generateSchedule();
                        case "5" -> viewSchedule();
                        case "6" -> showReports();
                        case "7" -> {
                            System.out.println("Goodbye.");
                            return;
                        }
                        case "8" -> loadSampleData();
                        default -> System.out.println("Please choose an option from the menu.");
                    }
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid input: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println(); // Input ended (Ctrl+D, or end of piped input).
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== Shift Scheduler ===");
        System.out.println("1) Add employee");
        System.out.println("2) Set availability");
        System.out.println("3) Set coverage");
        System.out.println("4) Generate schedule");
        System.out.println("5) View schedule");
        System.out.println("6) Reports");
        System.out.println("7) Exit");
        System.out.println("8) Load sample data");
        System.out.print("> ");
    }

    private void addEmployee() throws SQLException {
        String name = prompt("Name: ");
        String role = prompt("Role: ");
        int maxHours = promptInt("Max weekly hours [40]: ", 40);
        Employee saved = employees.add(new Employee(name, role, maxHours));
        System.out.printf("Added %s with id %d.%n", saved.getName(), saved.getId());
    }

    private void setAvailability() throws SQLException {
        List<Employee> all = employees.findAll();
        if (all.isEmpty()) {
            System.out.println("Add an employee first.");
            return;
        }
        all.forEach(e -> System.out.printf("  %d: %s (%s)%n", e.getId(), e.getName(), e.getRole()));
        int id = promptInt("Employee id: ", null);
        if (employees.findById(id).isEmpty()) {
            System.out.println("No employee has that id.");
            return;
        }
        List<Integer> days = promptDays();
        String startText = prompt("Start time HH:mm, or 'off' to clear these days: ");
        if (startText.equalsIgnoreCase("off")) {
            for (int day : days) {
                availability.clearDay(id, day);
            }
            System.out.println("Availability cleared.");
            return;
        }
        LocalTime start = parseTime(startText);
        LocalTime end = promptTime("End time HH:mm: ");
        for (int day : days) {
            // Checked first so bad input cannot clear what is already saved.
            Availability window = new Availability(id, day, start, end);
            availability.clearDay(id, day);
            availability.add(window);
        }
        System.out.println("Availability saved.");
    }

    private void setCoverage() throws SQLException {
        List<Integer> days = promptDays();
        LocalTime start = promptTime("Start time HH:mm: ");
        LocalTime end = promptTime("End time HH:mm: ");
        int minStaff = promptInt("Minimum staff: ", null);
        for (int day : days) {
            coverage.save(new CoverageRequirement(day, start, end, minStaff));
        }
        System.out.println("Coverage saved.");
    }

    private void generateSchedule() throws SQLException {
        LocalDate week = promptWeek();
        ScheduleGenerator.Result result = scheduling.generateWeek(week);
        System.out.printf(
                "Created %d shifts for the week of %s, with %d coverage gaps.%n",
                result.shifts().size(), week.format(DAY_FORMAT), result.gaps().size());
        printGaps(result.gaps());
    }

    private void viewSchedule() throws SQLException {
        LocalDate week = promptWeek();
        List<Shift> weekShifts = shifts.findBetween(week, week.plusDays(7));
        if (weekShifts.isEmpty()) {
            System.out.println("No shifts scheduled that week.");
            return;
        }
        var names =
                employees.findAll().stream()
                        .collect(Collectors.toMap(Employee::getId, Function.identity()));
        LocalDate currentDay = null;
        for (Shift shift : weekShifts) {
            if (!shift.getDate().equals(currentDay)) {
                currentDay = shift.getDate();
                System.out.println(currentDay.format(DAY_FORMAT));
            }
            Employee employee = names.get(shift.getEmployeeId());
            System.out.printf(
                    "  %s-%s  %-16s %s%n",
                    shift.getStart(), shift.getEnd(), employee.getName(), employee.getRole());
        }
    }

    private void showReports() throws SQLException {
        LocalDate week = promptWeek();
        System.out.println();
        System.out.println("Hours per employee");
        for (ReportService.EmployeeHours row : reports.hoursPerEmployee(week)) {
            System.out.printf(
                    "  %-16s %5.1f / %d hrs%n", row.name(), row.hours(), row.maxHours());
        }
        System.out.println();
        System.out.println("Coverage gaps");
        printGaps(reports.coverageGaps(week));
    }

    private void loadSampleData() throws SQLException {
        if (!employees.findAll().isEmpty() || !coverage.findAll().isEmpty()) {
            System.out.println("Sample data can only be loaded into an empty database.");
            return;
        }
        SampleData.load(database);
        System.out.println("Loaded 6 employees with availability and weekly coverage.");
    }

    private static void printGaps(List<CoverageGap> gaps) {
        if (gaps.isEmpty()) {
            System.out.println("  None. Every slot reached its minimum staff.");
        }
        for (CoverageGap gap : gaps) {
            System.out.printf(
                    "  %s %s-%s: %d of %d staff (short %d)%n",
                    gap.date().format(DAY_FORMAT),
                    gap.start(),
                    gap.end(),
                    gap.assigned(),
                    gap.required(),
                    gap.shortfall());
        }
    }

    // --- Input helpers: each re-asks until the input is valid. ---

    private String prompt(String label) {
        while (true) {
            System.out.print(label);
            String value = in.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("A value is required.");
        }
    }

    private int promptInt(String label, Integer defaultValue) {
        while (true) {
            System.out.print(label);
            String value = in.nextLine().trim();
            if (value.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private LocalTime promptTime(String label) {
        while (true) {
            try {
                return parseTime(prompt(label));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use 24-hour HH:mm, for example 09:00 or 17:30.");
        }
    }

    private List<Integer> promptDays() {
        while (true) {
            String text = prompt("Days, 1=Mon to 7=Sun (examples: 3 or 1-5 or 1,3,5): ");
            try {
                return parseDays(text);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static List<Integer> parseDays(String text) {
        List<Integer> days = new ArrayList<>();
        try {
            for (String part : text.split(",")) {
                String[] range = part.trim().split("-");
                if (range.length > 2) {
                    throw new NumberFormatException();
                }
                int first = Integer.parseInt(range[0].trim());
                int last = range.length == 2 ? Integer.parseInt(range[1].trim()) : first;
                if (first < 1 || last > 7 || first > last) {
                    throw new NumberFormatException();
                }
                for (int day = first; day <= last; day++) {
                    if (!days.contains(day)) {
                        days.add(day);
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Use days from 1 to 7, for example 3 or 1-5 or 1,3,5.");
        }
        return days;
    }

    /** Asks for a date and returns the Monday of its week. Blank means next week. */
    private LocalDate promptWeek() {
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        while (true) {
            System.out.printf("Any date in the week, YYYY-MM-DD [%s]: ", nextMonday);
            String text = in.nextLine().trim();
            if (text.isEmpty()) {
                return nextMonday;
            }
            try {
                return LocalDate.parse(text)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            } catch (DateTimeParseException e) {
                System.out.println("Use the format YYYY-MM-DD.");
            }
        }
    }
}
