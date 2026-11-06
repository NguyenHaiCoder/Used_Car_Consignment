-- Bỏ trạng thái SUPERSEDED: bản ghi cũ chuyển sang TERMINATED.
IF OBJECT_ID(N'dbo.consignment_agreements', N'U') IS NOT NULL
BEGIN
    UPDATE dbo.consignment_agreements
    SET status = N'TERMINATED'
    WHERE status = N'SUPERSEDED';
END;
