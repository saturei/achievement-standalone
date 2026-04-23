-- =====================================================
-- 成果管理模块数据库表结构
-- =====================================================

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
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
-- 成果流程表（可选）
-- =====================================================

-- 成果流程表
CREATE TABLE IF NOT EXISTS achievement_processes (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    step VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    date TIMESTAMP,
    owner VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_processes_achievement_id ON achievement_processes(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_processes_step ON achievement_processes(step);

-- 成果会议表
CREATE TABLE IF NOT EXISTS achievement_meetings (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    process_step VARCHAR(20),
    name VARCHAR(100) NOT NULL,
    date TIMESTAMP NOT NULL,
    attendees TEXT,
    status VARCHAR(20) DEFAULT 'scheduled',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_meetings_achievement_id ON achievement_meetings(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_meetings_date ON achievement_meetings(date);

-- 成果功能表
CREATE TABLE IF NOT EXISTS achievement_functions (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    process_step VARCHAR(20),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_functions_achievement_id ON achievement_functions(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_functions_status ON achievement_functions(status);

-- 成果审批表
CREATE TABLE IF NOT EXISTS achievement_approvals (
    id VARCHAR(20) PRIMARY KEY,
    achievement_id VARCHAR(50) NOT NULL,
    process_step VARCHAR(20),
    name VARCHAR(100) NOT NULL,
    approver VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    date TIMESTAMP,
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_achievement_approvals_achievement_id ON achievement_approvals(achievement_id);
CREATE INDEX IF NOT EXISTS idx_achievement_approvals_status ON achievement_approvals(status);

-- =====================================================
-- 表关系说明
-- =====================================================
--
-- achievements (成果主表)
--   ├── achievement_status_records (状态变更记录)
--   ├── achievement_version_records (版本变更记录)
--   ├── achievement_attachments (附件)
--   ├── achievement_checkout_history (出库历史)
--   ├── achievement_version_history (版本历史)
--   ├── achievement_processes (流程记录)
--   ├── achievement_meetings (会议记录)
--   ├── achievement_functions (功能记录)
--   └── achievement_approvals (审批记录)
--
-- products (产品) ──────── achievements (成果)
-- modules (模块) ──────── achievements (成果)
-- users (用户) ──────── achievements (成果)
--                       ├── achievement_checkout_history (出库历史)
--                       └── achievement_approvals (审批记录)
