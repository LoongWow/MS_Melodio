# 微服务拆分计划：user-music-service → user-service + music-service

## 项目概况

**当前状态：**
- `user-music-service` 是一个混合了用户认证和音乐功能的单体微服务
- 端口：8081
- 使用 Spring Cloud 架构（Eureka + Zuul + Config Server）
- 数据库：MySQL（music_agent）
- 缓存：Redis
- MyBatis 注解方式（无 XML mapper）

**拆分目标：**
将 `user-music-service` 无损拆分为两个独立微服务：
1. **user-service**（用户服务）：专门处理用户认证、登录、注册
2. **music-service**（音乐服务）：专门处理音乐搜索、播放、历史记录

---

## 功能划分分析

### 🔐 用户服务 (user-service) - 端口 8082

**核心职责：**用户身份认证、账户管理、网易云 Cookie 管理

**Controller 层：**
- `/api/user/login` - 账号密码登录
- `/api/user/register` - 用户注册
- `/api/user/netease-login` - 网易云手机号/邮箱登录
- `/api/user/cookie-status` - 验证 Cookie 有效性
- `/api/user/send-captcha` - 发送验证码
- `/api/user/captcha-login` - 验证码登录
- `/api/user/qr-key` - 生成二维码 Key
- `/api/user/qr-create` - 创建二维码图片
- `/api/user/qr-check` - 检查二维码状态
- `/api/user/qr-login` - 完成二维码登录

**Service 层：**
- `AuthService` - 账号密码登录/注册
- `NeteaseCookieService` - 网易云账号登录
- `CaptchaLoginService` - 验证码登录
- `QrLoginService` - 二维码登录
- `UserCookieManagementService`（新增）- 统一管理用户 Cookie，提供 FeignClient 接口

**Entity 层：**
- `UserAccount` - 用户账户信息
- `UserCookie` - 用户 Cookie 存储

**Repository 层：**
- `UserAccountMapper`
- `UserCookieMapper`

**数据库表：**
- `user_account`
- `user_cookie`

---

### 🎵 音乐服务 (music-service) - 端口 8083

**核心职责：**音乐搜索、播放、歌词、历史记录、用户偏好分析

**Controller 层：**
- `/api/music/search` - 搜索歌曲
- `/api/music/play-url` - 获取播放链接
- `/api/music/cache-song-id` - 从缓存获取歌曲 ID
- `/api/music/lyric/new` - 获取歌词
- `/api/music/play-history` - 记录/获取播放历史
- `/api/music/user-preference` - 获取用户偏好分析

**Service 层：**
- `MusicApiService` - 网易云 API 调用（搜索、播放链接、歌词）
- `PlayHistoryService` - 播放历史管理
- `UserPreferenceService` - 用户偏好分析
- `UserCookieFeignClient`（新增）- 通过 Feign 调用 user-service 获取 Cookie

**Entity 层：**
- `PlayHistory` - 播放历史
- `UserPreference` - 用户偏好统计

**Repository 层：**
- `PlayHistoryMapper`
- `UserPreferenceMapper`

**数据库表：**
- `play_history`
- `user_preference`
- `recommend_cache_log`
- `agent_conversation`

**Tools 层：**
- `MusicTools` - 音乐播放工具（需要通过 Feign 获取 Cookie）

---

## 服务间通信设计

### 关键依赖关系

**music-service 需要调用 user-service 的场景：**

1. **获取用户 Cookie**
   - 场景：`MusicApiService.getPlayUrl()` 需要 Cookie 调用网易云 API
   - 方案：通过 `UserCookieFeignClient` 调用 `user-service` 的 `/api/user/cookie/{musicUserId}` 接口

2. **验证用户身份**
   - 场景：记录播放历史、用户偏好时需要验证用户是否存在
   - 方案：通过 Feign 调用 `user-service` 的 `/api/user/exists/{musicUserId}` 接口（新增）

### Feign Client 接口设计

