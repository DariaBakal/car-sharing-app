DELETE FROM rentals;
DELETE FROM users;
DELETE FROM cars;

ALTER TABLE users AUTO_INCREMENT = 3;
ALTER TABLE cars AUTO_INCREMENT = 2;
ALTER TABLE rentals AUTO_INCREMENT = 3;

SET @TEST_PASSWORD_HASH = '2a10$0hQ9d4hU2n8l2j3k4h5g6.A9d8n7j6k5l4m3n2o1p0q1r2s3t4u5v6w7x8y9z0a1b2';

INSERT INTO users (id, email, password, first_name, last_name, role) VALUES (
1,
'customer@test.com',
@TEST_PASSWORD_HASH,
'C',
'U',
'CUSTOMER'
);

INSERT INTO users (id, email, password, first_name, last_name, role) VALUES (
2,
'manager@test.com',
@TEST_PASSWORD_HASH,
'M',
'A',
'MANAGER'
);

INSERT INTO cars (id, brand, model, type, inventory, daily_fee) VALUES (
1,
'Tesla',
'Model 3',
'SEDAN',
10,
50.00
);

INSERT INTO rentals (id, rental_date, return_date, actual_return_date, car_id, user_id) VALUES (
1,
CURRENT_DATE(),
DATE_ADD(CURRENT_DATE(), INTERVAL 3 DAY),
NULL,
1,
1
);

INSERT INTO rentals (id, rental_date, return_date, actual_return_date, car_id, user_id) VALUES (
2,
DATE_SUB(CURRENT_DATE(), INTERVAL 5 DAY),
DATE_SUB(CURRENT_DATE(), INTERVAL 2 DAY),
DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY),
1,
2
);