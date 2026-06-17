-- =============================================
-- Book Order System - H2 初始化脚本
-- 用于 dev 环境（H2 内存数据库）
-- =============================================

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
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- 权限表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    type TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- 用户-角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
);

-- ----------------------------
-- 角色-权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE (role_id, permission_id)
);

-- =============================================
-- 初始化数据（MERGE INTO 兼容 H2）
-- =============================================

-- 角色
MERGE INTO sys_role (id, role_code, role_name, description) KEY(id) VALUES
(1, 'ADMIN', '管理员', '系统管理员，拥有所有权限'),
(2, 'LIBRARIAN', '图书管理员', '图书管理员，管理图书和订单'),
(3, 'READER', '读者', '普通读者，可浏览和借阅图书');

-- 权限
MERGE INTO sys_permission (id, permission_code, permission_name, type) KEY(id) VALUES
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
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 7), (1, 8), (1, 9), (1, 10), (1, 11),
(1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17);

-- LIBRARIAN 拥有图书和订单管理权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(2, 7), (2, 8), (2, 9), (2, 10), (2, 11),
(2, 12), (2, 13), (2, 14), (2, 15), (2, 16);

-- READER 只有查看权限和创建订单权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(3, 8), (3, 13), (3, 14);

-- 默认管理员账号 (密码: admin123, BCrypt加密)
MERGE INTO sys_user (id, username, password, nickname, status) KEY(id) VALUES
(1, 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Tz4dOFOvFhgpoSLimuFhq', '系统管理员', 1);

-- 绑定 admin 用户到 ADMIN 角色
MERGE INTO sys_user_role (user_id, role_id) KEY(user_id, role_id) VALUES (1, 1);

-- ----------------------------
-- 图书分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS book_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- 图书表
-- ----------------------------
CREATE TABLE IF NOT EXISTS book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    isbn VARCHAR(20),
    publisher VARCHAR(100),
    price DECIMAL(10,2),
    stock INT NOT NULL DEFAULT 0,
    available_copies INT NOT NULL DEFAULT 0,
    category_id BIGINT,
    cover_url VARCHAR(500),
    description TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 分类权限
MERGE INTO sys_permission (id, permission_code, permission_name, type) KEY(id) VALUES
(18, 'category:list', '查看分类列表', 3),
(19, 'category:create', '创建分类', 2),
(20, 'category:update', '修改分类', 2),
(21, 'category:delete', '删除分类', 2);

-- ADMIN 补充分类权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(1, 18), (1, 19), (1, 20), (1, 21);

-- LIBRARIAN 补充分类权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(2, 18), (2, 19), (2, 20), (2, 21);

-- READER 只有查看分类权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(3, 18);

-- 示例分类
MERGE INTO book_category (id, name, description, status) KEY(id) VALUES
(1, '文学小说', '中外文学、小说、散文等', 1),
(2, '科技', '计算机、工程、自然科学等', 1),
(3, '历史人文', '历史、哲学、社会学等', 1);

-- 示例图书
MERGE INTO book (id, title, author, isbn, publisher, price, stock, available_copies, category_id, status) KEY(id) VALUES
(1, '百年孤独', '加西亚·马尔克斯', '9787544253468', '南海出版公司', 39.50, 50, 50, 1, 1),
(2, '深入理解计算机系统', 'Randal E.Bryant', '9787111544937', '机械工业出版社', 139.00, 20, 20, 2, 1),
(3, '人类简史', '尤瓦尔·赫拉利', '9787508647357', '中信出版社', 68.00, 30, 30, 3, 1);

-- ----------------------------
-- 借阅记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS borrow_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_time DATETIME NOT NULL,
    due_time DATETIME NOT NULL,
    return_time DATETIME,
    renew_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- 操作日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    target VARCHAR(500),
    detail VARCHAR(1000),
    ip VARCHAR(50),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 借阅 + 日志权限
MERGE INTO sys_permission (id, permission_code, permission_name, type) KEY(id) VALUES
(22, 'borrow:manage', '借阅管理', 1),
(23, 'borrow:create', '借书', 2),
(24, 'borrow:return', '还书', 2),
(25, 'borrow:renew', '续借', 2),
(26, 'borrow:my', '我的借阅', 3),
(27, 'log:list', '查看操作日志', 3);

-- ADMIN 补充所有新权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27);

-- LIBRARIAN 借阅管理权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(2, 22), (2, 23), (2, 24), (2, 25), (2, 26);

-- READER 借阅权限
MERGE INTO sys_role_permission (role_id, permission_id) KEY(role_id, permission_id) VALUES
(3, 23), (3, 24), (3, 25), (3, 26);
