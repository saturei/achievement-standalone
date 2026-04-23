-- =====================================================
-- 成果管理系统数据库初始化脚本
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS achievement_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE achievement_db;

-- 用户表（基础表，成果管理可能需要关联用户）
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(20) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',
    email VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 产品表（基础表，成果需要关联产品）
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    achievement_count INTEGER DEFAULT 0,
    description TEXT,
    status VARCHAR(20) DEFAULT 'active',
    owner VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_owner ON products(owner);
CREATE INDEX IF NOT EXISTS idx_products_code ON products(code);

-- 模块表（基础表，成果需要关联模块）
CREATE TABLE IF NOT EXISTS modules (
    id VARCHAR(20) PRIMARY KEY,
    product_id VARCHAR(20) NOT NULL,
    product_name VARCHAR(100),
    name VARCHAR(100) NOT NULL,
    value_type VARCHAR(20),
    priority VARCHAR(10),
    description TEXT,
    status VARCHAR(20) DEFAULT 'registered',
    owner VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_modules_product_id ON modules(product_id);
CREATE INDEX IF NOT EXISTS idx_modules_status ON modules(status);
CREATE INDEX IF NOT EXISTS idx_modules_owner ON modules(owner);

-- =====================================================
-- 成果核心表
-- =====================================================

-- 成果主表
CREATE TABLE IF NOT EXISTS achievements (
    id VARCHAR(50) PRIMARY KEY,
    
    -- 基础数据
    product_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(200),
    organization_id VARCHAR(50),
    organization_name VARCHAR(200),
    department_id VARCHAR(50),
    department_name VARCHAR(200),
    name VARCHAR(200) NOT NULL,
    version VARCHAR(50),
    product_external_version VARCHAR(50),
    has_baseline VARCHAR(20) DEFAULT '无基线',
    requirement_proposer VARCHAR(100),
    achievement_form VARCHAR(100),
    sale_type VARCHAR(100),
    function_list_file VARCHAR(500),
    package_ids JSON,
    application_scenario TEXT,
    module_id VARCHAR(50),
    module_name VARCHAR(200),
    type VARCHAR(50),
    description TEXT,
    owner VARCHAR(100),
    
    -- 计划数据
    achievement_target TEXT,
    planned_acceptance_date DATE,
    acceptance_method TEXT,
    acceptor VARCHAR(200),
    related_project_id VARCHAR(50),
    related_project_name VARCHAR(200),
    related_order_id VARCHAR(50),
    related_order_name VARCHAR(200),
    acceptance_requirements TEXT,
    acceptance_organization VARCHAR(200),
    
    -- 变更数据
    change_reason TEXT,
    
    -- 实际数据
    deliverables TEXT,
    code_repository_url VARCHAR(500),
    demo_url VARCHAR(500),
    actual_acceptance_date DATE,
    
    -- 系统审计数据
    status VARCHAR(50) DEFAULT 'pre_register',
    estimated_acceptance_month VARCHAR(20),
    pre_register_time TIMESTAMP,
    register_time TIMESTAMP,
    record_time TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 其他
    risk_tags JSON
);

-- 成果表索引
CREATE INDEX IF NOT EXISTS idx_achievements_product_id ON achievements(product_id);
CREATE INDEX IF NOT EXISTS idx_achievements_module_id ON achievements(module_id);
CREATE INDEX IF NOT EXISTS idx_achievements_status ON achievements(status);
CREATE INDEX IF NOT EXISTS idx_achievements_type ON achievements(type);
CREATE INDEX IF NOT EXISTS idx_achievements_owner ON achievements(owner);
CREATE INDEX IF NOT EXISTS idx_achievements_pre_register_time ON achievements(pre_register_time);
CREATE INDEX IF NOT EXISTS idx_achievements_product_external_version ON achievements(product_external_version);

-- =====================================================
-- 成果相关记录表
-- =====================================================

-- 成果状态变更记录表
CREATE TABLE IF NOT EXISTS achievement_status_records (
    id VARCHAR(50) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    achievement_name VARCHAR(200) NOT NULL,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    change_reason TEXT,
    operator VARCHAR(100),
    change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_status_records_achievement_id ON achievement_status_records(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_status_records_change_time ON achievement_status_records(change_time);

-- 成果版本变更记录表
CREATE TABLE IF NOT EXISTS achievement_version_records (
    id VARCHAR(50) PRIMARY KEY,
    achievement_name VARCHAR(100) NOT NULL,
    achievement_id VARCHAR(50) NOT NULL,
    product_external_version VARCHAR(20) NOT NULL,
    from_version VARCHAR(20) NOT NULL,
    to_version VARCHAR(20) NOT NULL,
    changed_fields JSON NOT NULL,
    change_description TEXT,
    risk_tags JSON,
    operator VARCHAR(50),
    change_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_achievement_version_records_achievement_name ON achievement_version_records(achievement_name);
CREATE INDEX IF NOT EXISTS idx_achievement_version_records_achievement_id ON achievement_version_records(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_version_records_product_external_version ON achievement_version_records(product_external_version);
CREATE INDEX IF NOT EXISTS idx_achievement_version_records_change_time ON achievement_version_records(change_time);

-- 成果附件表
CREATE TABLE IF NOT EXISTS achievement_attachments (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    achievement_version VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    size INTEGER,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploader VARCHAR(50),
    file_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_attachments_achievement_id ON achievement_attachments(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_attachments_upload_time ON achievement_attachments(upload_time);

-- 成果出库历史表
CREATE TABLE IF NOT EXISTS achievement_checkout_history (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    achievement_version VARCHAR(20),
    user_id VARCHAR(20),
    user_name VARCHAR(50),
    checkout_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    purpose TEXT,
    download_count INTEGER DEFAULT 0,
    return_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_achievement_checkout_history_achievement_id ON achievement_checkout_history(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_checkout_history_user_id ON achievement_checkout_history(user_id);
CREATE INDEX IF NOT EXISTS idx_achievement_checkout_history_checkout_time ON achievement_checkout_history(checkout_time);

-- 成果版本历史表（兼容旧版本）
CREATE TABLE IF NOT EXISTS achievement_version_history (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    description TEXT,
    change_reason TEXT,
    change_type VARCHAR(20),
    operator VARCHAR(50),
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parent_version VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_version_history_achievement_id ON achievement_version_history(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_version_history_operation_time ON achievement_version_history(operation_time);

-- =====================================================
-- 插入示例数据
-- =====================================================

-- 插入示例用户
INSERT INTO users (id, username, password_hash, role, email, phone, status) VALUES
('U001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin', 'admin@example.com', '13800138000', 'active'),
('U002', 'user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'user', 'user1@example.com', '13800138001', 'active'),
('U003', 'user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'user', 'user2@example.com', '13800138002', 'active');

-- 插入示例产品
INSERT INTO products (id, name, code, achievement_count, description, status, owner) VALUES
('P001', '企业管理系统', 'EMS-001', 5, '企业级管理系统产品', 'active', '张三'),
('P002', '数据分析平台', 'DAP-001', 3, '大数据分析平台', 'active', '李四'),
('P003', '移动办公应用', 'MOA-001', 2, '移动办公解决方案', 'active', '王五');

-- 插入示例模块
INSERT INTO modules (id, product_id, product_name, name, value_type, priority, description, status, owner) VALUES
('M001', 'P001', '企业管理系统', '用户管理模块', '核心', '高', '用户权限管理', 'registered', '张三'),
('M002', 'P001', '企业管理系统', '订单管理模块', '核心', '高', '订单处理流程', 'registered', '李四'),
('M003', 'P002', '数据分析平台', '数据可视化模块', '增值', '中', '数据展示功能', 'registered', '王五'),
('M004', 'P002', '数据分析平台', '报表生成模块', '核心', '高', '报表自动生成', 'registered', '赵六'),
('M005', 'P003', '移动办公应用', '消息推送模块', '核心', '高', '实时消息推送', 'registered', '钱七');

-- 插入示例成果
INSERT INTO achievements (
    id, name, version, product_id, product_name, product_external_version,
    organization_id, organization_name, department_id, department_name,
    has_baseline, requirement_proposer, achievement_form, sale_type,
    application_scenario, module_id, module_name, type, description, owner,
    achievement_target, planned_acceptance_date, acceptance_method, acceptor,
    related_project_id, related_project_name, related_order_id, related_order_name,
    acceptance_requirements, status, pre_register_time, created_by
) VALUES
(
    '用户管理模块_V1.0.0', '用户管理模块', 'V1.0.0', 'P001', '企业管理系统', 'V1.0.0',
    'ORG001', '研发中心', 'DEPT001', '软件开发部',
    '有基线', '产品部', '软件模块', '可售',
    '企业内部用户管理', 'M001', '用户管理模块', '模块', '用户权限管理和认证功能', '张三',
    '实现用户增删改查和权限管理', '2024-06-30', '内部验收', '李四',
    'PROJ001', '企业管理系统V2.0', 'ORD001', '订单管理系统',
    '完成所有功能测试，通过安全审计', 'recorded', '2024-01-15 10:00:00', 'admin'
),
(
    '订单管理模块_V1.0.0', '订单管理模块', 'V1.0.0', 'P001', '企业管理系统', 'V1.0.0',
    'ORG001', '研发中心', 'DEPT001', '软件开发部',
    '无基线', '产品部', '软件模块', '可售',
    '企业订单处理', 'M002', '订单管理模块', '模块', '订单全流程管理', '李四',
    '实现订单创建、审批、执行流程', '2024-07-15', '内部验收', '王五',
    'PROJ001', '企业管理系统V2.0', 'ORD002', '供应链系统',
    '完成流程测试，性能达标', 'register', '2024-02-20 14:30:00', 'admin'
),
(
    '数据可视化模块_V1.0.0', '数据可视化模块', 'V1.0.0', 'P002', '数据分析平台', 'V1.0.0',
    'ORG002', '数据中心', 'DEPT002', '数据分析部',
    '有基线', '客户需求', '软件模块', '可售',
    '数据大屏展示', 'M003', '数据可视化模块', '模块', '数据图表展示功能', '王五',
    '支持多种图表类型，实时数据更新', '2024-08-30', '客户验收', '赵六',
    'PROJ002', '数据平台建设', 'ORD003', '数据中台项目',
    '图表渲染性能达标，支持自定义配置', 'pre_register', '2024-03-10 09:00:00', 'admin'
);

-- 插入示例状态变更记录
INSERT INTO achievement_status_records (
    id, achievement_id, achievement_name, from_status, to_status, 
    change_type, change_reason, operator, change_time
) VALUES
('SR001', '用户管理模块_V1.0.0', '用户管理模块', 'pre_register', 'register', 
 '状态流转', '完成预注册，进入注册阶段', 'admin', '2024-02-01 10:00:00'),
('SR002', '用户管理模块_V1.0.0', '用户管理模块', 'register', 'recorded', 
 '状态流转', '验收通过，完成登记', 'admin', '2024-03-15 15:00:00'),
('SR003', '订单管理模块_V1.0.0', '订单管理模块', 'pre_register', 'register', 
 '状态流转', '完成预注册，进入注册阶段', 'admin', '2024-02-25 11:00:00');

-- 插入示例版本变更记录
INSERT INTO achievement_version_records (
    id, achievement_name, achievement_id, product_external_version,
    from_version, to_version, changed_fields, change_description, 
    operator, change_time, created_at
) VALUES
('VR001', '用户管理模块', '用户管理模块_V1.0.0', 'V1.0.0',
 'V0.0.0', 'V1.0.0', '["name", "description", "owner"]', '初始版本创建',
 'admin', '2024-01-15 10:00:00', '2024-01-15 10:00:00');

-- =====================================================
-- 完成
-- =====================================================
SELECT '数据库初始化完成！' AS message;
