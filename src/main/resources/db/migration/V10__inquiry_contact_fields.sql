-- Public contact fields on inquiries (idempotent; EXEC để Flyway không cắt khối IF).
IF COL_LENGTH('inquiries', 'full_name') IS NULL
    EXEC('ALTER TABLE inquiries ADD full_name nvarchar(160) NULL');

IF COL_LENGTH('inquiries', 'phone') IS NULL
    EXEC('ALTER TABLE inquiries ADD phone nvarchar(30) NULL');

IF COL_LENGTH('inquiries', 'email') IS NULL
    EXEC('ALTER TABLE inquiries ADD email nvarchar(180) NULL');

IF COL_LENGTH('inquiries', 'need') IS NULL
    EXEC('ALTER TABLE inquiries ADD need nvarchar(40) NULL');

-- NOT NULL + DEFAULT 0: bắt buộc khi bảng inquiries đã có dòng
IF COL_LENGTH('inquiries', 'consent_stored') IS NULL
    EXEC('ALTER TABLE inquiries ADD consent_stored bit NOT NULL CONSTRAINT DF_inquiries_consent_stored DEFAULT 0');
