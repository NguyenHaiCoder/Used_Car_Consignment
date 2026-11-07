-- Hợp đồng bán: CLOSED → COMPLETED (hoàn tất giao dịch, hợp đồng vẫn còn giá trị).
-- Bàn giao: CLOSED → FINALIZED (đã hoàn tất sau bàn giao).
UPDATE sales_contracts SET status = 'COMPLETED' WHERE status = 'CLOSED';
UPDATE handovers SET status = 'FINALIZED' WHERE status = 'CLOSED';
