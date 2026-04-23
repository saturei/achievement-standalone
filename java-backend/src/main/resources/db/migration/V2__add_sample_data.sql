-- =====================================================
-- 成果管理模块示例数据
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
