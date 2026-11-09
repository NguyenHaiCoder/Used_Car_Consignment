-- Ép cột chữ Inquiry/Viewing sang NVARCHAR. Một EXEC để Flyway không cắt khối DECLARE.
EXEC(N'
DECLARE @sql nvarchar(max);
DECLARE @n sysname;

-- Drop UNIQUE trên inquiries.code
SELECT @n = kc.name
FROM sys.key_constraints kc
INNER JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id AND ic.is_included_column = 0
INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID(''inquiries'') AND kc.type = ''UQ'' AND c.name = ''code'';
IF @n IS NOT NULL
BEGIN
    SET @sql = N''ALTER TABLE inquiries DROP CONSTRAINT ['' + @n + N'']'';
    EXEC(@sql);
END;

-- Drop CHECK trên inquiries.status
SET @n = NULL;
SELECT @n = cc.name
FROM sys.check_constraints cc
INNER JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID(''inquiries'') AND c.name = ''status'';
IF @n IS NOT NULL
BEGIN
    SET @sql = N''ALTER TABLE inquiries DROP CONSTRAINT ['' + @n + N'']'';
    EXEC(@sql);
END;

IF COL_LENGTH(''inquiries'', ''code'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN code nvarchar(40) NOT NULL'');
IF COL_LENGTH(''inquiries'', ''channel'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN channel nvarchar(40) NULL'');
IF COL_LENGTH(''inquiries'', ''full_name'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN full_name nvarchar(160) NULL'');
IF COL_LENGTH(''inquiries'', ''phone'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN phone nvarchar(30) NULL'');
IF COL_LENGTH(''inquiries'', ''email'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN email nvarchar(180) NULL'');
IF COL_LENGTH(''inquiries'', ''need'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN need nvarchar(40) NULL'');
IF COL_LENGTH(''inquiries'', ''id_number'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN id_number nvarchar(20) NULL'');
IF COL_LENGTH(''inquiries'', ''address'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN address nvarchar(500) NULL'');
IF COL_LENGTH(''inquiries'', ''notes'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN notes nvarchar(2000) NULL'');
IF COL_LENGTH(''inquiries'', ''close_reason'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN close_reason nvarchar(120) NULL'');
IF COL_LENGTH(''inquiries'', ''status'') IS NOT NULL
    EXEC(N''ALTER TABLE inquiries ALTER COLUMN status nvarchar(40) NOT NULL'');

-- Recreate unique code
IF COL_LENGTH(''inquiries'', ''code'') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM sys.key_constraints kc
       INNER JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
       INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
       WHERE kc.parent_object_id = OBJECT_ID(''inquiries'') AND kc.type = ''UQ'' AND c.name = ''code'')
    EXEC(N''ALTER TABLE inquiries ADD CONSTRAINT uq_inquiries_code UNIQUE (code)'');

-- Drop UNIQUE trên viewings.code
SET @n = NULL;
SELECT @n = kc.name
FROM sys.key_constraints kc
INNER JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id AND ic.is_included_column = 0
INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID(''viewings'') AND kc.type = ''UQ'' AND c.name = ''code'';
IF @n IS NOT NULL
BEGIN
    SET @sql = N''ALTER TABLE viewings DROP CONSTRAINT ['' + @n + N'']'';
    EXEC(@sql);
END;

-- Drop CHECK trên viewings.status
SET @n = NULL;
SELECT @n = cc.name
FROM sys.check_constraints cc
INNER JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID(''viewings'') AND c.name = ''status'';
IF @n IS NOT NULL
BEGIN
    SET @sql = N''ALTER TABLE viewings DROP CONSTRAINT ['' + @n + N'']'';
    EXEC(@sql);
END;

IF OBJECT_ID(''viewings'', ''U'') IS NOT NULL AND COL_LENGTH(''viewings'', ''code'') IS NOT NULL
    EXEC(N''ALTER TABLE viewings ALTER COLUMN code nvarchar(40) NOT NULL'');
IF OBJECT_ID(''viewings'', ''U'') IS NOT NULL AND COL_LENGTH(''viewings'', ''status'') IS NOT NULL
    EXEC(N''ALTER TABLE viewings ALTER COLUMN status nvarchar(40) NOT NULL'');
IF OBJECT_ID(''viewings'', ''U'') IS NOT NULL AND COL_LENGTH(''viewings'', ''viewing_type'') IS NOT NULL
    EXEC(N''ALTER TABLE viewings ALTER COLUMN viewing_type nvarchar(40) NULL'');
IF OBJECT_ID(''viewings'', ''U'') IS NOT NULL AND COL_LENGTH(''viewings'', ''location'') IS NOT NULL
    EXEC(N''ALTER TABLE viewings ALTER COLUMN location nvarchar(160) NULL'');
IF OBJECT_ID(''viewings'', ''U'') IS NOT NULL AND COL_LENGTH(''viewings'', ''notes'') IS NOT NULL
    EXEC(N''ALTER TABLE viewings ALTER COLUMN notes nvarchar(1000) NULL'');

IF OBJECT_ID(''viewings'', ''U'') IS NOT NULL
   AND COL_LENGTH(''viewings'', ''code'') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM sys.key_constraints kc
       INNER JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
       INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
       WHERE kc.parent_object_id = OBJECT_ID(''viewings'') AND kc.type = ''UQ'' AND c.name = ''code'')
    EXEC(N''ALTER TABLE viewings ADD CONSTRAINT uq_viewings_code UNIQUE (code)'');
');
