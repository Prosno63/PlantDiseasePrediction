-- Development seed users only.
-- Password for all accounts: password
-- Change these credentials before using a non-development environment.

INSERT INTO users (phone_number, password_hash, name, role)
VALUES
    ('01710000001', '$2a$10$ciRGNF2JXfQeGk00bBzxcO52NkH2OLbCy5vhz.zIAW07t6J/E40Lq', 'Farmer One', 'FARMER'),
    ('01710000002', '$2a$10$ciRGNF2JXfQeGk00bBzxcO52NkH2OLbCy5vhz.zIAW07t6J/E40Lq', 'Farmer Two', 'FARMER'),
    ('01710000003', '$2a$10$ciRGNF2JXfQeGk00bBzxcO52NkH2OLbCy5vhz.zIAW07t6J/E40Lq', 'System Admin', 'ADMIN'),
    ('01710000004', '$2a$10$ciRGNF2JXfQeGk00bBzxcO52NkH2OLbCy5vhz.zIAW07t6J/E40Lq', 'Agriculture Expert', 'EXPERT')
ON CONFLICT (phone_number) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    role = EXCLUDED.role;
