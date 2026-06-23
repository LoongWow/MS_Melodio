# 微服务启动指南

## 问题修复

✅ **已修复的编译错误：**
- 移除了 `CaptchaLoginService` 中对 `MusicApiService` 的依赖
- 移除了 `NeteaseCookieService` 中对 `MusicApiService` 的依赖
- 这两个服务现在只依赖 `RestTemplate` 来调用网易云 API

## 前置条件

在启动微服务之前，请确保以下服务正在运行：

1. **MySQL 数据库** (端口 3306)
   - 数据库名：`music_agent`
   - 用户名：`root`
   - 密码：`root123`

2. **Redis 服务** (端口 6379)
   - 密码：`123456`

3. **网易云音乐 API** (端口 3000)
   - 如果使用 NeteaseCloudMusicApi 项目，请先启动它

## 启动顺序

### 1. 启动 Eureka Server (服务注册中心)

```bash
cd backend/eureka-server
mvn spring-boot:run
```

**验证：** 访问 http://localhost:8761，应该看到 Eureka 控制台

---

### 2. 启动 Config Server (配置中心)

```bash
cd backend/config-server
mvn spring-boot:run
```

**验证：** 
- Config Server 在端口 8888 启动
- 日志中应该显示加载了配置文件

---

### 3. 启动 user-service (用户服务)

```bash
cd backend/user-service
mvn spring-boot:run
```

**验证：**
- 服务在端口 8082 启动
- 访问 http://localhost:8761，应该看到 `USER-SERVICE` 已注册
- 日志中应该显示：
  ```
  Tomcat started on port(s): 8082
  Registered instance USER-SERVICE/...
  ```

**如果启动失败：**
- 检查 MySQL 是否启动
- 检查 Config Server 是否正常运行
- 查看日志中的具体错误信息

---

### 4. 启动 music-service (音乐服务)

```bash
cd backend/music-service
mvn spring-boot:run
```

**验证：**
- 服务在端口 8083 启动
- 访问 http://localhost:8761，应该看到 `MUSIC-SERVICE` 已注册
- 日志中应该显示：
  ```
  Tomcat started on port(s): 8083
  Registered instance MUSIC-SERVICE/...
  ```

**如果启动失败：**
- 检查 user-service 是否正常运行（music-service 依赖它）
- 检查 Feign Client 配置是否正确
- 查看日志中的具体错误信息

---

### 5. 启动 Zuul Gateway (API 网关)

```bash
cd backend/zuul-gateway
mvn spring-boot:run
```

**验证：**
- 网关在端口 8080 启动
- 访问 http://localhost:8761，应该看到 `ZUUL-GATEWAY` 已注册
- 日志中应该显示路由配置已加载

---

### 6. (可选) 启动 AI Service

```bash
cd backend/ai-service
mvn spring-boot:run
```

---

## 服务端口总览

| 服务名称 | 端口 | 说明 |
|---------|------|------|
| Eureka Server | 8761 | 服务注册中心 |
| Config Server | 8888 | 配置中心 |
| user-service | 8082 | 用户认证服务 |
| music-service | 8083 | 音乐功能服务 |
| Zuul Gateway | 8080 | API 网关（前端访问入口） |
| ai-service | (配置中指定) | AI 服务 |

---

## API 路由规则

### 用户相关 API (通过 Zuul 路由到 user-service)

- `POST /api/user/login` - 账号密码登录
- `POST /api/user/register` - 用户注册
- `POST /api/user/netease-login` - 网易云登录
- `POST /api/user/send-captcha` - 发送验证码
- `POST /api/user/captcha-login` - 验证码登录
- `GET /api/user/qr-key` - 生成二维码 Key
- `GET /api/user/qr-create` - 创建二维码图片
- `GET /api/user/qr-check` - 检查二维码状态
- `POST /api/user/qr-login` - 完成二维码登录
- `GET /api/user/cookie-status` - 验证 Cookie

### 音乐相关 API (通过 Zuul 路由到 music-service)

- `GET /api/music/search` - 搜索歌曲
- `GET /api/music/play-url` - 获取播放链接
- `GET /api/music/lyric/new` - 获取歌词
- `POST /api/music/play-history` - 记录播放历史
- `GET /api/music/play-history` - 获取播放历史
- `DELETE /api/music/play-history` - 清空播放历史
- `GET /api/music/user-preference` - 获取用户偏好

