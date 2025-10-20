DELETE FROM payments;
DELETE FROM rentals;
DELETE FROM users;
DELETE FROM cars;

ALTER TABLE users AUTO_INCREMENT = 3;

SET @TEST_PASSWORD_HASH = '$2a$10$0hQ9d4hU2n8l2j3k4h5g6.A9d8n7j6k5l4m3n2o1p0q1r2s3t4u5v6w7x8y9z0a1b2';

INSERT INTO users (id, email, password, first_name, last_name, role) VALUES
(1, 'customer@test.com', @TEST_PASSWORD_HASH, 'John', 'Doe', 'CUSTOMER'),
(2, 'manager@test.com', @TEST_PASSWORD_HASH, 'Jane', 'Smith', 'MANAGER');
