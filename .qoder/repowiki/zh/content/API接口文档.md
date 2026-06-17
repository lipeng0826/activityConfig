# API接口文档

<cite>
**本文引用的文件**
- [AuthController.java](file://src/main/java/com/bookorder/controller/AuthController.java)
- [LoginRequest.java](file://src/main/java/com/bookorder/dto/LoginRequest.java)
- [RegisterRequest.java](file://src/main/java/com/bookorder/dto/RegisterRequest.java)
- [UserInfoVO.java](file://src/main/java/com/bookorder/dto/UserInfoVO.java)
- [Result.java](file://src/main/java/com/bookorder/common/Result.java)
- [BusinessException.java](file://src/main/java/com/bookorder/common/BusinessException.java)
- [GlobalExceptionHandler.java](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java)
- [JwtUtil.java](file://src/main/java/com/bookorder/security/JwtUtil.java)
- [JwtAuthenticationFilter.java](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java)
- [SecurityConfig.java](file://src/main/java/com/bookorder/config/SecurityConfig.java)
- [SysUserService.java](file://src/main/java/com/bookorder/service/SysUserService.java)
- [SysUserServiceImpl.java](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java)
- [SysUserMapper.java](file://src/main/java/com/bookorder/mapper/SysUserMapper.java)
- [SysUserDetails.java](file://src/main/java/com/bookorder/security/SysUserDetails.java)
- [application.yml](file://src/main/resources/application.yml)
- [README.md](file://README.md)
</cite>

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细接口规范](#详细接口规范)
6. [依赖关系分析](#依赖关系分析)
7. [性能与安全考虑](#性能与安全考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件为图书订单系统中认证相关接口的完整API文档，覆盖登录、注册、获取当前用户信息三个接口。文档详细说明了每个接口的HTTP方法、URL路径、请求参数、响应格式、认证机制（Bearer Token）、错误码含义与处理方式、以及API测试与调试建议。同时给出接口版本管理与向后兼容性建议。

## 项目结构
认证模块主要由以下层次组成：
- 控制层：AuthController 提供对外认证接口
- DTO 层：LoginRequest、RegisterRequest、UserInfoVO 定义请求与响应载体
- 服务层：SysUserService 及其实现 SysUserServiceImpl 负责业务逻辑
- 持久层：SysUserMapper 提供用户角色与权限查询
- 安全层：SecurityConfig、JwtAuthenticationFilter、JwtUtil、SysUserDetails 实现基于JWT的无状态认证与授权
- 统一响应与异常：Result、GlobalExceptionHandler 提供统一响应格式与异常处理

```mermaid
graph TB
subgraph "控制层"
AC["AuthController"]
end
subgraph "DTO层"
LR["LoginRequest"]
RR["RegisterRequest"]
UV["UserInfoVO"]
end
subgraph "服务层"
SU["SysUserService"]
SI["SysUserServiceImpl"]
end
subgraph "持久层"
UM["SysUserMapper"]
end
subgraph "安全层"
SC["SecurityConfig"]
JF["JwtAuthenticationFilter"]
JU["JwtUtil"]
SD["SysUserDetails"]
end
subgraph "通用"
RS["Result"]
GE["GlobalExceptionHandler"]
end
AC --> SU
SU --> SI
SI --> UM
SI --> JU
SC --> JF
JF --> JU
JF --> SD
AC --> RS
GE --> RS
```

图表来源
- [AuthController.java:18-58](file://src/main/java/com/bookorder/controller/AuthController.java#L18-L58)
- [SysUserService.java:6-15](file://src/main/java/com/bookorder/service/SysUserService.java#L6-L15)
- [SysUserServiceImpl.java:23-86](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java#L23-L86)
- [SysUserMapper.java:12-24](file://src/main/java/com/bookorder/mapper/SysUserMapper.java#L12-L24)
- [SecurityConfig.java:26-62](file://src/main/java/com/bookorder/config/SecurityConfig.java#L26-L62)
- [JwtAuthenticationFilter.java:20-55](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L20-L55)
- [JwtUtil.java:14-61](file://src/main/java/com/bookorder/security/JwtUtil.java#L14-L61)
- [SysUserDetails.java:10-53](file://src/main/java/com/bookorder/security/SysUserDetails.java#L10-L53)
- [Result.java:3-40](file://src/main/java/com/bookorder/common/Result.java#L3-L40)
- [GlobalExceptionHandler.java:17-61](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L17-L61)

章节来源
- [AuthController.java:18-58](file://src/main/java/com/bookorder/controller/AuthController.java#L18-L58)
- [README.md:128-167](file://README.md#L128-L167)

## 核心组件
- 统一响应体 Result：所有接口返回统一结构，包含状态码、消息与数据体
- 全局异常处理器 GlobalExceptionHandler：将业务异常、参数校验异常、认证/授权异常映射为统一响应
- JWT 工具类 JwtUtil：生成、解析、校验Token并提取用户信息
- 安全过滤器 JwtAuthenticationFilter：从请求头解析Bearer Token，构建认证上下文
- 安全配置 SecurityConfig：定义放行路径、认证入口与拒绝处理、添加JWT过滤器
- 用户详情 SysUserDetails：封装用户身份、权限与启用状态
- 用户服务 SysUserService/SysUserServiceImpl：登录认证、注册、获取用户信息与默认角色绑定
- 用户Mapper SysUserMapper：按用户ID查询角色编码与权限编码

章节来源
- [Result.java:3-40](file://src/main/java/com/bookorder/common/Result.java#L3-L40)
- [GlobalExceptionHandler.java:17-61](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L17-L61)
- [JwtUtil.java:14-61](file://src/main/java/com/bookorder/security/JwtUtil.java#L14-L61)
- [JwtAuthenticationFilter.java:20-55](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L20-L55)
- [SecurityConfig.java:26-62](file://src/main/java/com/bookorder/config/SecurityConfig.java#L26-L62)
- [SysUserDetails.java:10-53](file://src/main/java/com/bookorder/security/SysUserDetails.java#L10-L53)
- [SysUserService.java:6-15](file://src/main/java/com/bookorder/service/SysUserService.java#L6-L15)
- [SysUserServiceImpl.java:23-86](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java#L23-L86)
- [SysUserMapper.java:12-24](file://src/main/java/com/bookorder/mapper/SysUserMapper.java#L12-L24)

## 架构总览
认证流程概览如下：

```mermaid
sequenceDiagram
participant C as "客户端"
participant F as "JwtAuthenticationFilter"
participant S as "SecurityConfig"
participant U as "AuthController"
participant SVC as "SysUserServiceImpl"
participant M as "SysUserMapper"
participant T as "JwtUtil"
rect rgb(255,255,255)
Note over C,S : "登录/注册"
C->>U : "POST /api/auth/login 或 /api/auth/register"
U->>SVC : "调用业务方法"
SVC->>T : "生成JWT"
T-->>SVC : "返回token"
SVC-->>U : "返回token"
U-->>C : "Result{code,message,data}"
end
rect rgb(255,255,255)
Note over C,F : "携带Token访问受保护资源"
C->>F : "GET /api/auth/me<br/>Authorization : Bearer <token>"
F->>T : "校验并解析token"
T-->>F : "claims(含userId/username)"
F->>S : "加载UserDetails并设置认证上下文"
S-->>F : "通过"
F-->>C : "放行到控制器"
C->>U : "GET /api/auth/me"
U->>M : "查询角色与权限"
M-->>U : "角色/权限列表"
U-->>C : "Result{code,message,data}"
end
```

图表来源
- [AuthController.java:28-57](file://src/main/java/com/bookorder/controller/AuthController.java#L28-L57)
- [SysUserServiceImpl.java:50-55](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java#L50-L55)
- [JwtAuthenticationFilter.java:28-46](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L28-L46)
- [JwtUtil.java:27-43](file://src/main/java/com/bookorder/security/JwtUtil.java#L27-L43)
- [SysUserMapper.java:14-23](file://src/main/java/com/bookorder/mapper/SysUserMapper.java#L14-L23)
- [SecurityConfig.java:35-59](file://src/main/java/com/bookorder/config/SecurityConfig.java#L35-L59)

## 详细接口规范

### 1) 登录接口
- 请求方法：POST
- 请求路径：/api/auth/login
- 功能描述：用户凭用户名与密码进行认证，成功后返回JWT Token
- 认证要求：无需Token
- Content-Type：application/json

请求体字段
- username：字符串，必填，用户名
- password：字符串，必填，密码

请求示例
- POST /api/auth/login
- Content-Type: application/json
- 请求体示例参考：[LoginRequest.java:5-17](file://src/main/java/com/bookorder/dto/LoginRequest.java#L5-L17)

响应体结构
- code：整数，状态码
- message：字符串，提示信息
- data：字符串，JWT Token

成功响应示例
- code：200
- message："success"
- data：JWT字符串

错误响应示例
- 401 未授权：用户名或密码错误
- 400 参数错误：请求体校验失败（如字段为空）
- 500 系统错误：服务器内部异常

章节来源
- [AuthController.java:28-32](file://src/main/java/com/bookorder/controller/AuthController.java#L28-L32)
- [SysUserServiceImpl.java:50-55](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java#L50-L55)
- [GlobalExceptionHandler.java:28-32](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L28-L32)
- [GlobalExceptionHandler.java:40-47](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L40-L47)
- [SecurityConfig.java:39-42](file://src/main/java/com/bookorder/config/SecurityConfig.java#L39-L42)

### 2) 注册接口
- 请求方法：POST
- 请求路径：/api/auth/register
- 功能描述：创建新用户，默认赋予 READER 角色
- 认证要求：无需Token
- Content-Type：application/json

请求体字段
- username：字符串，必填，长度3-50；用户名唯一
- password：字符串，必填，长度6-50
- nickname：字符串，可选，用户昵称

请求示例
- POST /api/auth/register
- Content-Type: application/json
- 请求体示例参考：[RegisterRequest.java:6-24](file://src/main/java/com/bookorder/dto/RegisterRequest.java#L6-L24)

响应体结构
- code：整数，状态码
- message：字符串，提示信息
- data：空值（注册成功无额外数据）

成功响应示例
- code：200
- message："success"
- data：null

错误响应示例
- 400 参数错误：用户名已存在、字段长度不合法、字段为空
- 500 系统错误：服务器内部异常

章节来源
- [AuthController.java:34-38](file://src/main/java/com/bookorder/controller/AuthController.java#L34-L38)
- [SysUserServiceImpl.java:58-80](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java#L58-L80)
- [GlobalExceptionHandler.java:22-26](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L22-L26)
- [GlobalExceptionHandler.java:40-47](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L40-L47)

### 3) 获取当前用户信息接口
- 请求方法：GET
- 请求路径：/api/auth/me
- 功能描述：返回当前登录用户的详细信息，包含角色与权限列表
- 认证要求：需要携带有效Bearer Token
- Content-Type：application/json
- Authorization：Bearer <token>

请求示例
- GET /api/auth/me
- Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

响应体结构
- code：整数，状态码
- message：字符串，提示信息
- data：对象，包含用户基本信息与权限信息
  - id：长整型，用户ID
  - username：字符串，用户名
  - nickname：字符串，昵称
  - email：字符串，邮箱
  - phone：字符串，电话
  - roles：字符串数组，角色编码列表
  - permissions：字符串数组，权限编码列表

成功响应示例
- code：200
- message："success"
- data：包含上述字段的对象

错误响应示例
- 401 未登录或token已过期：未提供或无效Token
- 403 权限不足：访问被拒绝
- 500 系统错误：服务器内部异常

章节来源
- [AuthController.java:40-57](file://src/main/java/com/bookorder/controller/AuthController.java#L40-L57)
- [SysUserMapper.java:14-23](file://src/main/java/com/bookorder/mapper/SysUserMapper.java#L14-L23)
- [UserInfoVO.java:5-29](file://src/main/java/com/bookorder/dto/UserInfoVO.java#L5-L29)
- [SecurityConfig.java:44-57](file://src/main/java/com/bookorder/config/SecurityConfig.java#L44-L57)
- [GlobalExceptionHandler.java:34-38](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L34-L38)

## 依赖关系分析

```mermaid
classDiagram
class AuthController {
+login(LoginRequest) Result~String~
+register(RegisterRequest) Result~Void~
+me(SysUserDetails) Result~UserInfoVO~
}
class SysUserService {
<<interface>>
+login(username,password) String
+register(username,password,nickname) void
+getUserInfo(userId) SysUser
}
class SysUserServiceImpl {
+login(username,password) String
+register(username,password,nickname) void
+getUserInfo(userId) SysUser
}
class SysUserMapper {
+selectRoleCodesByUserId(userId) String[]
+selectPermissionCodesByUserId(userId) String[]
}
class JwtUtil {
+generateToken(userId,username) String
+validateToken(token) boolean
+parseToken(token) Claims
}
class JwtAuthenticationFilter {
+doFilterInternal(req,res,chain) void
}
class SecurityConfig {
+securityFilterChain(http) SecurityFilterChain
}
class Result {
+success() Result
+success(data) Result
+error(code,message) Result
}
class GlobalExceptionHandler {
+handleBusinessException(e) Result
+handleValidationException(e) Result
+handleBadCredentialsException(e) Result
+handleAccessDeniedException(e) Result
}
AuthController --> SysUserService : "依赖"
SysUserServiceImpl ..|> SysUserService : "实现"
SysUserServiceImpl --> SysUserMapper : "使用"
SysUserServiceImpl --> JwtUtil : "使用"
SecurityConfig --> JwtAuthenticationFilter : "注册"
JwtAuthenticationFilter --> JwtUtil : "使用"
AuthController --> Result : "返回"
GlobalExceptionHandler --> Result : "构造"
```

图表来源
- [AuthController.java:18-58](file://src/main/java/com/bookorder/controller/AuthController.java#L18-L58)
- [SysUserService.java:6-15](file://src/main/java/com/bookorder/service/SysUserService.java#L6-L15)
- [SysUserServiceImpl.java:23-86](file://src/main/java/com/bookorder/service/impl/SysUserServiceImpl.java#L23-L86)
- [SysUserMapper.java:12-24](file://src/main/java/com/bookorder/mapper/SysUserMapper.java#L12-L24)
- [JwtUtil.java:14-61](file://src/main/java/com/bookorder/security/JwtUtil.java#L14-L61)
- [JwtAuthenticationFilter.java:20-55](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L20-L55)
- [SecurityConfig.java:26-62](file://src/main/java/com/bookorder/config/SecurityConfig.java#L26-L62)
- [Result.java:3-40](file://src/main/java/com/bookorder/common/Result.java#L3-L40)
- [GlobalExceptionHandler.java:17-61](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L17-L61)

## 性能与安全考虑
- Token有效期：默认一天（可通过配置项调整），建议在移动端场景下结合刷新策略
- 无状态认证：服务端不保存会话，降低扩展复杂度
- 参数校验：使用注解驱动的校验，减少重复校验逻辑
- 异常统一处理：避免泄露敏感信息，仅返回必要错误码与消息
- 数据脱敏：返回的用户信息不含敏感字段（如密码），注意不要在日志中打印Token

[本节为通用建议，不直接分析具体文件]

## 故障排查指南
常见问题与处理
- 400 参数错误
  - 现象：请求体字段缺失或不符合约束
  - 处理：检查请求体字段是否符合DTO约束（长度、非空等）
  - 参考：[GlobalExceptionHandler.java:40-47](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L40-L47)
- 401 未登录或token已过期
  - 现象：访问受保护接口未携带或携带无效Token
  - 处理：确保Authorization头以“Bearer ”前缀携带有效Token；检查Token是否过期
  - 参考：[SecurityConfig.java:44-49](file://src/main/java/com/bookorder/config/SecurityConfig.java#L44-L49)，[JwtAuthenticationFilter.java:48-54](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L48-L54)
- 403 权限不足
  - 现象：认证通过但无访问权限
  - 处理：确认用户角色与权限是否满足接口要求
  - 参考：[SecurityConfig.java:51-57](file://src/main/java/com/bookorder/config/SecurityConfig.java#L51-L57)
- 业务异常（如用户名已存在）
  - 现象：抛出 BusinessException，返回自定义错误码
  - 处理：根据错误码提示用户或前端进行相应处理
  - 参考：[BusinessException.java:3-18](file://src/main/java/com/bookorder/common/BusinessException.java#L3-L18)，[GlobalExceptionHandler.java:22-26](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L22-L26)

章节来源
- [GlobalExceptionHandler.java:22-26](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L22-L26)
- [GlobalExceptionHandler.java:28-32](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L28-L32)
- [GlobalExceptionHandler.java:34-38](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L34-L38)
- [GlobalExceptionHandler.java:40-47](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L40-L47)
- [SecurityConfig.java:44-57](file://src/main/java/com/bookorder/config/SecurityConfig.java#L44-L57)
- [JwtAuthenticationFilter.java:48-54](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L48-L54)

## 结论
本API文档覆盖了认证模块的核心接口，明确了请求与响应格式、认证机制、错误码与处理方式，并提供了测试与排错建议。系统采用JWT无状态认证与Spring Security集成，具备良好的扩展性与安全性。建议在生产环境中进一步完善Token刷新策略、接口限流与审计日志。

[本节为总结性内容，不直接分析具体文件]

## 附录

### A. 统一响应格式
- 成功响应：code=200，message="success"，data为具体数据
- 错误响应：code为错误码，message为错误描述，data通常为null

章节来源
- [Result.java:18-35](file://src/main/java/com/bookorder/common/Result.java#L18-L35)

### B. 错误码说明
- 200：成功
- 400：参数错误/请求体校验失败
- 401：未登录或Token无效
- 403：权限不足
- 500：系统内部错误

章节来源
- [GlobalExceptionHandler.java:22-60](file://src/main/java/com/bookorder/common/GlobalExceptionHandler.java#L22-L60)

### C. 认证机制与权限要求
- Bearer Token：请求头Authorization: Bearer <token>
- 放行路径：/api/auth/login、/api/auth/register
- 其他接口均需认证

章节来源
- [SecurityConfig.java:39-42](file://src/main/java/com/bookorder/config/SecurityConfig.java#L39-L42)
- [JwtAuthenticationFilter.java:48-54](file://src/main/java/com/bookorder/security/JwtAuthenticationFilter.java#L48-L54)

### D. API测试与调试建议
- 使用工具：curl、Postman、Insomnia
- 测试步骤：
  1) 调用登录接口获取Token
  2) 在后续请求头中添加Authorization: Bearer <token>
  3) 对于注册接口，先检查用户名是否已存在
- 调试要点：
  - 关注401/403错误，确认Token有效性与权限
  - 关注400错误，检查请求体字段与长度
  - 开启日志观察异常堆栈

章节来源
- [README.md:68-126](file://README.md#L68-L126)

### E. 接口版本管理与向后兼容
- 建议方案：
  - URL版本：/api/v1/auth/login
  - Header版本：Accept: application/vnd.company.v1+json
  - 查询参数版本：/api/auth/login?v=1
- 向后兼容：
  - 新增字段保持默认值，避免破坏现有客户端
  - 废弃字段保留但标记为废弃，提供迁移指引
  - 错误码与消息保持稳定，避免客户端硬编码

[本节为通用建议，不直接分析具体文件]