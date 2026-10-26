CREATE DATABASE IF NOT EXISTS gatherly
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gatherly;

-- Starter schema only. Use Flyway migrations in the application for ongoing changes.
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(180) NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  event_date DATE,
  location VARCHAR(255),
  guest_count INT,
  currency CHAR(3) NOT NULL DEFAULT 'INR',
  total_budget DECIMAL(12,2) NOT NULL DEFAULT 0,
  style_preferences TEXT,
  dietary_requirements TEXT,
  accessibility_needs TEXT,
  priorities TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_events_user_date (user_id, event_date)
);

CREATE TABLE IF NOT EXISTS budget_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  category VARCHAR(80) NOT NULL,
  description VARCHAR(255),
  estimated_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  actual_amount DECIMAL(12,2),
  is_estimate BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_budget_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  INDEX idx_budget_event (event_id)
);

CREATE TABLE IF NOT EXISTS tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  title VARCHAR(220) NOT NULL,
  description TEXT,
  due_date DATE,
  status VARCHAR(24) NOT NULL DEFAULT 'TODO',
  priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  assignee_label VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_tasks_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  INDEX idx_tasks_event_due (event_id, due_date)
);

CREATE TABLE IF NOT EXISTS vendors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  category VARCHAR(80) NOT NULL,
  name VARCHAR(180),
  contact_details VARCHAR(255),
  quoted_amount DECIMAL(12,2),
  quote_status VARCHAR(24) NOT NULL DEFAULT 'UNVERIFIED',
  booking_status VARCHAR(24) NOT NULL DEFAULT 'NOT_BOOKED',
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_vendors_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  INDEX idx_vendors_event (event_id)
);

CREATE TABLE IF NOT EXISTS plan_revisions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  change_summary TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_revisions_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  UNIQUE KEY uq_event_version (event_id, version_number)
);
