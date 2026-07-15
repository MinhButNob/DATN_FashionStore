-- =============================================
-- SQL Migration: Nâng cấp bảng chats
-- Thêm cột chat_type và updated_at
-- =============================================

-- Thêm cột chat_type nếu chưa có
ALTER TABLE chats ADD COLUMN IF NOT EXISTS chat_type VARCHAR(20) DEFAULT 'STAFF';

-- Thêm cột updated_at nếu chưa có
ALTER TABLE chats ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Cập nhật giá trị updated_at cho các bản ghi cũ
UPDATE chats SET updated_at = created_at WHERE updated_at IS NULL;

-- =============================================
-- Chat status reference:
-- ACTIVE        - chat đang hoạt động (legacy)
-- WAITING_STAFF - đang chờ nhân viên tiếp nhận
-- IN_PROGRESS   - đang được nhân viên hỗ trợ
-- CLOSED        - đã đóng
--
-- Chat type reference:
-- AI    - chat với Haru AI
-- STAFF - chat với nhân viên
-- =============================================
