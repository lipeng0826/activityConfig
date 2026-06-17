# Book Order System - 图书订单管理系统

基于 Spring Boot 3 + Java 17 + Maven 构建，集成 MyBatis-Plus、Spring Security、JWT，实现 RBAC 权限管理。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.2.5 |
| Java | 17 |
| MyBatis-Plus | 3.5.6 |
| Spring Security | 6.x |
| JWT (jjwt) | 0.12.5 |
| MySQL | 8.0+ |

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 2. 建库

```sql
CREATE DATABASE IF NOT EXISTS book_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 修改数据库配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/book_order?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: root
```

### 4. 启动项目

```bash
mvn spring-boot:run
```

启动成功后访问 `http://localhost:8080`。

> 首次启动时，Spring Boot 会自动执行 `resources/sql/init.sql` 初始化表结构和默认数据。

## 默认账号

| 用户名 | 密码 | 角色 | 权限 |
|--------|------|------|------|
| admin | admin123 | ADMIN | 所有权限 |

> 注册接口会自动为新用户分配 READER 角色。

## 角色说明

| 角色 | 编码 | 权限 |
|------|------|------|
| 管理员 | ADMIN | 系统管理、用户管理、图书管理、订单管理、角色管理 |
| 图书管理员 | LIBRARIAN | 图书管理、订单管理 |
| 读者 | READER | 查看图书、查看订单、创建订单 |

## API 接口

### 登录

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### 注册

```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "reader1",
  "password": "123456",
  "nickname": "读者1"
}
```

### 获取当前用户信息

```bash
GET /api/auth/me
Authorization: Bearer <token>
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "email": null,
    "phone": null,
    "roles": ["ADMIN"],
    "permissions": ["system:manage", "user:manage", "user:list", "..."]
  }
}
```

## 项目结构

```
src/main/java/com/bookorder/
├── BookOrderApplication.java     # 启动类
├── common/
│   ├── Result.java               # 统一响应
│   ├── BusinessException.java    # 业务异常
│   └── GlobalExceptionHandler.java # 全局异常处理
├── config/
│   ├── SecurityConfig.java       # Spring Security 配置
│   └── MyBatisPlusConfig.java    # MyBatis-Plus 配置
├── controller/
│   └── AuthController.java       # 认证接口 (login/register/me)
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   └── UserInfoVO.java
├── entity/
│   ├── SysUser.java
│   ├── SysRole.java
│   ├── SysPermission.java
│   ├── SysUserRole.java
│   └── SysRolePermission.java
├── mapper/
│   ├── SysUserMapper.java
│   ├── SysRoleMapper.java
│   ├── SysPermissionMapper.java
│   ├── SysUserRoleMapper.java
│   └── SysRolePermissionMapper.java
├── security/
│   ├── JwtUtil.java              # JWT 工具类
│   ├── JwtAuthenticationFilter.java # JWT 过滤器
│   ├── SysUserDetails.java       # 用户详情
│   └── UserDetailsServiceImpl.java # UserDetailsService 实现
└── service/
    ├── SysUserService.java
    └── impl/
        └── SysUserServiceImpl.java
```