```java
// music-service 中新增
@FeignClient(name = "user-service", fallback = UserCookieFeignClientFallback.class)
public interface UserCookieFeignClient {
    
    @GetMapping("/api/user/cookie/{musicUserId}")
    String getCookie(@PathVariable("musicUserId") Long musicUserId);
    
    @GetMapping("/api/user/exists/{musicUserId}")
    Boolean userExists(@PathVariable("musicUserId") Long musicUserId);
}
```

---

## 配置变更

### user-service 配置 (config-repo/user-service.yml)

```yaml
server:
  port: 8082

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3306/music_agent?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password: 123456

mybatis:
  type-aliases-package: org.example.entity
  configuration:
    map-underscore-to-camel-case: true

music:
  api:
    base-url: http://localhost:3000

logging:
  level:
    org.example.repo: DEBUG
```

### music-service 配置 (config-repo/music-service.yml)

```yaml
server:
  port: 8083

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

spring:
  application:
    name: music-service
  datasource:
    url: jdbc:mysql://localhost:3306/music_agent?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password: 123456

mybatis:
  type-aliases-package: org.example.entity
  configuration:
    map-underscore-to-camel-case: true

music:
  api:
    base-url: http://localhost:3000

feign:
  hystrix:
    enabled: true

logging:
  level:
    org.example.repo: DEBUG
```

### Zuul Gateway 路由更新

```yaml
zuul:
  routes:
    # 用户服务路由
    user-login:
      path: /api/user/**
      serviceId: user-service
      stripPrefix: false
    
    # 音乐服务路由
    music-service:
      path: /api/music/**
      serviceId: music-service
      stripPrefix: false
    
    # AI 服务路由保持不变
    ai-chat:
      path: /api/music/chat
      serviceId: ai-service
      stripPrefix: false
    # ... 其他 AI 路由保持不变
```

---

## 迁移步骤

### Phase 1: 创建新服务骨架

**步骤 1.1：创建 user-service 目录结构**
- 复制 user-music-service 作为模板
- 清理不需要的音乐相关代码
- 重命名主类为 `UserServiceApplication`
- 更新 pom.xml 的 artifactId 为 `user-service`

**步骤 1.2：创建 music-service 目录结构**
- 复制 user-music-service 作为模板
- 清理不需要的用户认证代码
- 重命名主类为 `MusicServiceApplication`
- 更新 pom.xml 的 artifactId 为 `music-service`
- 添加 `spring-cloud-starter-openfeign` 依赖

### Phase 2: 迁移 user-service 代码

**步骤 2.1：保留用户相关代码**
- ✅ `AuthService`
- ✅ `NeteaseCookieService`
- ✅ `CaptchaLoginService`
- ✅ `QrLoginService`
- ✅ `UserAccount`、`UserCookie` 实体
- ✅ `UserAccountMapper`、`UserCookieMapper`
- ✅ 所有 DTO：`LoginRequest`、`LoginResponse`、`RegisterRequest`、`NeteaseCookieResponse` 等

**步骤 2.2：创建新的 UserController**
- 从 `MusicController` 中提取所有用户登录相关的接口
- 更新 `@RequestMapping` 为 `/api/user`
- 新增 `/api/user/cookie/{musicUserId}` 接口供 music-service 调用
- 新增 `/api/user/exists/{musicUserId}` 接口供 music-service 调用

**步骤 2.3：清理不需要的代码**
- ❌ 删除 `MusicApiService`（音乐相关）
- ❌ 删除 `PlayHistoryService`、`UserPreferenceService`
- ❌ 删除 `MusicTools`
- ❌ 删除音乐相关 Entity 和 Mapper

**步骤 2.4：更新配置**
- 修改 `bootstrap.yml` 中的 `spring.application.name` 为 `user-service`
- 在 config-server 中创建 `user-service.yml`

### Phase 3: 迁移 music-service 代码

