# 领取型活动后端系统 · 自学动手路线图

## Context

工作区 `/Users/lipeng/Desktop/myProject/activityConfig` 当前为空目录。本次目标不是一次性帮你把代码写完后你直接跑，而是输出一份**可边做边学**的练习地图。你按步骤自己写，我在你卡住或需要 review 时介入。

核心方向：
- 业务：**领取型活动**（券/红包/奖励）。
- 模块：**活动配置后台** + **用户查看/领取模块**。
- 技术栈：**Java + Spring Boot + MyBatis-Plus + Redis + MySQL**。
- 本地用 Docker 起 MySQL/Redis，生产再迁阿里云。
- 重点练：**库存扣减、幂等、并发控制、规则配置可扩展**。

---

## 学习原则

1. **先跑通，再优化**：第一版用最简单的方式实现，确认端到端可运行后再加缓存、锁、限流。
2. **每一步都要有验证**：不能只写代码不测试，每个 Task 后面跟着“验证方式”。
3. **记录踩坑笔记**：建议开一个 `notes.md`，把遇到的报错、解决方案、面试点记下来。
4. **不懂先查再问我**：遇到报错先 Google/AI 查 10 分钟，把错误信息、已尝试方案整理清楚再来问我，这样提升最快。

---

## 第一阶段：把骨架搭起来（不需要复杂设计）

### Task 1：初始化一个能跑的 Spring Boot 项目

**你要做什么：**
- 用 IDEA 的 Spring Initializr 新建一个 Maven 项目，放到 `/Users/lipeng/Desktop/myProject/activityConfig`。
- 依赖只选：Spring Web、Spring Data JDBC（或先不选 ORM，手动写 SQL 也行）、Lombok。
- JDK 17，Spring Boot 3.2.x。
- 写一个 `HelloController`，跑起来后浏览器访问 `http://localhost:8080/hello` 能返回字符串。

**不要做什么：**
- 不要一上来就分多模块、不要加 Redis、不要加 MyBatis-Plus。

**验证方式：**
- `mvn spring-boot:run` 或 IDEA 运行，`curl http://localhost:8080/hello` 通了就下一步。

**面试点：**
- Spring Boot 自动配置原理、`@SpringBootApplication` 里有什么。

---

### Task 2：本地 Docker 起 MySQL + Redis

**你要做什么：**
- 在项目根目录写 `docker-compose.yml`，启动 MySQL 8.0 和 Redis 7.0。
- MySQL 建库 `activity_db`。
- 写 `application.yml` 或 `application-dev.yml`，配置数据源和 Redis。
- 写一个测试接口 `/db-check`，从数据库查 `SELECT 1` 返回，证明连上了。

