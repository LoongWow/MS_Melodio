# 微服务端口分配表

## 当前端口配置

| 服务名称 | 端口 | 说明 | 状态 |
|---------|------|------|------|
| Eureka Server | 8761 | 服务注册中心 | ✅ 正常 |
| Config Server | 8888 | 配置中心 | ✅ 正常 |
| Zuul Gateway | 8080 | API 网关（前端访问入口） | ✅ 正常 |
| user-service | 8082 | 用户认证服务 | ✅ 已拆分 |
| music-service | 8083 | 音乐功能服务 | ✅ 已拆分 |
| ai-service | 8084 | AI 智能服务 | ✅ 已修复 |
| user-music-service (原) | 8081 | 原单体服务（已拆分） | ⚠️ 保留备份 |

## 外部依赖服务

| 服务名称 | 端口 | 说明 |
|---------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存服务 |
| 网易云音乐 API | 3000 | 第三方音乐 API |

---

## 端口冲突解决

### 问题描述
ai-service 原配置使用端口 8082，与新拆分的 user-service 冲突。

### 解决方案
✅ 已将 ai-service 端口修改为 8084

### 配置文件位置
- `backend/config-server/src/main/resources/config-repo/ai-service.yml`
- `server.port: 8084`

---

## 启动顺序（更新）

按以下顺序启动所有微服务：

```bash
# 1. Eureka Server (8761)
cd backend/eureka-server && mvn spring-boot:run

# 2. Config Server (8888)
cd backend/config-server && mvn spring-boot:run

# 3. user-service (8082)
cd backend/user-service && mvn spring-boot:run

# 4. music-service (8083)
cd backend/music-service && mvn spring-boot:run

# 5. ai-service (8084)
cd backend/ai-service && mvn spring-boot:run

# 6. Zuul Gateway (8080)
cd backend/zuul-gateway && mvn spring-boot:run
```

---

## API 路由映射（通过 Zuul Gateway）

### 用户服务 API
```
http://localhost:8080/api/user/*  → user-service (8082)
```

### 音乐服务 API
```
http://localhost:8080/api/music/*  → music-service (8083)
```

### AI 服务 API
```
http://localhost:8080/api/music/chat        → ai-service (8084)
http://localhost:8080/api/music/greeting    → ai-service (8084)
http://localhost:8080/api/music/genre       → ai-service (8084)
http://localhost:8080/api/music/genres/batch → ai-service (8084)
http://localhost:8080/api/music/knowledge/** → ai-service (8084)
http://localhost:8080/api/ai/**             → ai-service (8084)
```

---

## 验证服务启动

### 1. 检查 Eureka 控制台
访问：http://localhost:8761

应该看到以下服务已注册：
- USER-SERVICE (8082)
- MUSIC-SERVICE (8083)
- AI-SERVICE (8084)
- ZUUL-GATEWAY (8080)

### 2. 检查端口占用
```bash
# Windows
netstat -ano | findstr "8080 8082 8083 8084 8761 8888"

# Linux/Mac
netstat -tuln | grep -E "8080|8082|8083|8084|8761|8888"
```

### 3. 测试服务健康
```bash
# 测试用户服务
curl http://localhost:8082/actuator/health

# 测试音乐服务
curl http://localhost:8083/actuator/health

# 测试 AI 服务
curl http://localhost:8084/actuator/health
```

---

## 架构图

```
                    ┌─────────────────┐
                    │  Frontend (Vue) │
                    │   Port: 5173    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Zuul Gateway   │
                    │   Port: 8080    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │user-service  │ │music-service │ │  ai-service  │
      │  Port: 8082  │ │  Port: 8083  │ │  Port: 8084  │
      └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
             │                │                │
             │     Feign ──────┘                │
             │     Client                       │
             │                                  │
             └──────────────┬───────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
                ▼                       ▼
        ┌──────────────┐        ┌──────────────┐
        │    MySQL     │        │    Redis     │
        │  Port: 3306  │        │  Port: 6379  │
        └──────────────┘        └──────────────┘
```

---

## 服务依赖关系

### music-service 依赖
- ✅ user-service (通过 Feign Client 获取 Cookie)
- ✅ MySQL (存储播放历史、用户偏好)
- ✅ Redis (缓存推荐数据)
- ✅ 网易云音乐 API (搜索、播放链接)

### user-service 依赖
- ✅ MySQL (存储用户账户、Cookie)
- ✅ Redis (缓存认证信息)
- ✅ 网易云音乐 API (网易云登录)

### ai-service 依赖
- ✅ MySQL (存储对话历史、知识库)
- ✅ Redis (缓存对话上下文)
- ✅ DeepSeek API (AI 对话)

---

## 下一步

现在所有端口冲突已解决，您可以：

1. ✅ 重启 Config Server（如果已启动）
2. ✅ 启动 ai-service（现在使用端口 8084）
3. ✅ 验证所有服务在 Eureka 中注册成功
4. ✅ 启动前端进行完整功能测试

祝您启动顺利！🚀
