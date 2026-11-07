-- Public contact fields on inquiries (idempotent; SQL Server needs DEFAULT for NOT NULL on non-empty table).
IF COL_LENGTH('inquiries', 'full_name') IS NULL
BEGIN
    ALTER TABLE inquiries ADD full_name nvarchar(160) NULL;
END;

IF COL_LENGTH('inquiries', 'phone') IS NULL
BEGIN
    ALTER TABLE inquiries ADD phone nvarchar(30) NULL;
END;

IF COL_LENGTH('inquiries', 'email') IS NULL
BEGIN
    ALTER TABLE inquiries ADD email nvarchar(180) NULL;
END;

IF COL_LENGTH('inquiries', 'need') IS NULL
BEGIN
    ALTER TABLE inquiries ADD need nvarchar(40) NULL;
END;

IF COL_LENGTH('inquiries', 'consent_stored') IS NULL
BEGIN
    ALTER TABLE inquiries ADD consent_stored bit NOT NULL
        CONSTRAINT DF_inquiries_consent_stored DEFAULT 0;
END;