**参考配置：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/activity_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
```

**验证方式：**
- Docker 容器启动成功，`/db-check` 接口返回 OK。

**常见坑：**
- MySQL 8.0 驱动类是 `com.mysql.cj.jdbc.Driver`。
- 时区不对导致时间字段差 8 小时。

---

### Task 3：引入 MyBatis-Plus 并写第一个 CRUD

**你要做什么：**
- 在 `pom.xml` 加入 MyBatis-Plus 依赖。
- 配置 `MybatisPlusConfig`（主要是分页插件）。
- 创建第一张表 `activity`（字段参考下文“最简表结构”），写对应的 Entity、Mapper、Service、Controller。
- 实现 `POST /activity` 创建活动、`GET /activity/{id}` 查询活动。

**最简表结构（第一阶段先用这个，后面再加）：**
```sql
CREATE TABLE `activity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_code` varchar(64) NOT NULL COMMENT '活动编码',
  `activity_name` varchar(128) NOT NULL COMMENT '活动名称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿 1上架 2下架',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `stock` int unsigned NOT NULL DEFAULT '0' COMMENT '库存',
  `taken_stock` int unsigned NOT NULL DEFAULT '0' COMMENT '已领取',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`activity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**验证方式：**
- Postman 创建活动，再查询，数据正确落库。

**面试点：**
- MyBatis-Plus 的 BaseMapper 提供了哪些方法？`QueryWrapper` 和 `LambdaQueryWrapper` 区别。

---

## 第二阶段：把领取流程跑通（先不考虑并发）

### Task 4：实现无并发的领取接口

**你要做什么：**
- 新建 `user_take_record` 表：
```sql
CREATE TABLE `user_take_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `activity_id` bigint unsigned NOT NULL,
  `take_status` tinyint NOT NULL DEFAULT '2' COMMENT '2成功 3失败',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
- 实现 `POST /api/activity/take`，参数：`userId`、`activityId`。
- 业务逻辑：
  1. 查活动是否存在、是否上架、时间是否在有效期内。
  2. 查库存是否足够（`stock > taken_stock`）。
  3. `taken_stock + 1`。
  4. 插入领取记录。
- 返回领取成功。

**验证方式：**
- 创建一个库存为 10 的活动，用不同 `userId` 领取 10 次，数据库 `taken_stock` 变为 10，记录表有 10 条。

**面试点：**
- 这里有没有 bug？（提示：并发会超卖）

---

### Task 5：给领取接口加幂等

**你要做什么：**
- 在请求里加一个 `requestId`（String，前端生成 UUID）。
- 在 `user_take_record` 表加唯一索引 `UNIQUE KEY uk_request (request_id)`。
- 领取前先查：如果 `requestId` 已存在，直接返回之前的领取结果。
- 不存在才执行业务。

**验证方式：**
- 同一个 `requestId` 发 10 次，只产生一条记录，返回都相同。

**常见坑：**
- 先查后插并发时可能两个线程都判断为“不存在”，然后都插入。唯一索引兜底可以防住，但要处理好 `DuplicateKeyException`。

**面试点：**
- 幂等为什么要前端传 requestId？后端自己生成不行吗？

---

### Task 6：限制用户只能领一次

**你要做什么：**
- 在 `user_take_record` 表加唯一索引 `UNIQUE KEY uk_user_activity (user_id, activity_id)`。
- 领取前查记录：如果用户已领过，返回“已领取”。
- 注意和 Task 5 的 `requestId` 幂等结合：同一用户点两次，第一次成功，第二次返回“已领取”。

**验证方式：**
- 同一 `userId` 同一个 `activityId` 领两次，第二次返回已领取。

---

## 第三阶段：处理并发（这是活动系统最核心的考点）

### Task 7：用数据库乐观锁解决超卖

**你要做什么：**
- 在 `activity` 表加字段 `version` int default 0。
- 修改领取逻辑，用 MyBatis-Plus 的乐观锁：
  ```java
  update activity set taken_stock = taken_stock + 1, version = version + 1
  where id = ? and version = ? and stock > taken_stock
  ```
- 更新失败表示并发冲突，返回“系统繁忙”或重试。

**验证方式：**
- 用 JMeter 或自己写多线程测试：库存 100，1000 个线程同时领，最终 `taken_stock` 不超过 100。

**面试点：**
- 乐观锁和悲观锁的区别？乐观锁适合什么场景？

---

### Task 8：用 Redis + Lua 优化库存扣减性能

**你要做什么：**
- 活动发布时把库存预热到 Redis：`activity:stock:{activityId}`。
- 领取时用 Lua 脚本原子扣减：
  ```lua
  local stock = redis.call('get', KEYS[1])
  if tonumber(stock) > 0 then
      redis.call('decrby', KEYS[1], 1)
      return 1
  else
      return 0
  end
  ```
- Redis 扣减成功后，再写数据库领取记录，并异步/定时同步数据库库存。

**验证方式：**
- 压测领取接口，QPS 应该比纯数据库乐观锁高很多。
- 最终数据库 `taken_stock` 和 Redis 扣减量一致。

**面试点：**
- 为什么用 Lua 脚本？`WATCH/MULTI/EXEC` 行不行？
- Redis 扣减成功但写库失败怎么办？

---

### Task 9：用分布式锁解决同一用户并发重复领取

**你要做什么：**
- 引入 Redisson。
- 领取流程里加锁：`lock:take:{userId}:{activityId}`。
- 获取锁失败返回“操作太频繁”。

**验证方式：**
- 同一用户同一活动并发 50 次请求，只产生一条领取记录。

**常见坑：**
- 锁没释放（异常导致），用 `try-finally` 或 Redisson 的看门狗。
- 锁粒度过大导致性能差。

---

## 第四阶段：把配置做灵活（体现可扩展性）

### Task 10：拆分活动规则表

**你要做什么：**
- 新建 `activity_rule` 表，规则用 JSON 存：
```sql
CREATE TABLE `activity_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint unsigned NOT NULL,
  `rule_type` varchar(32) NOT NULL COMMENT 'TIME/USER_LIMIT',
  `rule_key` varchar(64) NOT NULL,
  `rule_value` text COMMENT 'JSON',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
- 创建活动时同时插入规则。
- 领取时读取规则并校验：
  - 时间规则：是否在有效时间。
  - 用户限制规则：每人总共能领几次、每天能领几次。

**验证方式：**
- 配置“每人限领 1 次”，第二次领取被拦截。
- 配置活动时间外不可领取。

**面试点：**
- 如果以后加“白名单规则”，要怎么扩展？

---

### Task 11：抽象奖励模型

**你要做什么：**
- 新建 `reward` 表和 `reward_stock` 表（参考原方案）。
- 把 `activity` 表的 `stock` 字段迁移到 `reward_stock`。
- 一个活动可以配多个奖励，每个奖励有自己的库存。
- 领取时先选定一个奖励（目前简单按顺序或默认第一个），再扣库存。

**验证方式：**
- 一个活动配两个奖励，各 10 库存，分别领取后库存独立扣减。

---

### Task 12：用策略模式支持不同活动类型

**你要做什么：**
- 定义接口 `ActivityHandler`：
  ```java
  public interface ActivityHandler {
      Integer getActivityType();
      TakeResult take(Long userId, Long activityId, String requestId);
  }
  ```
- 实现 `TakeActivityHandler`（领取型）。
- 领取接口根据 `activity.activity_type` 路由到对应 Handler。

**验证方式：**
- 新增一个活动类型枚举值后，只需新增 Handler 即可扩展，不用改领取接口。

**面试点：**
- 策略模式和工厂模式在这里怎么用？

---

## 第五阶段：补全运营后台和 C 端能力

### Task 13：实现活动配置后台接口

**你要做什么：**
- `POST /admin/activity/create`：创建活动 + 规则 + 奖励 + 库存。
- `POST /admin/activity/publish/{id}`：发布活动，同时把配置和库存预热到 Redis。
- `POST /admin/activity/offline/{id}`：下架，删除 Redis 缓存。
- `GET /admin/activity/list`：分页列表。

**验证方式：**
- Postman 走一遍完整运营流程：创建 → 发布 → C 端领取 → 下架 → C 端不可领取。

---

### Task 14：实现 C 端活动列表和详情

**你要做什么：**
- `GET /api/activity/list`：返回当前可参加活动列表。
- `GET /api/activity/detail/{id}`：返回活动详情、规则、剩余库存。
- `GET /api/activity/my-records?userId=xxx`：返回我的领取记录。
- 列表和详情优先读 Redis，未命中再读数据库。

**验证方式：**
- 发布后列表能立刻看到；下架后列表消失。

---

### Task 15：加限流和日志

**你要做什么：**
- 用 Sentinel 或自定义 Redis 计数器实现：
  - 单接口 QPS 限流。
  - 单用户领取频率限制（如 1 秒 1 次）。
- 用 `@OpLog` + AOP 记录操作日志（领取得记录谁、什么时间、什么活动）。

**验证方式：**
- 高频请求被拦截，返回“请求太频繁”。
- 操作日志表有记录。

---

## 第六阶段：压测、对账、复盘

### Task 16：写单元测试和压测

**你要做什么：**
- 用 JUnit 5 + Mockito 写 Service 层单元测试。
- 用 JMeter 或自己写多线程脚本压测领取接口，目标：
  - 1000 并发，库存 100，最终不超卖。
  - 同一用户并发，不重复领取。

**验证方式：**
- 压测后数据库 `taken_stock <= stock`，`user_take_record` 数量正确。

---

### Task 17：实现对账任务

**你要做什么：**
- 写一个定时任务（Spring Scheduler），每小时执行：
  - 比对 Redis 库存和数据库 `reward_stock.remain_stock`。
  - 比对 `user_take_record` 成功数和 `user_reward_account` 数量。
- 不一致时打印告警日志。

**验证方式：**
- 手动修改 Redis 库存，看定时任务能否检测出不一致。

---

### Task 18：整理面试问题和自己的理解

**你要整理的问题：**
1. 如何防止超卖？
2. 幂等怎么做？
3. 缓存一致性怎么保证？
4. Redis 挂了怎么办？
5. 如何支持后续扩展抽奖/任务？
6. 库存扣减失败怎么回滚？

**输出物：**
- 一份你自己的 `interview-notes.md`，用自己的话回答这 6 个问题。

---

## 我的角色

我**不会**直接替你写完代码。你每完成一个 Task 后：
- 自己先跑通、先查资料。
- 卡住了把错误信息、你尝试过的方案、相关代码片段发给我。
- 我可以帮你 review 代码、指出设计问题、解释原理、提供伪代码或关键代码片段。

如果你现在就开始 Task 1，遇到任何报错都可以直接丢给我。# 领取型活动后端系统实现方案

## Context

工作区 `/Users/lipeng/Desktop/myProject/activityConfig` 当前为空目录，只有 `.git`。用户希望从零开始搭建一套活动后端系统，核心方向已明确：

- 业务类型：**领取型活动**（券/红包/奖励），用户看到活动后点击领取。
- 核心模块：**活动配置后台**（运营配置活动、规则、奖励、库存）+ **用户查看/领取模块**（C 端列表、详情、领取、我的记录）。
- 技术栈：**Java + Spring Boot**。
- 部署：**本地无 MySQL/Redis，计划购买阿里云 RDS/Redis**；本地开发阶段可用 Docker 启动 MySQL + Redis。
- 设计重点：**灵活配置、可扩展**，后续可平滑支持抽奖型、任务型等活动。

本方案按照“需求文档 → 技术实现 → 具体细节 → 项目结构 → 实现步骤 → 风险建议”完整展开，目标是一份可逐步落地的文档级计划。

---

## 1. 需求文档

### 1.1 项目目标

为运营团队提供一站式的领取型活动配置能力，为 C 端用户提供稳定、流畅的活动查看与领取体验。系统需支持：

- 创建/编辑/上下架领取型活动（券、红包、奖励）。
- 灵活配置奖励、库存、领取规则、可见范围、时间窗口。
- 用户查看活动列表/详情、点击领取、查看领取记录。
- 为后续扩展抽奖型、任务型活动预留扩展点。

### 1.2 用户角色与用例

| 角色 | 用例 |
|------|------|
| 运营人员（Admin） | 登录后台、创建活动、配置规则、管理库存、上下架活动、查看数据 |
| C 端用户（User） | 浏览活动列表、查看活动详情、点击领取、查看我的领取记录 |
| 系统 | 库存扣减、幂等校验、防刷控制、发放奖励、记录流水、缓存同步 |

### 1.3 功能模块拆解

#### 1.3.1 活动配置模块（运营后台）

| 功能 | 说明 |
|------|------|
| 活动管理 | 创建、编辑、删除、上下架、复制活动 |
| 规则配置 | 领取时间、领取次数、用户可见范围、资格校验 |
| 奖励配置 | 奖励类型、面额、库存、发放方式 |
| 库存管理 | 总库存、已领取、剩余库存、库存变更日志 |
| 数据看板 | 领取人数、领取次数、发放量（后续迭代） |

#### 1.3.2 用户查看模块（C 端）

| 功能 | 说明 |
|------|------|
| 活动列表 | 按状态、可见范围、时间筛选展示 |
| 活动详情 | 展示活动信息、规则、领取按钮状态 |
| 领取接口 | 校验资格、扣库存、发放奖励、记录流水 |
| 我的记录 | 查询当前用户的领取记录 |

#### 1.3.3 公共能力

| 能力 | 说明 |
|------|------|
| 缓存管理 | 活动配置缓存、库存缓存、用户领取计数缓存 |
| 幂等控制 | 防止重复领取、重复提交 |
| 限流防刷 | 接口级、用户级、IP 级限流 |
| 分布式锁 | 领取扣库存、库存回补充斥 |
| 任务补偿 | 库存对账、发放失败补偿 |

### 1.4 核心业务规则

#### 1.4.1 活动时间规则

- 活动有 `start_time` 和 `end_time`，仅在该时间窗口内可领取。
- 未到开始时间：展示「即将开始」。
- 已结束：展示「已结束」。
- 运营可手动上下架，上架且时间在窗口内才允许领取。

#### 1.4.2 库存规则

- 每个奖励/活动维度维护总库存 `total_stock` 和已领取库存 `taken_stock`。
- 领取成功时原子扣减库存。
- 库存不足时返回「已领完」。
- 允许库存回滚（用户取消、发放失败等情况）。

#### 1.4.3 领取资格规则

- 用户已登录。
- 活动时间有效。
- 活动状态为上架。
- 用户不在黑名单。
- 用户领取次数未超过限制（每日/每周/总计）。
- 同一用户同一活动不可重复领取（根据业务可配置）。

#### 1.4.4 幂等规则

- 领取接口必须携带 `request_id`（前端生成的 UUID），服务端幂等校验。
- 同一 `request_id` 重复提交返回首次结果，不重复扣库存/发奖。

#### 1.4.5 防刷规则

- 接口级限流：单接口 QPS 上限。
- 用户级限流：单用户领取频率上限。
- IP 级限流：单 IP 请求频率上限。
- 异常行为监控：同一用户短时间内高频领取触发风控。

#### 1.4.6 发放规则

- 领取成功后在领取记录表中写入「发放中」。
- 调用奖励发放服务（券、红包、积分等）。
- 发放成功更新为「发放成功」，失败进入补偿队列。

### 1.5 非功能需求

| 维度 | 要求 |
|------|------|
| 性能 | 活动列表接口 P99 < 200ms；领取接口 P99 < 300ms |
| 可用性 | 核心业务链路具备降级能力（如缓存失效可降级查库） |
| 安全 | SQL 注入防护、参数校验、接口鉴权、防重放攻击 |
| 可扩展 | 活动类型、规则、奖励类型可插件化扩展 |
| 可运维 | 统一日志、监控、告警、链路追踪 |
| 数据一致性 | 库存扣减与领取记录强一致，分布式场景通过最终一致性保证 |

---

## 2. 技术实现方案

### 2.1 技术选型

| 层级 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 语言/运行时 | JDK | 17（LTS） | 长期支持版本，性能与语法兼顾 |
| 框架 | Spring Boot | 3.2.x | 主流版本，兼容 JDK 17 |
| 数据库 | MySQL | 8.0+ | 阿里云 RDS MySQL 8.0 |
| 缓存 | Redis | 7.0+ | 阿里云 Redis 企业版 |
| ORM | MyBatis-Plus | 3.5.5+ | 简化 CRUD，适合快速开发 |
| 连接池 | HikariCP | Spring Boot 内置 | 默认高性能连接池 |
| 序列化 | Jackson | Spring Boot 内置 | JSON 处理 |
| 工具库 | Hutool | 5.8.x | 工具类集合 |
| 限流 | Sentinel / Guava RateLimiter | 1.8.8 / 31.x | 初期可用 Sentinel 轻量接入 |
| 分布式锁 | Redisson | 3.25.x | 基于 Redis 的分布式锁 |
| 消息队列 | RocketMQ / 本地定时任务 | 5.x / 本地表 | 后续可接入 MQ，初期用定时任务做补偿 |
| 单元测试 | JUnit 5 + Mockito | Spring Boot 内置 | |
| 容器化 | Docker | 最新稳定版 | 本地开发及部署 |
| 文档 | SpringDoc OpenAPI | 2.3.x | 自动生成 API 文档 |

### 2.2 系统架构图

```text
┌─────────────────────────────────────────────────────────────┐
│                         客户端层                              │
│  （运营后台 Web / H5 / App）                                   │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTPS
┌───────────────────────▼─────────────────────────────────────┐
│                      网关/接入层                               │
│     Nginx / Spring Cloud Gateway（可选，后续接入）              │
│     限流、鉴权、负载均衡                                       │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      应用服务层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 活动配置服务  │  │ 用户领取服务  │  │ 公共能力服务  │       │
│  │ ActivityAdmin│  │ ActivityUser │  │ Common       │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   MySQL      │ │    Redis     │ │  RocketMQ    │
│  (RDS 8.0)   │ │  (阿里云 Redis) │ │   (可选)     │
└──────────────┘ └──────────────┘ └──────────────┘
```

### 2.3 部署架构

#### 生产环境

```text
用户请求
   │
   ▼
