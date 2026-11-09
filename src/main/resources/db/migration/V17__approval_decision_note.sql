-- Nguyễn Việt Hải: ghi chú quyết định của Quản lý, tách khỏi reason (lý do của người gửi yêu cầu).
IF COL_LENGTH('dbo.approvals', 'decision_note') IS NULL
BEGIN
    ALTER TABLE dbo.approvals ADD decision_note NVARCHAR(2000) NULL;
END;
