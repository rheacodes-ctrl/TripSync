
CREATE DATABASE IF NOT EXISTS tripsync;
USE tripsync;

-- 1. USERS
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role ENUM('TRAVELER', 'TRIP_ADMIN') NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. TRIPS
CREATE TABLE trips (
    trip_id VARCHAR(36) PRIMARY KEY,
    trip_name VARCHAR(150) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    budget DECIMAL(12,2) NOT NULL,
    admin_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_trip_dates
        CHECK (end_date >= start_date),
    CONSTRAINT chk_trip_budget
        CHECK (budget > 0),

    FOREIGN KEY (admin_id) REFERENCES users(user_id)
);

-- 3. TRIP MEMBERS (Many-to-Many)
CREATE TABLE trip_members (
    trip_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (trip_id, user_id),
    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
        ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- 4. DESTINATIONS
CREATE TABLE destinations (
    destination_id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    description TEXT,
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL,

    CHECK (departure_date >= arrival_date),
    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
        ON DELETE CASCADE
);

-- 5. ACTIVITIES
CREATE TABLE activities (
    activity_id VARCHAR(36) PRIMARY KEY,
    destination_id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    activity_date DATE NOT NULL,
    activity_time TIME,
    estimated_cost DECIMAL(12,2) DEFAULT 0,
    status ENUM('PLANNED', 'COMPLETED', 'CANCELLED')
        DEFAULT 'PLANNED',

    CHECK (estimated_cost >= 0),
    FOREIGN KEY (destination_id) REFERENCES destinations(destination_id)
        ON DELETE CASCADE
);

-- 6. ACTIVITY PARTICIPANTS (Many-to-Many)
CREATE TABLE activity_participants (
    activity_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,

    PRIMARY KEY (activity_id, user_id),
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id)
        ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- 7. ACTIVITY VOTES
CREATE TABLE activity_votes (
    vote_id VARCHAR(36) PRIMARY KEY,
    activity_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (activity_id, user_id),
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id)
        ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- 8. RESERVATIONS
CREATE TABLE reservations (
    reservation_id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    reservation_type ENUM('HOTEL', 'TRANSPORT', 'ACTIVITY') NOT NULL,
    provider_name VARCHAR(150) NOT NULL,
    cost DECIMAL(12,2) NOT NULL,
    reservation_date DATE NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED')
        DEFAULT 'PENDING',
    details JSON,

    CHECK (cost >= 0),
    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
        ON DELETE CASCADE
);

-- 9. EXPENSES
CREATE TABLE expenses (
    expense_id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    paid_by VARCHAR(36) NOT NULL,
    expense_date DATE NOT NULL,

    CHECK (amount > 0),
    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
        ON DELETE CASCADE,
    FOREIGN KEY (paid_by) REFERENCES users(user_id)
);

-- 10. EXPENSE SPLITS
CREATE TABLE expense_splits (
    split_id VARCHAR(36) PRIMARY KEY,
    expense_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    share_amount DECIMAL(12,2) NOT NULL,
    is_settled BOOLEAN DEFAULT FALSE,

    CHECK (share_amount >= 0),
    UNIQUE (expense_id, user_id),
    FOREIGN KEY (expense_id) REFERENCES expenses(expense_id)
        ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 11. PAYMENTS / SETTLEMENTS
CREATE TABLE payments (
    payment_id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    from_user_id VARCHAR(36) NOT NULL,
    to_user_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED')
        DEFAULT 'COMPLETED',

    CHECK (amount > 0),
    CHECK (from_user_id <> to_user_id),
    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
        ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(user_id),
    FOREIGN KEY (to_user_id) REFERENCES users(user_id)
);

-- 12. DOCUMENTS
CREATE TABLE documents (
    document_id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    uploaded_by VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
        ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

-- INDEXES
CREATE INDEX idx_trips_admin ON trips(admin_id);
CREATE INDEX idx_destinations_trip ON destinations(trip_id);
CREATE INDEX idx_activities_destination ON activities(destination_id);
CREATE INDEX idx_reservations_trip ON reservations(trip_id);
CREATE INDEX idx_expenses_trip ON expenses(trip_id);
CREATE INDEX idx_expenses_payer ON expenses(paid_by);
CREATE INDEX idx_payments_trip ON payments(trip_id);

-- VERIFY TABLES
SHOW TABLES;