阿里云 SLB / Nginx
   │
   ▼
ECS / 容器服务部署 Spring Boot 应用（多实例）
   │
   ├── 阿里云 RDS MySQL 8.0（主从）
   └── 阿里云 Redis 7.0（集群/主从）
```

#### 本地开发兼容

- 无 MySQL 时：使用 Docker 启动 MySQL 8.0 容器。
- 无 Redis 时：使用 Docker 启动 Redis 7.0 容器。
- 项目配置 `application-dev.yml` 指向本地 Docker 服务。
- 生产配置 `application-prod.yml` 指向阿里云 RDS / Redis。

Docker Compose 本地启动示例：

```yaml
version: '3'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: activity_db
    ports:
      - "3306:3306"
  redis:
    image: redis:7.0
    ports:
      - "6379:6379"
```

### 2.4 数据流图

```text
1. 运营创建活动
   运营后台 ──POST──▶ ActivityAdminController
                          │
                          ▼
                   写入 activity / activity_rule / reward / stock 表
                          │
                          ▼
                   发布时同步到 Redis（活动配置 + 库存预热）

2. 用户查看活动列表
   C端 ──GET──▶ ActivityUserController
                    │
                    ▼
             先读 Redis 缓存
                    │
        ┌──────────┴──────────┐
        ▼                     ▼
      命中缓存              未命中
        │                     │
        ▼                     ▼
     返回列表              读数据库并回写缓存

