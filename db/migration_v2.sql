-- Migration v2: secure the accounts of an existing installation
--
-- Run after schema.sql + seed_data.sql on a database that already holds
-- plain-text or SHA-256 passwords.
--
-- Two things happen here:
--   1. The password column is widened to 255 characters so a PBKDF2 hash
--      (pbkdf2$<iterations>$<salt>$<hash>) fits comfortably.
--   2. The demo accounts are given PBKDF2-HMAC-SHA256 hashes.
--
-- To hash a password of your own, run:
--     java -cp ".;lib/*" util.PasswordHashTool "MyPassword"
-- and paste the printed value into an UPDATE statement.
--
-- Accounts that are NOT listed here keep their existing password. The
-- application still verifies legacy plain-text and SHA-256 rows, and it
-- silently upgrades them to PBKDF2 the first time the user signs in - so no
-- one gets locked out.
USE employee_performance_db;

-- 1. Widen the password column for the PBKDF2 hash format.
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NOT NULL;

-- 2. Make the rating columns mandatory - a CHECK constraint alone still
--    permits NULL, and a NULL rating would break the score calculation.
ALTER TABLE evaluations MODIFY COLUMN communication INT NOT NULL CHECK (communication BETWEEN 1 AND 5);
ALTER TABLE evaluations MODIFY COLUMN technical INT NOT NULL CHECK (technical BETWEEN 1 AND 5);
ALTER TABLE evaluations MODIFY COLUMN punctuality INT NOT NULL CHECK (punctuality BETWEEN 1 AND 5);
ALTER TABLE evaluations MODIFY COLUMN teamwork INT NOT NULL CHECK (teamwork BETWEEN 1 AND 5);
ALTER TABLE feedback MODIFY COLUMN rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5);

-- 3. Replace the demo passwords with PBKDF2 hashes.
--    manager123 and emp123 are the credentials documented in README.md.
UPDATE users SET password = 'pbkdf2$210000$B6uXjdwYvzSGacw8bihxig==$SVqlNsjydYvMtJMAVeqRsAqsRCqNIP1tvLDmQllzYeA='
 WHERE email = 'manager@company.com';

UPDATE users SET password = 'pbkdf2$210000$ofm3xx+yUV1A9+C6M2kl/A==$ae+ARbWzlRwE5/RgOxTbclBi4Nw2wXvlbD7dOnKSLiA='
 WHERE email IN ('priya@company.com', 'amit@company.com', 'sneha@company.com');

-- Verify
SELECT user_id, name, email, role, LEFT(password, 20) AS password_format FROM users;
