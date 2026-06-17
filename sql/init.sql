-- =============================================
-- Book Order System - Database Initialization
-- =============================================

CREATE DATABASE IF NOT EXISTS book_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE book_order;

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 权限表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    type TINYINT NOT NULL DEFAULT 1 COMMENT '1-菜单 2-按钮 3-接口',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 用户-角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 角色-权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 初始化数据
-- =============================================

-- 角色
INSERT IGNORE INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '管理员', '系统管理员，拥有所有权限'),
(2, 'LIBRARIAN', '图书管理员', '图书管理员，管理图书和订单'),
(3, 'READER', '读者', '普通读者，可浏览和借阅图书');

-- 权限
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, type) VALUES
(1, 'system:manage', '系统管理', 1),
(2, 'user:manage', '用户管理', 1),
(3, 'user:list', '查看用户列表', 3),
(4, 'user:create', '创建用户', 2),
(5, 'user:update', '修改用户', 2),
(6, 'user:delete', '删除用户', 2),
(7, 'book:manage', '图书管理', 1),
(8, 'book:list', '查看图书列表', 3),
(9, 'book:create', '创建图书', 2),
(10, 'book:update', '修改图书', 2),
(11, 'book:delete', '删除图书', 2),
(12, 'order:manage', '订单管理', 1),
(13, 'order:list', '查看订单列表', 3),
(14, 'order:create', '创建订单', 2),
(15, 'order:update', '修改订单', 2),
(16, 'order:delete', '删除订单', 2),
(17, 'role:manage', '角色管理', 1);

-- ADMIN 拥有所有权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 7), (1, 8), (1, 9), (1, 10), (1, 11),
(1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17);

-- LIBRARIAN 拥有图书和订单管理权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(2, 7), (2, 8), (2, 9), (2, 10), (2, 11),
(2, 12), (2, 13), (2, 14), (2, 15), (2, 16);

-- READER 只有查看权限和创建订单权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(3, 8), (3, 13), (3, 14);

-- 默认管理员账号 (密码: admin123)
-- BCrypt 加密后的密码
INSERT IGNORE INTO sys_user (id, username, password, nickname, status) VALUES
(1, 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Tz4dOFOvFhgpoSLimuFhq', '系统管理员', 1);

-- 绑定 admin 用户到 ADMIN 角色
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);
