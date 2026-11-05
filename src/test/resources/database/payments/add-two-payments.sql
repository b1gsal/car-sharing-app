INSERT INTO rentals (id, rental_date, return_date, actual_return_date, car_id, user_id) VALUES
(10, '2026-11-01', '2026-11-05', NULL, 1, 11),
(11, '2026-10-01', '2026-10-05', '2026-10-05', 2, 11);

INSERT INTO payments (id, status, type, rental_id, session_url, session_id, amount_to_pay, created_at) VALUES
(10, 'EXPIRED', 'PAYMENT', 10, 'https://checkout.stripe.com/session10', 'cs_test_10', 120.00, NOW()),
(11, 'PAID', 'PAYMENT', 11, 'https://checkout.stripe.com/session11', 'cs_test_11', 225.00, NOW());