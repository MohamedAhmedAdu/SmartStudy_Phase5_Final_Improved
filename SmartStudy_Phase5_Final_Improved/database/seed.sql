USE smartstudy;
-- Default administrator credentials: admin@smartstudy.local / Admin123!
INSERT INTO administrators(full_name,email,password_hash)
VALUES ('System Administrator','admin@smartstudy.local','$2a$10$mU7xU2VOkfI7SDW845LaeO5/d1AKC.RxjCQGFDSwULHVDr.TD5MTq')
ON DUPLICATE KEY UPDATE full_name='System Administrator';
