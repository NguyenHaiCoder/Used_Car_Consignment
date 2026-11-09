-- Nguyễn Việt Hải: commit "fix: nvarchar" gắn @Nationalized cho các field dưới đây nhưng không đổi kiểu cột.
-- DB đã có sẵn (ddl-auto update không đổi kiểu cột) vẫn là varchar, driver SQL Server không đọc varchar thành NCHAR
-- nên mọi trang đọc hồ sơ ký gửi, xe, tin đăng đều lỗi 500. Đổi các cột sang nvarchar cùng độ dài.
-- Index một cột trên cột cần đổi được gỡ ra rồi tạo lại (UNIQUE giữ nguyên là UNIQUE). Chạy lại nhiều lần vẫn an toàn.
EXEC(N'
DECLARE @t sysname, @c sysname, @len int, @notnull bit;
DECLARE @ix sysname, @isUnique bit, @isConstraint bit, @sql nvarchar(max);
DECLARE @recreate TABLE (name sysname, isUnique bit, isConstraint bit);

DECLARE cols CURSOR LOCAL FAST_FORWARD FOR
    SELECT t, c, len, notnull FROM (VALUES
        (N''consignment_agreements'', N''code'', 40, 1),
        (N''consignments'', N''code'', 40, 1),
        (N''consignors'', N''account_number'', 40, 1),
        (N''consignors'', N''id_number'', 20, 1),
        (N''consignors'', N''phone'', 30, 1),
        (N''vehicles'', N''chassis_number'', 40, 0),
        (N''vehicles'', N''engine_number'', 40, 0),
        (N''vehicles'', N''fuel_type'', 40, 0),
        (N''vehicles'', N''license_plate'', 20, 0),
        (N''vehicles'', N''transmission'', 40, 0),
        (N''vehicles'', N''vehicle_type'', 40, 0),
        (N''vehicles'', N''vin'', 32, 1)
    ) v(t, c, len, notnull);

OPEN cols;
FETCH NEXT FROM cols INTO @t, @c, @len, @notnull;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = @t AND COLUMN_NAME = @c AND DATA_TYPE = ''varchar'')
    BEGIN
        DELETE FROM @recreate;
        INSERT INTO @recreate (name, isUnique, isConstraint)
        SELECT i.name, i.is_unique, i.is_unique_constraint
        FROM sys.indexes i
        JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
        JOIN sys.columns cl ON cl.object_id = ic.object_id AND cl.column_id = ic.column_id
        WHERE i.object_id = OBJECT_ID(@t) AND cl.name = @c AND i.is_primary_key = 0
          AND (SELECT COUNT(*) FROM sys.index_columns x
               WHERE x.object_id = i.object_id AND x.index_id = i.index_id) = 1;

        DECLARE drops CURSOR LOCAL FAST_FORWARD FOR SELECT name, isConstraint FROM @recreate;
        OPEN drops;
        FETCH NEXT FROM drops INTO @ix, @isConstraint;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @sql = CASE WHEN @isConstraint = 1
                THEN N''ALTER TABLE '' + QUOTENAME(@t) + N'' DROP CONSTRAINT '' + QUOTENAME(@ix)
                ELSE N''DROP INDEX '' + QUOTENAME(@ix) + N'' ON '' + QUOTENAME(@t) END;
            EXEC(@sql);
            FETCH NEXT FROM drops INTO @ix, @isConstraint;
        END;
        CLOSE drops;
        DEALLOCATE drops;

        SET @sql = N''ALTER TABLE '' + QUOTENAME(@t) + N'' ALTER COLUMN '' + QUOTENAME(@c)
                 + N'' NVARCHAR('' + CAST(@len AS nvarchar(10)) + N'')''
                 + CASE WHEN @notnull = 1 THEN N'' NOT NULL'' ELSE N'' NULL'' END;
        EXEC(@sql);

        DECLARE adds CURSOR LOCAL FAST_FORWARD FOR SELECT name, isUnique, isConstraint FROM @recreate;
        OPEN adds;
        FETCH NEXT FROM adds INTO @ix, @isUnique, @isConstraint;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @sql = CASE
                WHEN @isConstraint = 1 THEN N''ALTER TABLE '' + QUOTENAME(@t) + N'' ADD CONSTRAINT '' + QUOTENAME(@ix)
                                            + N'' UNIQUE ('' + QUOTENAME(@c) + N'')''
                WHEN @isUnique = 1 THEN N''CREATE UNIQUE INDEX '' + QUOTENAME(@ix) + N'' ON '' + QUOTENAME(@t)
                                       + N'' ('' + QUOTENAME(@c) + N'')''
                ELSE N''CREATE INDEX '' + QUOTENAME(@ix) + N'' ON '' + QUOTENAME(@t) + N'' ('' + QUOTENAME(@c) + N'')'' END;
            EXEC(@sql);
            FETCH NEXT FROM adds INTO @ix, @isUnique, @isConstraint;
        END;
        CLOSE adds;
        DEALLOCATE adds;
    END;
    FETCH NEXT FROM cols INTO @t, @c, @len, @notnull;
END;
CLOSE cols;
DEALLOCATE cols;
');
