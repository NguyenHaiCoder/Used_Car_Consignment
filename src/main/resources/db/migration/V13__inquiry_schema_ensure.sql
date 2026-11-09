-- Đảm bảo schema Inquiry/Reservation (SQL Server). Idempotent khi mở Spring Boot.
-- Chạy sau V10–V12: vá máy local thiếu cột / Hibernate ddl-auto từng fail.

IF COL_LENGTH('inquiries', 'full_name') IS NULL
    EXEC('ALTER TABLE inquiries ADD full_name nvarchar(160) NULL');

IF COL_LENGTH('inquiries', 'phone') IS NULL
    EXEC('ALTER TABLE inquiries ADD phone nvarchar(30) NULL');

IF COL_LENGTH('inquiries', 'email') IS NULL
    EXEC('ALTER TABLE inquiries ADD email nvarchar(180) NULL');

IF COL_LENGTH('inquiries', 'need') IS NULL
    EXEC('ALTER TABLE inquiries ADD need nvarchar(40) NULL');

IF COL_LENGTH('inquiries', 'id_number') IS NULL
    EXEC('ALTER TABLE inquiries ADD id_number nvarchar(20) NULL');

IF COL_LENGTH('inquiries', 'address') IS NULL
    EXEC('ALTER TABLE inquiries ADD address nvarchar(500) NULL');

-- consent_stored: NOT NULL + DEFAULT 0 (bảng đã có dữ liệu)
IF COL_LENGTH('inquiries', 'consent_stored') IS NULL
    EXEC('ALTER TABLE inquiries ADD consent_stored bit NOT NULL CONSTRAINT DF_inquiries_consent_stored DEFAULT 0');

-- Cột đã có nhưng còn NULL → backfill + siết NOT NULL
IF COL_LENGTH('inquiries', 'consent_stored') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.columns c
       INNER JOIN sys.tables t ON c.object_id = t.object_id
       WHERE t.name = 'inquiries' AND c.name = 'consent_stored' AND c.is_nullable = 1
   )
BEGIN
    EXEC('UPDATE inquiries SET consent_stored = 0 WHERE consent_stored IS NULL');
    IF OBJECT_ID('DF_inquiries_consent_stored', 'D') IS NULL
        EXEC('ALTER TABLE inquiries ADD CONSTRAINT DF_inquiries_consent_stored DEFAULT 0 FOR consent_stored');
    EXEC('ALTER TABLE inquiries ALTER COLUMN consent_stored bit NOT NULL');
END;

IF OBJECT_ID('fk_inquiries_buyer', 'F') IS NOT NULL
    EXEC('ALTER TABLE inquiries DROP CONSTRAINT fk_inquiries_buyer');

IF COL_LENGTH('inquiries', 'buyer_id') IS NOT NULL
    EXEC('ALTER TABLE inquiries DROP COLUMN buyer_id');

IF OBJECT_ID('fk_reservations_buyer', 'F') IS NOT NULL
    EXEC('ALTER TABLE reservations DROP CONSTRAINT fk_reservations_buyer');

IF COL_LENGTH('reservations', 'buyer_id') IS NOT NULL
    EXEC('ALTER TABLE reservations DROP COLUMN buyer_id');
