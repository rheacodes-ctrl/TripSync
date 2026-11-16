
USE tripsync;

-- USERS
INSERT INTO users (user_id, name, email, role, phone)
VALUES
('U001', 'Ishi', 'ishi@example.com', 'TRIP_ADMIN', '9876543210'),
('U002', 'Rhea', 'rhea@example.com', 'TRAVELER', '9876543211'),
('U003', 'Shreya', 'shreya@example.com', 'TRAVELER', '9876543212');

-- TRIP
INSERT INTO trips
(trip_id, trip_name, description, start_date, end_date, budget, admin_id)
VALUES
('T001', 'Goa Vacation', 'A fun trip with friends',
 '2027-01-10', '2027-01-15', 60000.00, 'U001');

-- TRIP MEMBERS
INSERT INTO trip_members (trip_id, user_id)
VALUES
('T001', 'U001'),
('T001', 'U002'),
('T001', 'U003');

-- DESTINATIONS
INSERT INTO destinations
(destination_id, trip_id, name, city, country, description,
 arrival_date, departure_date)
VALUES
('D001', 'T001', 'Baga Beach', 'North Goa', 'India',
 'Popular beach destination', '2027-01-10', '2027-01-11'),
('D002', 'T001', 'Fort Aguada', 'Candolim', 'India',
 'Historic Portuguese fort', '2027-01-12', '2027-01-12'),
('D003', 'T001', 'Anjuna Beach', 'Anjuna', 'India',
 'Famous scenic beach', '2027-01-13', '2027-01-14');

-- ACTIVITIES
INSERT INTO activities
(activity_id, destination_id, name, description,
 activity_date, activity_time, estimated_cost, status)
VALUES
('A001', 'D001', 'Water Sports', 'Water sports at Baga',
 '2027-01-10', '10:00:00', 1500.00, 'PLANNED'),
('A002', 'D001', 'Beach Dinner', 'Dinner by the beach',
 '2027-01-11', '19:00:00', 2000.00, 'PLANNED'),
('A003', 'D002', 'Fort Tour', 'Explore the historic fort',
 '2027-01-12', '11:00:00', 500.00, 'PLANNED');

-- ACTIVITY PARTICIPANTS
INSERT INTO activity_participants (activity_id, user_id)
VALUES
('A001', 'U002'),
('A001', 'U003'),
('A002', 'U001'),
('A002', 'U002'),
('A003', 'U001');

-- ACTIVITY VOTES
INSERT INTO activity_votes (vote_id, activity_id, user_id)
VALUES
('V001', 'A001', 'U002'),
('V002', 'A001', 'U003');

-- RESERVATIONS
INSERT INTO reservations
(reservation_id, trip_id, reservation_type, provider_name,
 cost, reservation_date, status)
VALUES
('R001', 'T001', 'HOTEL', 'Taj Resort',
 12000.00, '2027-01-10', 'CONFIRMED'),
('R002', 'T001', 'TRANSPORT', 'IndiGo',
 8500.00, '2027-01-09', 'CONFIRMED'),
('R003', 'T001', 'ACTIVITY', 'Water Sports Provider',
 1500.00, '2027-01-10', 'CONFIRMED');

-- EXPENSES
INSERT INTO expenses
(expense_id, trip_id, description, amount, paid_by, expense_date)
VALUES
('E001', 'T001', 'Group Dinner', 3000.00, 'U002', '2027-01-10');

-- EXPENSE SPLITS
INSERT INTO expense_splits
(split_id, expense_id, user_id, share_amount, is_settled)
VALUES
('S001', 'E001', 'U001', 1000.00, FALSE),
('S002', 'E001', 'U002', 1000.00, TRUE),
('S003', 'E001', 'U003', 1000.00, FALSE);

-- PAYMENT SETTLEMENT
INSERT INTO payments
(payment_id, trip_id, from_user_id, to_user_id, amount, status)
VALUES
('P001', 'T001', 'U003', 'U002', 1000.00, 'COMPLETED');

-- DOCUMENTS
INSERT INTO documents
(document_id, trip_id, uploaded_by, file_name, file_path)
VALUES
('DOC001', 'T001', 'U001', 'hotel-booking.pdf',
 'uploads/hotel-booking.pdf');
