-- Hợp đồng ký gửi 1-1 với hồ sơ: bỏ cột version_no, thêm UNIQUE trên consignment_id.
-- Bảng chưa tồn tại (DB mới) thì bỏ qua: Hibernate tự tạo bảng đúng schema mới.
IF OBJECT_ID(N'dbo.consignment_agreements', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.consignment_agreements', N'version_no') IS NOT NULL
    BEGIN
        DECLARE @df sysname;
        SELECT @df = dc.name
        FROM sys.default_constraints dc
        INNER JOIN sys.columns c
            ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.consignment_agreements')
          AND c.name = N'version_no';
        IF @df IS NOT NULL
            EXEC(N'ALTER TABLE dbo.consignment_agreements DROP CONSTRAINT [' + @df + N']');
        ALTER TABLE dbo.consignment_agreements DROP COLUMN version_no;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes i
        INNER JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
        INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE i.object_id = OBJECT_ID(N'dbo.consignment_agreements')
          AND i.is_unique = 1 AND i.is_primary_key = 0
          AND c.name = N'consignment_id'
        GROUP BY i.index_id
        HAVING COUNT(*) = 1
    )
        ALTER TABLE dbo.consignment_agreements
            ADD CONSTRAINT UQ_consignment_agreements_consignment UNIQUE (consignment_id);
END;