3. 用户领取
   C端 ──POST──▶ ActivityUserController
                     │
                     ▼
              参数校验 + 幂等校验（request_id）
                     │
                     ▼
              资格校验（时间、状态、用户次数）
                     │
                     ▼
              获取分布式锁（userId + activityId）
                     │
                     ▼
              Redis 原子扣减库存 / 数据库扣减
                     │
                     ▼
              写入领取记录（发放中）
                     │
                     ▼
              调用奖励发放服务
                     │
                     ▼
              更新领取记录状态（成功/失败）
                     │
                     ▼
              失败进入补偿任务 / 库存回滚
```

---

## 3. 具体细节方案

### 3.1 数据库设计

#### 3.1.1 表清单

| 序号 | 表名 | 说明 |
|------|------|------|
| 1 | `activity` | 活动主表 |
| 2 | `activity_rule` | 活动规则表 |
| 3 | `reward` | 奖励配置表 |
| 4 | `reward_stock` | 奖励库存表 |
| 5 | `user_take_record` | 用户领取记录表 |
| 6 | `user_reward_account` | 用户奖励账户表（券/红包实例） |
| 7 | `stock_change_log` | 库存变更日志表 |
| 8 | `idempotent_record` | 幂等记录表 |
| 9 | `activity_publish_log` | 活动发布日志表 |

#### 3.1.2 表结构

##### 1. activity（活动主表）

```sql
CREATE TABLE `activity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `activity_code` varchar(64) NOT NULL COMMENT '活动唯一编码',
  `activity_name` varchar(128) NOT NULL COMMENT '活动名称',
  `activity_type` tinyint NOT NULL DEFAULT '1' COMMENT '活动类型：1-领取型 2-抽奖型 3-任务型',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-草稿 1-已发布 2-已下架',
  `start_time` datetime NOT NULL COMMENT '活动开始时间',
  `end_time` datetime NOT NULL COMMENT '活动结束时间',
  `visible_scope` tinyint NOT NULL DEFAULT '1' COMMENT '可见范围：1-全部 2-白名单 3-黑名单',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `updator` varchar(64) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_code` (`activity_code`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`),
  KEY `idx_type_status` (`activity_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动主表';
```

##### 2. activity_rule（活动规则表）

```sql
CREATE TABLE `activity_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `rule_type` varchar(32) NOT NULL COMMENT '规则类型：TIME/USER_LIMIT/BLACKLIST/WHITELIST',
  `rule_key` varchar(64) NOT NULL COMMENT '规则KEY',
  `rule_value` text NOT NULL COMMENT '规则值，JSON格式',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_rule` (`activity_id`, `rule_type`, `rule_key`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动规则表';
```

##### 3. reward（奖励配置表）

```sql
CREATE TABLE `reward` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `reward_code` varchar(64) NOT NULL COMMENT '奖励编码',
  `reward_name` varchar(128) NOT NULL COMMENT '奖励名称',
  `reward_type` tinyint NOT NULL COMMENT '奖励类型：1-优惠券 2-红包 3-积分 4-实物',
  `reward_config` json NOT NULL COMMENT '奖励配置JSON：面额、有效期等',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reward_code` (`reward_code`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励配置表';
```

##### 4. reward_stock（奖励库存表）

```sql
CREATE TABLE `reward_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `reward_id` bigint unsigned NOT NULL COMMENT '奖励ID',
  `total_stock` int unsigned NOT NULL DEFAULT '0' COMMENT '总库存',
  `taken_stock` int unsigned NOT NULL DEFAULT '0' COMMENT '已领取库存',
  `remain_stock` int unsigned NOT NULL DEFAULT '0' COMMENT '剩余库存',
  `version` int unsigned NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_reward` (`activity_id`, `reward_id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励库存表';
```

##### 5. user_take_record（用户领取记录表）

```sql
CREATE TABLE `user_take_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) NOT NULL COMMENT '幂等请求ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `activity_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `reward_id` bigint unsigned NOT NULL COMMENT '奖励ID',
  `take_status` tinyint NOT NULL DEFAULT '1' COMMENT '领取状态：1-发放中 2-成功 3-失败',
  `take_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `reward_info` json DEFAULT NULL COMMENT '发放奖励信息',
  `fail_reason` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  UNIQUE KEY `uk_user_activity_reward` (`user_id`, `activity_id`, `reward_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_take_time` (`take_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领取记录表';
```

> 注：如果业务允许一个用户多次领取同一奖励，则 `uk_user_activity_reward` 需要调整。

##### 6. user_reward_account（用户奖励账户表）

```sql
CREATE TABLE `user_reward_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `take_record_id` bigint unsigned NOT NULL COMMENT '领取记录ID',
  `reward_type` tinyint NOT NULL COMMENT '奖励类型',
  `reward_code` varchar(64) NOT NULL COMMENT '奖励编码',
  `reward_status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-未使用 2-已使用 3-已过期',
  `valid_start_time` datetime DEFAULT NULL COMMENT '有效期开始',
  `valid_end_time` datetime DEFAULT NULL COMMENT '有效期结束',
  `extra_info` json DEFAULT NULL COMMENT '扩展信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_take_record` (`take_record_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_status` (`user_id`, `reward_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户奖励账户表';
```

##### 7. stock_change_log（库存变更日志表）

```sql
CREATE TABLE `stock_change_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint unsigned NOT NULL,
  `reward_id` bigint unsigned NOT NULL,
  `change_type` tinyint NOT NULL COMMENT '变更类型：1-扣减 2-回滚 3-人工调整',
  `change_num` int NOT NULL COMMENT '变更数量（扣减为负）',
  `before_num` int unsigned NOT NULL COMMENT '变更前数量',
  `after_num` int unsigned NOT NULL COMMENT '变更后数量',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务ID（领取记录ID/request_id）',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_reward` (`activity_id`, `reward_id`),
  KEY `idx_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变更日志表';
```

##### 8. idempotent_record（幂等记录表）

```sql
CREATE TABLE `idempotent_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) NOT NULL COMMENT '幂等请求ID',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型：TAKE_REWARD',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务单号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-处理中 2-成功 3-失败',
  `response` text COMMENT '响应结果JSON',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等记录表';
```

##### 9. activity_publish_log（活动发布日志表）

```sql
CREATE TABLE `activity_publish_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `activity_id` bigint unsigned NOT NULL,
  `publish_status` tinyint NOT NULL COMMENT '发布状态：1-成功 2-失败',
  `publish_content` json COMMENT '发布内容快照',
  `error_msg` varchar(512) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动发布日志表';
```

#### 3.1.3 ER 关系描述

```text
activity 1 ──────── N activity_rule
activity 1 ──────── N reward
activity 1 ──────── 1 reward_stock（按奖励维度）
reward   1 ──────── 1 reward_stock

user_take_record N ── 1 activity
user_take_record N ── 1 reward
user_take_record 1 ───── 1 user_reward_account
user_take_record 1 ───── N stock_change_log
```

---

## 3.2 接口设计

### 3.2.1 管理后台接口

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/admin/activity/create` | 创建活动 |
| POST | `/admin/activity/update` | 编辑活动 |
| POST | `/admin/activity/publish/{activityId}` | 发布活动 |
| POST | `/admin/activity/offline/{activityId}` | 下架活动 |
| GET | `/admin/activity/detail/{activityId}` | 活动详情 |
| GET | `/admin/activity/list` | 活动列表（分页） |
| POST | `/admin/activity/stock/adjust` | 库存调整 |

### 3.2.2 C 端接口

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/activity/list` | 活动列表 |
| GET | `/api/activity/detail/{activityId}` | 活动详情 |
| POST | `/api/activity/take` | 领取奖励 |
| GET | `/api/activity/my-records` | 我的领取记录 |

### 3.2.3 接口字段示例

#### 创建活动

请求：

```json
POST /admin/activity/create
{
  "activityName": "618新人红包",
  "activityCode": "618_NEW_USER_RED_PACKET_2026",
  "activityType": 1,
  "startTime": "2026-06-01 00:00:00",
  "endTime": "2026-06-30 23:59:59",
  "visibleScope": 1,
  "rules": [
    { "ruleType": "USER_LIMIT", "ruleKey": "TOTAL", "ruleValue": "{\"limit\": 1}" },
    { "ruleType": "TIME", "ruleKey": "DAILY", "ruleValue": "{\"start\":\"10:00:00\",\"end\":\"22:00:00\"}" }
  ],
  "rewards": [
    {
      "rewardCode": "RP_618_10",
      "rewardName": "10元红包",
      "rewardType": 2,
      "rewardConfig": "{\"amount\":10,\"currency\":\"CNY\",\"validDays\":7}",
      "totalStock": 10000
    }
  ]
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "activityId": 10001
  }
}
```

#### 领取奖励

请求：

```json
POST /api/activity/take
{
  "activityId": 10001,
  "requestId": "uuid-generated-by-client"
}
```

响应成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "takeRecordId": 888888,
    "rewardName": "10元红包",
    "rewardInfo": { "amount": 10 }
  }
}
```

响应失败：

```json
{
  "code": 400101,
  "message": "活动未开始"
}
```

### 3.2.4 状态码设计

| 码段 | 含义 |
|------|------|
| 200 | 成功 |
| 400xxx | 客户端错误（参数、幂等、资格等） |
| 500xxx | 服务端错误 |

示例：

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400001 | 参数错误 |
| 400100 | 活动不存在 |
| 400101 | 活动未开始 |
| 400102 | 活动已结束 |
| 400103 | 活动未上架 |
| 400104 | 库存不足 |
| 400105 | 已达到领取上限 |
| 400106 | 已领取过该奖励 |
| 400107 | 重复请求 |
| 500001 | 系统繁忙 |
| 500002 | 库存扣减失败 |
| 500003 | 发放失败 |

### 3.2.5 幂等设计

- 所有写接口强制携带 `requestId`。
- 服务端先查 `idempotent_record`：
  - 存在且成功：直接返回缓存响应。
  - 存在且处理中：根据时间判断是否超时，超时重试，未超时返回「处理中」。
  - 不存在：插入处理中记录，执行业务，更新为成功/失败并缓存响应。

---

## 3.3 核心流程

### 3.3.1 活动创建/发布流程

```text
1. 运营后台调用 /admin/activity/create
2. 校验参数合法性（时间、编码唯一性、库存非负）
3. 写入 activity 表（status=0 草稿）
4. 写入 activity_rule 表
5. 写入 reward 表
6. 写入 reward_stock 表
7. 返回 activityId

