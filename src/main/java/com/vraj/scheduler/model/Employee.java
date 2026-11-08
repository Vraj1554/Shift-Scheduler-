package com.vraj.scheduler.model;

public class Employee {
    private final int id;
    private final String name;
    private final String role;
    private final int maxWeeklyHours;

    /** An employee that has not been saved yet. */
    public Employee(String name, String role, int maxWeeklyHours) {
        this(0, name, role, maxWeeklyHours);
    }

    public Employee(int id, String name, String role, int maxWeeklyHours) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        if (maxWeeklyHours < 0) {
            throw new IllegalArgumentException("Max weekly hours cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.role = role;
        this.maxWeeklyHours = maxWeeklyHours;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public int getMaxWeeklyHours() {
        return maxWeeklyHours;
    }
}
