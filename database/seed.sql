-- Development seed users only.
-- Password for all accounts: password
-- Change these credentials before using a non-development environment.

INSERT INTO crop (name_bn, name_en, image_url, is_active, selectable, display_order)
SELECT 'ধান', 'Rice', 'https://api.example.com/assets/crops/rice.png', TRUE, TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM crop WHERE LOWER(name_en) = 'rice');

INSERT INTO crop (name_bn, name_en, image_url, is_active, selectable, display_order)
SELECT 'বেগুন', 'Eggplant', 'https://api.example.com/assets/crops/eggplant.png', TRUE, TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM crop WHERE LOWER(name_en) = 'eggplant');

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
