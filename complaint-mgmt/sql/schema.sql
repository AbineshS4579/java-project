-- ============================================================
--  Complaint Management System — MySQL Schema + Sample Data
-- ============================================================

CREATE DATABASE IF NOT EXISTS complaint_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE complaint_db;

-- ─────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    email       VARCHAR(180)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,          -- bcrypt hash
    role        ENUM('user','admin') NOT NULL DEFAULT 'user',
    department  VARCHAR(100)  DEFAULT NULL,
    is_active   TINYINT(1)    NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role  (role)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────
-- DEPARTMENTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────
-- COMPLAINTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS complaints (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT         NOT NULL,
    category      VARCHAR(80)  NOT NULL,
    priority      ENUM('low','medium','high','critical') NOT NULL DEFAULT 'medium',
    status        ENUM('pending','in_progress','resolved','closed','archived')
                               NOT NULL DEFAULT 'pending',
    user_id       INT          NOT NULL,
    assigned_to   INT          DEFAULT NULL,    -- FK → users
    department_id INT          DEFAULT NULL,    -- FK → departments
    resolution    TEXT         DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE CASCADE,
    FOREIGN KEY (assigned_to)   REFERENCES users(id)       ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_user_id   (user_id),
    INDEX idx_status    (status),
    INDEX idx_priority  (priority),
    INDEX idx_created   (created_at)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────
-- STATUS HISTORY
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS status_history (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id  INT          NOT NULL,
    changed_by    INT          NOT NULL,        -- FK → users
    old_status    VARCHAR(20)  DEFAULT NULL,
    new_status    VARCHAR(20)  NOT NULL,
    note          TEXT         DEFAULT NULL,
    changed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by)   REFERENCES users(id)      ON DELETE CASCADE,
    INDEX idx_complaint (complaint_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────
-- COMMENTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id INT          NOT NULL,
    user_id      INT          NOT NULL,
    body         TEXT         NOT NULL,
    is_internal  TINYINT(1)   NOT NULL DEFAULT 0,  -- admin-only notes
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
    INDEX idx_complaint (complaint_id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────
-- SESSIONS  (server-side token store)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
    token      VARCHAR(128) PRIMARY KEY,
    user_id    INT          NOT NULL,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id   (user_id),
    INDEX idx_expires   (expires_at)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────
-- SAMPLE DATA
-- ─────────────────────────────────────────────
INSERT INTO departments (name) VALUES
  ('Technical Support'),
  ('Billing'),
  ('Customer Service'),
  ('Operations'),
  ('Quality Assurance');

-- Passwords are bcrypt hashes of 'Password1!'
INSERT INTO users (name, email, password, role, department) VALUES
  ('Alice Admin',   'admin@demo.com', '$2a$12$LHBd3.VFBRzB.0I7SG.9UeJaAe7xWOfzFBnHpWY/PiLgq.lHVmzZi', 'admin', 'Operations'),
  ('Bob Smith',     'bob@demo.com',   '$2a$12$LHBd3.VFBRzB.0I7SG.9UeJaAe7xWOfzFBnHpWY/PiLgq.lHVmzZi', 'user',  NULL),
  ('Carol White',   'carol@demo.com', '$2a$12$LHBd3.VFBRzB.0I7SG.9UeJaAe7xWOfzFBnHpWY/PiLgq.lHVmzZi', 'user',  NULL),
  ('Dave Agent',    'dave@demo.com',  '$2a$12$LHBd3.VFBRzB.0I7SG.9UeJaAe7xWOfzFBnHpWY/PiLgq.lHVmzZi', 'admin', 'Technical Support');

INSERT INTO complaints (title, description, category, priority, status, user_id, assigned_to, department_id, resolution) VALUES
  ('Login page broken',       'Cannot log in after password reset.',            'Technical',  'high',     'in_progress', 2, 4, 1, NULL),
  ('Wrong invoice amount',    'Invoice #1023 overcharged by $50.',              'Billing',    'medium',   'pending',     3, NULL, 2, NULL),
  ('Slow response times',     'Dashboard takes 30s to load.',                  'Performance','critical', 'in_progress', 2, 4, 1, NULL),
  ('Feature request: export', 'Would like CSV export for reports.',             'Feature',    'low',      'pending',     3, NULL, 3, NULL),
  ('Account locked',          'Account locked after 3 wrong attempts.',        'Technical',  'high',     'resolved',    2, 4, 1, 'Account unlocked and policy updated.');

INSERT INTO status_history (complaint_id, changed_by, old_status, new_status, note) VALUES
  (1, 1, 'pending',     'in_progress', 'Assigned to Dave for investigation.'),
  (3, 1, 'pending',     'in_progress', 'Performance team looking into this.'),
  (5, 4, 'pending',     'in_progress', 'Investigating account lock issue.'),
  (5, 4, 'in_progress', 'resolved',    'Account unlocked and policy updated.');

INSERT INTO comments (complaint_id, user_id, body, is_internal) VALUES
  (1, 2, 'Still cannot log in even after clearing cache.',         0),
  (1, 4, 'Reproducing the issue now — will update shortly.',       0),
  (1, 1, 'Internal: Possible session token bug from last deploy.', 1),
  (3, 2, 'Getting timeouts every morning between 9-10 AM.',        0),
  (5, 2, 'Thank you for resolving this so quickly!',               0);
