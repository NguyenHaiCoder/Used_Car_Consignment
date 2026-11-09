-- Điều khoản + chữ ký base64 trên hợp đồng bán xe (UI Sales).
IF COL_LENGTH('dbo.sales_contracts', 'payment_terms') IS NULL
BEGIN
    ALTER TABLE dbo.sales_contracts ADD payment_terms NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH('dbo.sales_contracts', 'handover_terms') IS NULL
BEGIN
    ALTER TABLE dbo.sales_contracts ADD handover_terms NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH('dbo.sales_contracts', 'termination_terms') IS NULL
BEGIN
    ALTER TABLE dbo.sales_contracts ADD termination_terms NVARCHAR(MAX) NULL;
END;

-- Chữ ký dạng data-URL cần cột dài hơn 500.
IF COL_LENGTH('dbo.sales_contracts', 'seller_signature_path') IS NOT NULL
BEGIN
    ALTER TABLE dbo.sales_contracts ALTER COLUMN seller_signature_path NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH('dbo.sales_contracts', 'buyer_signature_path') IS NOT NULL
BEGIN
    ALTER TABLE dbo.sales_contracts ALTER COLUMN buyer_signature_path NVARCHAR(MAX) NULL;
END;
