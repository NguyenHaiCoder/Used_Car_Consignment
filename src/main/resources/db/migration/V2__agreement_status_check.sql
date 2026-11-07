-- Đồng bộ CHECK status với AgreementStatus (Java):
-- DRAFT, PENDING_SIGNATURE, ACTIVE, EXPIRED, TERMINATED
-- (bỏ SUPERSEDED đã migrate ở V1)

IF OBJECT_ID(N'dbo.consignment_agreements', N'U') IS NOT NULL
BEGIN
    UPDATE dbo.consignment_agreements
    SET status = N'TERMINATED'
    WHERE status = N'SUPERSEDED';

    DECLARE @ck sysname;
    SELECT @ck = cc.name
    FROM sys.check_constraints cc
    INNER JOIN sys.columns c
        ON c.object_id = cc.parent_object_id
       AND c.column_id = cc.parent_column_id
    WHERE cc.parent_object_id = OBJECT_ID(N'dbo.consignment_agreements')
      AND c.name = N'status';

    IF @ck IS NOT NULL
        EXEC(N'ALTER TABLE dbo.consignment_agreements DROP CONSTRAINT [' + @ck + N']');

    ALTER TABLE dbo.consignment_agreements
        ADD CONSTRAINT CK_consignment_agreements_status
        CHECK ([status] IN (
            N'DRAFT',
            N'PENDING_SIGNATURE',
            N'ACTIVE',
            N'EXPIRED',
            N'TERMINATED'
        ));
END;
