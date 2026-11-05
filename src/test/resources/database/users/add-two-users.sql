INSERT INTO users (id, email, first_name, last_name, password, is_deleted) VALUES
  (10, 'manager@example.com', 'Manager', 'User', '$2a$10$hashedPasswordForTests123456789', false),
  (11, 'customer@example.com', 'John', 'Doe', '$2a$10$hashedPasswordForTests123456789', false);

INSERT INTO users_roles (user_id, role_id) VALUES
(10, 2),
(11, 1);
