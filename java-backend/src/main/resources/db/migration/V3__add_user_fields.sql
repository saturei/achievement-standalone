-- =====================================================
-- Add DingTalk user fields to user_configs table
-- =====================================================

ALTER TABLE user_configs ADD COLUMN user_id VARCHAR(256);
ALTER TABLE user_configs ADD COLUMN avatar VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_user_configs_user_id ON user_configs(user_id);
