-- Reservation lấy khách từ Inquiry; bỏ buyer_id.
IF OBJECT_ID('fk_reservations_buyer', 'F') IS NOT NULL
    EXEC('ALTER TABLE reservations DROP CONSTRAINT fk_reservations_buyer');

IF COL_LENGTH('reservations', 'buyer_id') IS NOT NULL
    EXEC('ALTER TABLE reservations DROP COLUMN buyer_id');

IF COL_LENGTH('inquiries', 'id_number') IS NULL
    EXEC('ALTER TABLE inquiries ADD id_number nvarchar(20) NULL');

IF COL_LENGTH('inquiries', 'address') IS NULL
    EXEC('ALTER TABLE inquiries ADD address nvarchar(500) NULL');