**步骤 3.1：保留音乐相关代码**
- ✅ `MusicApiService`
- ✅ `PlayHistoryService`
- ✅ `UserPreferenceService`
- ✅ `MusicTools`
- ✅ `PlayHistory`、`UserPreference` 实体
- ✅ `PlayHistoryMapper`、`UserPreferenceMapper`
- ✅ 所有音乐相关 DTO：`MusicSongDto`、`SearchResponse`、`PlayUrlResponse` 等

**步骤 3.2：创建新的 MusicController**
- 从原 `MusicController` 中提取所有音乐相关的接口
- 保持 `@RequestMapping` 为 `/api/music`
- 移除所有用户登录相关的接口

**步骤 3.3：实现 Feign Client**
- 创建 `UserCookieFeignClient` 接口
- 实现 Fallback 类 `UserCookieFeignClientFallback`
- 修改 `MusicApiService.getAuthCookie()` 方法，改为调用 Feign Client

**步骤 3.4：清理不需要的代码**
- ❌ 删除 `AuthService`、`NeteaseCookieService`、`CaptchaLoginService`、`QrLoginService`
- ❌ 删除 `UserAccount`、`UserCookie` 实体
- ❌ 删除 `UserAccountMapper`、`UserCookieMapper`
- ⚠️ 保留部分用户相关 DTO（`LoginResponse` 等），因为前端仍需这些结构

**步骤 3.5：更新配置**
- 修改 `bootstrap.yml` 中的 `spring.application.name` 为 `music-service`
- 在 config-server 中创建 `music-service.yml`
- 启用 Feign 和 Hystrix

### Phase 4: 更新配置中心和网关

**步骤 4.1：更新 Config Server**
- 在 `config-repo` 中创建 `user-service.yml`
- 在 `config-repo` 中创建 `music-service.yml`
- 保留原 `user-music-service.yml`（备份）

**步骤 4.2：更新 Zuul Gateway**
- 修改 `zuul-gateway/src/main/resources/application.yml`
- 添加 `user-service` 路由：`/api/user/**`
- 修改 `music-service` 路由：`/api/music/**`
- 保留原 `user-music-service` 路由（备份）

### Phase 5: 更新前端 API 调用

**步骤 5.1：无需修改前端代码**
- ✅ 因为 Zuul 网关已经做了路由映射
- ✅ 前端仍然调用 `/api/user/**` 和 `/api/music/**`
- ✅ 网关会自动转发到对应的新服务

**步骤 5.2：验证前端功能**
- 测试登录功能
- 测试搜索音乐功能
- 测试播放音乐功能
- 测试播放历史功能

### Phase 6: 数据库迁移（无需修改）

**步骤 6.1：数据库共享方案**
- ✅ 两个服务共享同一个数据库 `music_agent`
- ✅ user-service 只访问 `user_account` 和 `user_cookie` 表
- ✅ music-service 只访问 `play_history` 和 `user_preference` 表
- ⚠️ 注意：避免跨服务直接访问对方的表

**步骤 6.2：Redis 缓存（无需修改）**
- ✅ 两个服务共享同一个 Redis 实例
- ✅ user-service 使用 `music:auth:*` 键存储 Cookie
- ✅ music-service 使用 `music:recommend:*` 等键存储音乐数据

### Phase 7: 测试验证

**步骤 7.1：单元测试迁移**
- 将 user-music-service 的测试分别迁移到两个新服务
- 用户相关测试 → user-service
- 音乐相关测试 → music-service

**步骤 7.2：集成测试**
- 启动 Eureka Server
- 启动 Config Server
- 启动 user-service
- 启动 music-service
- 启动 Zuul Gateway
- 验证服务注册
- 验证 Feign 调用

**步骤 7.3：端到端测试**
- 测试完整的用户登录流程
- 测试完整的音乐播放流程
- 测试播放历史记录功能
- 测试用户偏好分析功能

---

## 关键技术点

### 1. 编码问题（避免乱码）

**问题分析：**
- 源代码中存在部分中文注释出现乱码（如 `涓€у寲`、`鑹烘湳瀹?`）
- 可能是原文件编码不一致导致

