DELETE FROM payments;
DELETE FROM rentals;
DELETE FROM cars;
DELETE FROM users;

ALTER TABLE users AUTO_INCREMENT = 3;
ALTER TABLE cars AUTO_INCREMENT = 2;
ALTER TABLE rentals AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;

SET @TEST_PASSWORD_HASH = '$2a$10$0hQ9d4hU2n8l2j3k4h5g6.A9d8n7j6k5l4m3n2o1p0q1r2s3t4u5v6w7x8y9z0a1b2';

INSERT INTO users (id, email, password, first_name, last_name, role, is_deleted) VALUES
(1, 'manager@test.com', @TEST_PASSWORD_HASH, 'M', 'A', 'MANAGER', 0),
(2, 'customer@test.com', @TEST_PASSWORD_HASH, 'C', 'U', 'CUSTOMER', 0);

INSERT INTO cars (id, brand, model, type, inventory, daily_fee, is_deleted) VALUES
(1, 'Tesla', 'Model 3', 'SEDAN', 10, 50.00, 0);
