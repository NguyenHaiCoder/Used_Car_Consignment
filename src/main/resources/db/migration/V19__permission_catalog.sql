-- Nguyễn Việt Hải: danh mục phân quyền theo chức năng. Thêm nhóm, role sở hữu, thứ tự hiển thị.
-- Dữ liệu do PermissionCatalogSync nạp khi khởi động, 4 quyền chung cũ bị xóa ở đó.
IF OBJECT_ID('dbo.permissions', 'U') IS NOT NULL AND COL_LENGTH('dbo.permissions', 'module_name') IS NULL
BEGIN
    ALTER TABLE dbo.permissions ADD module_name NVARCHAR(120) NULL;
END;
IF OBJECT_ID('dbo.permissions', 'U') IS NOT NULL AND COL_LENGTH('dbo.permissions', 'owner_role') IS NULL
BEGIN
    ALTER TABLE dbo.permissions ADD owner_role VARCHAR(30) NULL;
END;
IF OBJECT_ID('dbo.permissions', 'U') IS NOT NULL AND COL_LENGTH('dbo.permissions', 'sort_order') IS NULL
BEGIN
    ALTER TABLE dbo.permissions ADD sort_order INT NULL;
END;
