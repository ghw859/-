# ICBC · 灵枢 后端服务

## 项目简介
工行杯金融科技创新大赛参赛作品后端服务，为前端Vue3项目提供RESTful API。

## 技术栈
- Spring Boot 3.2 + Java 21
- MyBatis-Plus（ORM）
- MySQL 8.0（主数据库）
- Redis（缓存/排队/验证码）
- JWT（登录认证）
- WebSocket（叫号推送）
- SpringDoc OpenAPI（Swagger接口文档）
- Hutool（工具库）

## 快速开始

### 1. 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+（或用IDEA自带）

### 2. 配置数据库
```sql
CREATE DATABASE lingmou DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 3. 修改配置
编辑 `src/main/resources/application.yml`：
- 修改 `spring.datasource.password` 为你的MySQL密码
- 修改 `jwt.secret` 为自定义密钥

### 4. 启动项目
```bash
# 方式一：命令行
mvn spring-boot:run

# 方式二：IDEA中运行
打开 LingmouApplication.java → 右键 Run
```

### 5. 验证启动
- 浏览器访问 http://localhost:8080/api/test → 看到 `{"code":0,"msg":"服务启动成功"}`
- 接口文档 http://localhost:8080/swagger

## 项目结构
```
src/main/java/com/icbc/lingmou/
├── controller/        ← 接口层（接收HTTP请求）
├── service/           ← 业务逻辑层
│   └── impl/
├── mapper/            ← 数据库操作层（MyBatis-Plus接口）
├── entity/            ← 数据库实体类
├── dto/               ← 数据传输对象
│   ├── request/       ← 请求参数
│   └── response/      ← 返回结果
├── common/            ← 公共类（统一返回/异常处理/错误码）
├── config/            ← 配置类（跨域/Redis/JWT/MyBatis-Plus/Web）
├── utils/             ← 工具类（JWT/SM3/脱敏）
├── interceptor/       ← 拦截器（登录校验）
└── LingmouApplication.java  ← 启动类
```

## 三人分工

| 人员 | 负责模块 | 接口数量 |
|------|---------|---------|
| A | 用户认证 + 预约管理 + 排队叫号 | 19个 |
| B | 网点管理 + 数据总览看板 | 14个 |
| C | 预填单/审计存证 + AI服务 | 15个 |

## 统一规范

### 返回格式
```json
{
  "code": 0,        // 0=成功，其他=错误码
  "msg": "success", // 提示信息
  "data": {}        // 实际数据
}
```

### 错误码
| 码段 | 模块 |
|------|------|
| 0 | 成功 |
| 1xxx | 通用错误 |
| 11xx | 用户认证 |
| 2xxx | 预约管理 |
| 3xxx | 网点管理 |
| 4xxx | 预填单/审计 |
| 5xxx | AI/系统 |

## Git分支策略
```
main        ← 受保护，只合并通过的代码
dev         ← 日常开发合并
feature/xxx ← 各自功能分支
```
