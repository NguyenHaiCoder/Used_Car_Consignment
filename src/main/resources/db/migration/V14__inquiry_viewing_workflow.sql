-- Luồng Tư vấn & lịch xem xe: cột Viewing + closeReason; dọn data mock/cũ.

IF COL_LENGTH('inquiries', 'close_reason') IS NULL
    EXEC('ALTER TABLE inquiries ADD close_reason nvarchar(120) NULL');

IF COL_LENGTH('viewings', 'duration_minutes') IS NULL
    EXEC('ALTER TABLE viewings ADD duration_minutes int NOT NULL CONSTRAINT DF_viewings_duration DEFAULT 45');

IF COL_LENGTH('viewings', 'status') IS NULL
    EXEC('ALTER TABLE viewings ADD status nvarchar(40) NOT NULL CONSTRAINT DF_viewings_status DEFAULT ''BOOKED''');

-- Public/OPEN chưa nhận → NEW
EXEC('UPDATE inquiries SET status = ''NEW'' WHERE status = ''OPEN'' AND assigned_to_user_id IS NULL');

-- Xóa data tư vấn cũ (giữ CONVERTED cho chuỗi sang tên)
IF OBJECT_ID('viewings', 'U') IS NOT NULL
    EXEC('DELETE FROM viewings');

EXEC('DELETE FROM inquiries WHERE status <> ''CONVERTED''');