**解决方案：**
✅ **所有文件使用 UTF-8 编码**
- pom.xml 中已配置：
  ```xml
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
  ```
- Maven Compiler Plugin 已配置 UTF-8
- 迁移时使用 Java 代码复制文件内容，确保编码正确

### 2. 无损迁移保证

**数据完整性：**
- ✅ 数据库表结构完全保留
- ✅ 所有业务逻辑代码原封不动
- ✅ 配置文件内容保持一致（只修改服务名和端口）

**功能完整性：**
- ✅ 所有 API 接口保持不变（URL 路径不变）
- ✅ 前端无需任何修改
- ✅ 通过 Feign Client 实现服务间调用，逻辑透明

**测试覆盖：**
- ✅ 迁移所有原有的单元测试
- ✅ 验证所有功能点正常工作

### 3. 配置管理

**Config Server 配置：**
- 创建独立的 `user-service.yml` 和 `music-service.yml`
- 每个服务的 `bootstrap.yml` 指向 Config Server
- 保留原 `user-music-service.yml` 作为备份

**Eureka 服务注册：**
- 两个服务使用不同的 `spring.application.name`
- 自动注册到 Eureka Server
- Feign Client 通过服务名发现服务

### 4. 事务处理

**user-service：**
- 用户注册时需要同时插入 `user_account` 和 `user_cookie`
- 使用 `@Transactional` 保证原子性

**music-service：**
- 记录播放历史时需要同时更新 `play_history` 和 `user_preference`
- 使用 `@Transactional` 保证原子性

**跨服务调用：**
- Feign 调用不在同一个事务中
- 采用最终一致性设计
- 如果 Cookie 获取失败，返回友好错误提示

---

## 风险评估与回滚方案

### 潜在风险

1. **Feign 调用失败**
   - 风险：music-service 调用 user-service 获取 Cookie 失败
   - 缓解：实现 Fallback 降级逻辑，优先从 Redis 读取

2. **服务启动顺序**
   - 风险：依赖服务未启动导致调用失败
   - 缓解：配置 Hystrix 重试机制，Eureka 服务发现

3. **性能影响**
   - 风险：从本地方法调用变为远程调用，延迟增加
   - 缓解：增加 Redis 缓存，减少跨服务调用频率

### 回滚方案

**如果拆分后出现问题：**
1. 停止 user-service 和 music-service
2. 重新启动原 user-music-service
3. Zuul Gateway 路由切回原服务
4. 前端无需任何修改

---

## 预期成果

### 架构优化

✅ **单一职责原则**
- user-service 专注于用户认证
- music-service 专注于音乐功能

✅ **可维护性提升**
- 代码职责清晰，易于理解和修改
- 独立部署，互不影响

✅ **可扩展性增强**
- 可以根据负载独立扩展各个服务
- 用户量大时扩展 user-service，音乐请求多时扩展 music-service

### 兼容性保证

✅ **前端零改动**
- API 路径保持不变
- 响应格式保持不变

✅ **数据零迁移**
- 共享数据库
- 数据表结构不变

✅ **功能零损失**
- 所有原有功能完整保留
- 业务逻辑完全一致

---

## 迁移时间估算

- Phase 1: 创建服务骨架 - 30 分钟
- Phase 2: 迁移 user-service - 1 小时
- Phase 3: 迁移 music-service - 1.5 小时
- Phase 4: 更新配置 - 30 分钟
- Phase 5: 前端验证 - 15 分钟
- Phase 6: 数据库验证 - 15 分钟
- Phase 7: 测试验证 - 1 小时

**总计：约 5 小时**

---

## 总结

本次拆分采用**最小侵入式**设计：
- ✅ 代码逻辑完全保留
- ✅ API 接口完全保留
- ✅ 数据库结构完全保留
- ✅ 前端代码完全保留
- ✅ 字符编码严格控制（UTF-8）

通过 Spring Cloud 的服务发现和 Feign Client，实现了**透明的服务拆分**，确保原有功能在新架构下无损运行。