8. 运营点击「发布」调用 /admin/activity/publish/{activityId}
9. 校验活动状态、时间有效性
10. 更新 activity.status = 1（已发布）
11. 同步活动配置到 Redis：
    - activity:config:{activityId}
    - activity:rule:{activityId}
    - activity:stock:{activityId}:{rewardId}
12. 写入 activity_publish_log
13. 返回发布成功
```

### 3.3.2 用户查看活动列表流程

```text
1. C端调用 /api/activity/list
2. 从 Redis 获取已发布活动ID列表：activity:published:list
3. 遍历活动ID，批量从 Redis 获取活动配置
4. 过滤：
   - 当前时间在 start_time 和 end_time 之间
   - 用户可见范围匹配
5. 按权重/时间排序
6. 返回列表

降级：
- 若 Redis 未命中，从数据库查询已发布活动并回写缓存
```

### 3.3.3 用户领取流程

```text
1. C端调用 /api/activity/take
2. 参数校验（activityId、requestId 非空）
3. 幂等校验（查 idempotent_record）
4. 从 Redis 获取活动配置
   - 未命中则查数据库并回写
5. 资格校验：
   - 活动状态、时间
   - 用户黑名单/白名单
   - 用户领取次数限制
   - 是否已领取过
6. 获取分布式锁：lock:take:{userId}:{activityId}
7. 再次校验资格（获取锁后二次确认）
8. 库存扣减：
   - Redis Lua 脚本原子扣减 remain_stock
   - 或数据库乐观锁扣减
