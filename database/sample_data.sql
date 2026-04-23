-- =====================================================
-- 成果管理模块示例数据
-- =====================================================

-- 插入示例产品
INSERT INTO products (id, name, code, achievement_count, status, owner) VALUES
('P001', '企业管理平台', 'EMP', 5, 'active', '张三'),
('P002', '数据分析系统', 'DAS', 3, 'active', '李四'),
('P003', '移动办公应用', 'MOA', 2, 'active', '王五');

-- 插入示例模块
INSERT INTO modules (id, product_id, product_name, name, value_type, priority, status, owner) VALUES
('M001', 'P001', '企业管理平台', '用户管理模块', '核心功能', '高', '已登记', '张三'),
('M002', 'P001', '企业管理平台', '权限管理模块', '核心功能', '高', '已登记', '张三'),
('M003', 'P002', '数据分析系统', '报表引擎', '核心功能', '高', '已登记', '李四'),
('M004', 'P003', '移动办公应用', '消息推送', '基础支撑', '中', '已登记', '王五');

-- 插入示例用户
INSERT INTO users (id, username, password_hash, role, email, status) VALUES
('U001', 'admin', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYqVqxqZ', 'admin', 'admin@example.com', 'active'),
('U002', 'zhangsan', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYqVqxqZ', 'user', 'zhangsan@example.com', 'active'),
('U003', 'lisi', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYqVqxqZ', 'user', 'lisi@example.com', 'active');

-- 插入示例成果
INSERT INTO achievements (
    id, name, version, product_id, product_name, module_id, module_name, 
    type, description, status, owner, pre_register_time, register_time, 
    record_time, created_by, created_at, updated_at
) VALUES
-- 预注册状态
('用户认证服务_V1.0.0', '用户认证服务', 'V1.0.0', 'P001', '企业管理平台', 'M001', '用户管理模块', 
 '功能', '统一的用户认证服务，支持多种认证方式', 'pre_register', '张三', 
 '2024-01-15 10:00:00', NULL, NULL, 'admin', '2024-01-15 10:00:00', '2024-01-15 10:00:00'),

-- 注册状态
('权限控制引擎_V1.0.0', '权限控制引擎', 'V1.0.0', 'P001', '企业管理平台', 'M002', '权限管理模块', 
 '模块', '基于RBAC的权限控制引擎', 'register', '张三', 
 '2024-01-10 09:00:00', '2024-02-01 14:00:00', NULL, 'admin', '2024-01-10 09:00:00', '2024-02-01 14:00:00'),

-- 登记状态
('智能报表生成器_V1.0.0', '智能报表生成器', 'V1.0.0', 'P002', '数据分析系统', 'M003', '报表引擎', 
 '模块', '支持多种数据源的智能报表生成工具', 'recorded', '李四', 
 '2023-12-01 08:00:00', '2024-01-15 10:00:00', '2024-03-01 16:00:00', 'admin', '2023-12-01 08:00:00', '2024-03-01 16:00:00'),

('数据可视化组件_V1.0.0', '数据可视化组件', 'V1.0.0', 'P002', '数据分析系统', 'M003', '报表引擎', 
 '资产', '丰富的数据可视化图表组件库', 'recorded', '李四', 
 '2023-11-15 09:00:00', '2024-01-10 11:00:00', '2024-02-20 15:00:00', 'admin', '2023-11-15 09:00:00', '2024-02-20 15:00:00'),

-- 下架状态
('即时通讯模块_V1.0.0', '即时通讯模块', 'V1.0.0', 'P003', '移动办公应用', 'M004', '消息推送', 
 '模块', '企业级即时通讯解决方案', 'offline', '王五', 
 '2023-10-01 10:00:00', '2023-11-01 14:00:00', '2024-01-01 16:00:00', 'admin', '2023-10-01 10:00:00', '2024-02-15 09:00:00'),

-- 变更版本
('用户认证服务_V1.0.1', '用户认证服务', 'V1.0.1', 'P001', '企业管理平台', 'M001', '用户管理模块', 
 '功能', '增加OAuth2.0认证支持', 'pre_register', '张三', 
 '2024-02-01 10:00:00', NULL, NULL, 'admin', '2024-02-01 10:00:00', '2024-02-01 10:00:00');

-- 插入示例状态变更记录
INSERT INTO achievement_status_records (
    id, achievement_id, achievement_name, from_status, to_status, 
    change_type, change_reason, operator, change_time, created_at
) VALUES
('SR001', '智能报表生成器_V1.0.0', '智能报表生成器', 'pre_register', 'register', 
 '注册', '审批通过', 'admin', '2024-01-15 10:00:00', '2024-01-15 10:00:00'),
('SR002', '智能报表生成器_V1.0.0', '智能报表生成器', 'register', 'recorded', 
 '登记', '验收完成', 'admin', '2024-03-01 16:00:00', '2024-03-01 16:00:00'),
('SR003', '即时通讯模块_V1.0.0', '即时通讯模块', 'recorded', 'offline', 
 '下架', '存在安全漏洞，需要修复', 'admin', '2024-02-15 09:00:00', '2024-02-15 09:00:00');

-- 插入示例版本变更记录
INSERT INTO achievement_version_records (
    id, achievement_name, achievement_id, product_external_version, 
    from_version, to_version, changed_fields, change_description, 
    risk_tags, operator, change_time, created_at
) VALUES
('VR001', '用户认证服务', '用户认证服务_V1.0.0', 'V1.0.0', 
 'V1.0.0', 'V1.0.1', 
 '{"description": {"old": "统一的用户认证服务，支持多种认证方式", "new": "增加OAuth2.0认证支持"}}',
 '增加OAuth2.0认证支持', 
 '["新功能"]', 'admin', '2024-02-01 10:00:00', '2024-02-01 10:00:00');

-- 插入示例出库记录
INSERT INTO achievement_checkout_history (
    id, achievement_id, achievement_version, user_id, user_name, 
    checkout_time, purpose, download_count, created_at
) VALUES
('CH001', '智能报表生成器_V1.0.0', 'V1.0.0', 'U002', '张三', 
 '2024-03-10 14:00:00', '用于项目A的报表功能开发', 3, '2024-03-10 14:00:00'),
('CH002', '数据可视化组件_V1.0.0', 'V1.0.0', 'U003', '李四', 
 '2024-03-12 09:00:00', '用于数据大屏展示', 5, '2024-03-12 09:00:00');