---

## 测试验证

### 1. 测试用户登录功能

```bash
# 测试账号密码登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "account": "testuser",
    "password": "password123",
    "cookie": "MUSIC_U=your_cookie_here"
  }'
```

### 2. 测试音乐搜索功能

```bash
# 测试搜索歌曲
curl "http://localhost:8080/api/music/search?keywords=周杰伦&userId=123456"
```

### 3. 测试服务间通信

music-service 会通过 Feign Client 调用 user-service 获取 Cookie：

```bash
# 测试获取播放链接（会触发 Feign 调用）
curl "http://localhost:8080/api/music/play-url?songId=123456&userId=你的用户ID"
```

---

## 常见问题排查

### 问题 1: user-service 启动失败，提示找不到符号

**原因：** `CaptchaLoginService` 或 `NeteaseCookieService` 中还引用了 `MusicApiService`

**解决方案：** 已修复，这两个服务现在只依赖 `RestTemplate`

---

### 问题 2: music-service 启动失败，提示找不到 UserCookieFeignClient

**检查：**
1. 确认 `UserCookieFeignClient.java` 是否存在于 `backend/music-service/src/main/java/org/example/client/`
2. 确认 `@EnableFeignClients` 注解在主类上
3. 确认 user-service 已经启动并注册到 Eureka

---

### 问题 3: Feign 调用失败，提示 Connection refused

**可能原因：**
1. user-service 未启动
2. Eureka 服务发现延迟（等待 30 秒）
3. 防火墙阻止了端口访问

**解决方案：**
1. 确认 user-service 在 Eureka 控制台中显示为 UP 状态
2. 检查 music-service 日志，查看 Feign Client 是否正确初始化
3. 检查网络连接和防火墙设置

---

### 问题 4: 数据库连接失败

**检查：**
1. MySQL 是否启动：`systemctl status mysql` (Linux) 或任务管理器 (Windows)
2. 数据库是否存在：`SHOW DATABASES LIKE 'music_agent';`
3. 用户名密码是否正确
4. 端口是否正确 (默认 3306)

---

### 问题 5: Redis 连接失败

**检查：**
1. Redis 是否启动：`redis-cli ping` (应返回 PONG)
2. 密码是否正确 (配置文件中的 `123456`)
3. 端口是否正确 (默认 6379)

**注意：** Redis 不可用时服务仍可启动，会降级到只使用数据库

---

## 前端配置

前端无需任何修改，只需确保 API 基础 URL 指向 Zuul Gateway：

```javascript
// frontend/src/utils/request.js
baseURL: 'http://localhost:8080'
```

---

## 监控和调试

### 查看 Eureka 服务注册情况

访问：http://localhost:8761

应该看到以下服务已注册：
- USER-SERVICE (端口 8082)
- MUSIC-SERVICE (端口 8083)
- ZUUL-GATEWAY (端口 8080)

### 查看服务日志

**user-service 日志关键点：**
- `Tomcat started on port(s): 8082`
- `Registered instance USER-SERVICE`
- `Mapped "{[/api/user/login]}"` (路由映射)

**music-service 日志关键点：**
- `Tomcat started on port(s): 8083`
- `Registered instance MUSIC-SERVICE`
- `Building bean definitions for org.example.client.UserCookieFeignClient` (Feign Client 初始化)

---

## 性能优化建议

1. **Redis 缓存预热**
   - 启动后预加载常用用户的 Cookie 到 Redis

2. **连接池配置**
   - 调整数据库连接池大小 (默认 HikariCP)
   - 调整 Redis 连接池大小

3. **Feign 超时配置**
   - 根据实际情况调整 Feign Client 超时时间
   - 在 `music-service.yml` 中配置：
     ```yaml
     feign:
       client:
         config:
           default:
             connectTimeout: 5000
             readTimeout: 10000
     ```

4. **Hystrix 降级优化**
   - 优化 Fallback 逻辑
   - 配置合理的超时时间

---

## 下一步

启动所有服务后：

1. ✅ 访问 http://localhost:8761 验证服务注册
2. ✅ 启动前端项目
3. ✅ 测试完整的用户登录流程
4. ✅ 测试完整的音乐播放流程
5. ✅ 测试播放历史记录功能

祝您启动顺利！🚀