9. 写入 user_take_record（状态=发放中）
10. 写入 stock_change_log
11. 调用奖励发放服务
12. 更新 user_take_record 状态（成功/失败）
13. 发放失败时：库存回滚 + 进入补偿任务
14. 释放分布式锁
15. 更新幂等记录并返回结果
```

### 3.3.4 配置变更同步到缓存流程

```text
方式一：发布时同步
- 运营发布/下架时，主动刷新 Redis

方式二：定时同步
- 每分钟扫描 activity.update_time 有变更的记录
- 增量同步到 Redis

方式三：缓存失效
- 运营编辑活动时，删除对应 Redis Key
- 下次访问时从数据库加载并回写
```

---

## 3.4 关键技术与实现细节

### 3.4.1 活动配置模型扩展设计

采用「活动主表 + 规则表 + 奖励表」的插件化模型：

```java
// 活动类型枚举
public enum ActivityTypeEnum {
    TAKE(1, "领取型"),
    LOTTERY(2, "抽奖型"),
    TASK(3, "任务型");
}

// 规则抽象
public interface ActivityRule {
    String getRuleType();
    boolean validate(ActivityContext context);
}

// 领取型规则实现
public class UserLimitRule implements ActivityRule { ... }
public class TimeRule implements ActivityRule { ... }
```

扩展新活动类型时：

1. 新增 `ActivityTypeEnum`。
2. 新增规则实现类。
3. 新增领取/抽奖/任务处理器。
4. 通过策略模式路由。

### 3.4.2 库存扣减方案

| 方案 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| 数据库悲观锁 | 低并发、库存敏感 | 强一致 | 性能差，易阻塞 |
| 数据库乐观锁 | 中低并发 | 无锁、性能好 | 高并发下重试多 |
| Redis 原子扣减 | 高并发 | 性能最好 | 需处理缓存与数据库一致性 |

推荐方案：**Redis 原子扣减 + 数据库异步对账**

```java
// Redis Lua 扣减脚本
String lua =
    "if (redis.call('exists', KEYS[1]) == 1) then " +
    "    local stock = tonumber(redis.call('get', KEYS[1])); " +
    "    if (stock == -1) then return 1; end " +  // -1 表示不限量
    "    if (stock > 0) then " +
    "        redis.call('decrby', KEYS[1], 1); " +
    "        return 1; " +
    "    end; " +
    "    return 0; " +
    "end; " +
    "return -1;";
