-- Inquiry không còn gắn Buyer; khách lưu full_name/phone trên inquiries.
IF OBJECT_ID('fk_inquiries_buyer', 'F') IS NOT NULL
    EXEC('ALTER TABLE inquiries DROP CONSTRAINT fk_inquiries_buyer');

IF COL_LENGTH('inquiries', 'buyer_id') IS NOT NULL
    EXEC('ALTER TABLE inquiries DROP COLUMN buyer_id');
