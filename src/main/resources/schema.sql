CREATE TABLE IF NOT EXISTS employee (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  role VARCHAR(50) NOT NULL,
  max_weekly_hours INT NOT NULL DEFAULT 40 CHECK (max_weekly_hours >= 0)
);

CREATE TABLE IF NOT EXISTS availability (
  id INT AUTO_INCREMENT PRIMARY KEY,
  employee_id INT NOT NULL REFERENCES employee(id),
  day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1=Mon ... 7=Sun
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS coverage_requirement (
  id INT AUTO_INCREMENT PRIMARY KEY,
  day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  min_staff INT NOT NULL CHECK (min_staff >= 1),
  CHECK (start_time < end_time),
  UNIQUE (day_of_week, start_time, end_time)
);

CREATE TABLE IF NOT EXISTS shift (
  id INT AUTO_INCREMENT PRIMARY KEY,
  employee_id INT NOT NULL REFERENCES employee(id),
  shift_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_availability_employee ON availability(employee_id);
CREATE INDEX IF NOT EXISTS idx_shift_date ON shift(shift_date);