```

流程：

1. 先扣 Redis 库存。
2. 扣减成功再写领取记录。
3. 定时任务将 Redis 扣减量同步回数据库（或对账）。
4. 库存耗尽时从数据库兜底校验。

### 3.4.3 幂等与防重设计

- 数据库唯一索引：`user_take_record` 的 `uk_request_id` 和 `uk_user_activity_reward`。
- Redis 幂等缓存：`idempotent:{requestId}`，设置过期时间。
- 领取接口必须携带 requestId。
- 分布式锁防止并发重复领取：`lock:take:{userId}:{activityId}`。

### 3.4.4 缓存一致性

- 读多写少：活动配置读 Redis，失效后查数据库回写。
- 库存强一致：Redis 扣减为主，数据库为兜底。
- 发布/下架：主动删除/刷新 Redis。
- 定时对账：每小时比对 Redis 库存与数据库库存，不一致报警。

### 3.4.5 限流/防刷

| 层级 | 实现 |
|------|------|
| 接口级 | Sentinel 配置单机 QPS 阈值 |
| 用户级 | Redis 计数器：`take:limit:{userId}:{activityId}:{date}` |
| IP 级 | Nginx / 网关层限流，或 Redis 计数器 |
| 风控 | 单位时间内领取次数异常触发验证码/封禁 |

### 3.4.6 分布式锁选型

使用 **Redisson**：

```java
RLock lock = redissonClient.getLock("lock:take:" + userId + ":" + activityId);
boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
if (!locked) {
    throw new BizException("系统繁忙，请稍后再试");
}
try {
    // 执行业务
} finally {
    lock.unlock();
}
```

锁粒度：按 `userId + activityId`，避免用户并发领取导致超卖。

### 3.4.7 任务补偿/对账机制

#### 补偿任务

- 扫描 `user_take_record` 中状态为「发放中」且超过 5 分钟的记录。
- 调用发放服务重试。
- 重试 3 次仍失败则标记为失败并回滚库存。

#### 对账任务

- 每小时比对 `reward_stock` 与 Redis 库存。
- 比对 `user_take_record` 与 `user_reward_account` 数量。
- 不一致记录报警并人工介入。

---

## 4. 项目目录与模块划分

### 4.1 Maven 项目结构

```text
activity-config/
├── activity-config-api/              # 对外 API 接口定义
│   ├── src/main/java/com/example/activity/api/
│   │   ├── request/
│   │   ├── response/
│   │   └── facade/
│   └── pom.xml
│
├── activity-config-admin/            # 运营后台服务（可独立部署）
│   ├── src/main/java/com/example/activity/admin/
│   │   ├── controller/
│   │   ├── service/
│   │   └── config/
│   └── pom.xml
│
├── activity-config-user/             # C 端用户服务（可独立部署）
│   ├── src/main/java/com/example/activity/user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── handler/
│   │   └── config/
│   └── pom.xml
│
├── activity-config-common/           # 公共依赖
│   ├── src/main/java/com/example/activity/common/
│   │   ├── entity/                  # 数据库实体
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── enums/
│   │   ├── exception/
│   │   ├── util/
│   │   ├── lock/
│   │   ├── idempotent/
│   │   └── constant/
│   └── pom.xml
│
├── activity-config-service/          # 核心业务逻辑
│   ├── src/main/java/com/example/activity/service/
│   │   ├── activity/
│   │   ├── reward/
│   │   ├── stock/
│   │   ├── take/
│   │   ├── rule/
│   │   └── compensate/
│   └── pom.xml
│
├── activity-config-start/            # 启动模块（聚合）
│   ├── src/main/java/com/example/activity/
│   │   └── ActivityConfigApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── mapper/
│   └── pom.xml
│
├── pom.xml                           # 父 POM
└── README.md
```

### 4.2 关键包名与类职责

| 包名 | 职责 |
|------|------|
| `com.example.activity.common.entity` | 数据库实体类 |
| `com.example.activity.common.mapper` | 数据访问层 |
| `com.example.activity.common.enums` | 枚举常量 |
| `com.example.activity.common.exception` | 全局异常与错误码 |
| `com.example.activity.common.lock` | Redisson 分布式锁封装 |
| `com.example.activity.common.idempotent` | 幂等组件 |
| `com.example.activity.service.activity` | 活动域服务 |
| `com.example.activity.service.reward` | 奖励域服务 |
| `com.example.activity.service.stock` | 库存域服务 |
| `com.example.activity.service.take` | 领取流程编排 |
| `com.example.activity.service.rule` | 规则引擎 |
| `com.example.activity.service.compensate` | 补偿任务 |
| `com.example.activity.admin.controller` | 后台管理接口 |
| `com.example.activity.user.controller` | C 端接口 |
| `com.example.activity.user.handler` | 不同活动类型处理器 |

### 4.3 关键类名

| 类名 | 职责 |
|------|------|
| `ActivityConfigApplication` | Spring Boot 启动类 |
| `ActivityAdminController` | 活动管理接口 |
| `ActivityUserController` | C 端活动接口 |
| `ActivityService` | 活动核心服务 |
| `RewardService` | 奖励核心服务 |
| `StockService` | 库存服务 |
| `TakeService` | 领取编排服务 |
| `TakeActivityHandler` | 领取型活动处理器 |
| `RuleEngine` | 规则校验引擎 |
| `IdempotentService` | 幂等控制服务 |
| `DistributedLockService` | 分布式锁服务 |
| `StockCompensateJob` | 库存补偿定时任务 |

---

## 5. 实现步骤（Step by Step）

### Task 1：项目初始化

- 使用 Spring Initializr 或 IDEA 创建 Maven 多模块项目。
- 配置父 POM，统一 JDK 17、Spring Boot 3.2.x、依赖版本。
- 提交第一次 Git commit。

输出：`pom.xml` 父工程骨架。

### Task 2：本地开发环境搭建

- 编写 `docker-compose.yml` 启动 MySQL 8.0 + Redis 7.0。
- 创建数据库 `activity_db`。
- 配置 `application-dev.yml`。

验证：本地可连接 MySQL 和 Redis。

### Task 3：公共模块搭建

- 创建 `activity-config-common`，引入 MyBatis-Plus、Lombok、Hutool、Redisson。
- 配置 MyBatis-Plus、分页插件、统一返回结果 `Result`、全局异常处理。

输出：公共依赖可用。

### Task 4：数据库建表

- 按第 3.1 节 SQL 创建所有表。
- 使用 Flyway 或手动执行。
- 配置 MyBatis-Plus 实体和 Mapper。

验证：表创建成功，Mapper 可正常查询。

### Task 5：活动配置后台接口

- 实现 `ActivityAdminController`。
- 实现活动创建、编辑、详情、列表接口。
- 使用事务保证 activity + rule + reward + stock 同时写入。

验证：Postman 可创建活动。

### Task 6：奖励与库存服务

- 实现 `RewardService` 和 `StockService`。
- 库存初始化写入 `reward_stock`。
- 实现库存调整接口。

验证：活动创建时库存正确初始化。

### Task 7：Redis 缓存接入

- 配置 Redisson 和 RedisTemplate。
- 实现活动配置缓存读取/写入/删除工具类。
- 实现库存预热：发布时写入 Redis。

验证：发布后 Redis 中存在活动配置和库存 Key。

### Task 8：C 端活动列表/详情接口

- 实现 `ActivityUserController.list()` 和 `detail()`。
- 从 Redis 读取活动配置，未命中查数据库。
- 按业务规则过滤展示。

验证：接口返回正确活动数据。

### Task 9：规则引擎实现

- 定义 `ActivityRule` 接口。
- 实现时间规则、用户领取次数规则。
- 在领取流程中调用规则引擎校验。

验证：不同规则能正确拦截或放行。

### Task 10：幂等组件实现

- 实现 `IdempotentService`。
- 使用数据库 + Redis 双重校验。
- 在领取接口中接入。

验证：同一 requestId 重复提交返回相同结果。

### Task 11：分布式锁封装

- 封装 `DistributedLockService`。
- 领取流程中加锁 `lock:take:{userId}:{activityId}`。

验证：并发请求不会重复领取。

### Task 12：库存扣减与领取流程

- 实现 `TakeService.take()`。
- 使用 Redis Lua 脚本原子扣减库存。
- 写入领取记录（发放中）。
- 调用奖励发放（模拟实现）。

验证：库存正确扣减，记录正确生成。

### Task 13：奖励发放与状态更新

- 实现 `RewardGrantService`。
- 发放成功更新记录状态为成功，写入 `user_reward_account`。
- 发放失败标记失败并回滚库存。

验证：成功/失败场景状态正确。

### Task 14：补偿与对账任务

- 实现定时任务扫描「发放中」记录进行补偿。
- 实现库存对账任务。

验证：异常记录被补偿，不一致被记录。

### Task 15：限流、测试与文档

- 接入 Sentinel 或自定义 Redis 限流。
- 编写单元测试和接口测试。
- 输出接口文档（SpringDoc）。

验证：压测不超时、不重复领取、不超卖。

---

## 6. 风险与建议

### 6.1 常见坑

| 风险 | 说明 | 建议 |
|------|------|------|
| 超卖 | 并发下库存扣减非原子 | 使用 Redis Lua 脚本 + 分布式锁 |
| 缓存击穿 | 热点活动缓存失效，大量请求打到数据库 | 缓存过期加随机值、互斥锁重建缓存 |
| 缓存与数据库库存不一致 | Redis 扣减后未同步数据库 | 定时对账 + 发布时全量同步 |
| 幂等失效 | requestId 生成重复或缓存未命中 | 数据库唯一索引兜底 |
| 活动配置未生效 | 运营发布但未同步缓存 | 发布接口强制刷新 Redis + 定时补偿 |
| 分布式锁未释放 | 异常导致锁未释放 | 使用 Redisson watch dog 自动续期 |
| 领取记录重复 | 用户并发重复领取 | 分布式锁 + 数据库唯一索引 |

### 6.2 面试常问点

1. **如何防止超卖？**
   - Redis 原子扣减 + 数据库乐观锁/唯一索引兜底 + 分布式锁。

2. **幂等怎么做的？**
   - requestId + Redis 缓存 + 数据库唯一索引 + 幂等记录表。

3. **缓存一致性如何保证？**
   - 发布时主动刷新、编辑时删除缓存、定时对账。

4. **高并发下怎么设计？**
   - 读走缓存、扣库存走 Redis、锁按用户粒度、限流/降级。

5. **如果 Redis 挂了怎么办？**
   - 降级走数据库悲观锁/乐观锁，限流保护数据库。

6. **如何支持后续抽奖/任务类型？**
   - 活动类型枚举 + 规则插件 + 策略模式处理器。

7. **库存扣减失败如何回滚？**
   - 记录 stock_change_log，发放失败时 Redis 回滚 + 数据库回滚。

8. **领取记录和发奖如何保证一致？**
   - 本地事务 + 最终一致性补偿 + 定时对账。

---

## 验证方式

1. 本地启动 Docker 版 MySQL + Redis。
2. 按顺序完成 Task 1 ~ Task 15，每步用 Postman / JUnit 验证。
3. 最终压测领取接口，验证：
   - 同一用户同一活动不重复领取；
   - 总领取量不超过库存；
   - 缓存与数据库库存一致；
   - 活动状态/时间规则正确拦截。
