# IoT OneNET Refactor

基于 Spring Boot 3 和 Java 17 的物联网平台后端，用于接入中国移动 OneNET、消费 Pulsar 设备消息，并提供设备状态、LED 控制、用户登录、平台日志、AI 检测结果和视频流信息接口。

## 项目概览

- OneNET 设备属性查询与下发
- Pulsar 消息消费、解析与状态落内存
- 登录、注册、会话检查、退出登录
- 平台日志查询与摘要统计
- AI 检测结果接收、最新结果和历史记录查询
- 视频流配置查询，配合前端控制台页面展示

## 目录结构

仓库根目录下实际代码位于：

```text
iot-onenet-refact-main/
└─ iot-onenet-refactor/
   └─ iot-onenet-refactor/
```

其中：

- `src/main/java`：Java 源码
- `src/main/resources/application.yml`：Spring Boot 配置
- `src/main/resources/static`：静态页面
- `logs/platform-events.jsonl`：平台日志文件

## 技术栈

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- Pulsar Client 3.2.4
- MySQL Connector/J
- Fastjson2
- Netty

## 运行要求

- JDK 17+
- Maven 3.9+
- 可访问 OneNET OpenAPI
- 可访问 OneNET Pulsar 服务

## 核心配置

项目主配置文件是 `src/main/resources/application.yml`。生产环境不要把敏感信息直接写死在仓库里，建议通过环境变量覆盖。

常用配置项：

- `server.port`：服务端口，当前默认 `8085`
- `ONENET_PRODUCT_ID`
- `ONENET_AUTHORIZATION`
- `ONENET_PULSAR_ACCESS_ID`
- `ONENET_PULSAR_SECRET_KEY`
- `ONENET_PULSAR_SUBSCRIPTION_NAME`
- `APP_LOGIN_USERNAME`
- `APP_LOGIN_PASSWORD`
- `APP_USER_STORE_MODE`：`memory` 或 `mysql`
- `APP_USER_STORE_JDBC_URL`
- `APP_USER_STORE_USERNAME`
- `APP_USER_STORE_PASSWORD`
- `APP_LOGS_FILE`

示例：

```powershell
$env:ONENET_PRODUCT_ID="your-product-id"
$env:ONENET_AUTHORIZATION="your-authorization"
$env:ONENET_PULSAR_ACCESS_ID="your-access-id"
$env:ONENET_PULSAR_SECRET_KEY="your-secret-key"
$env:ONENET_PULSAR_SUBSCRIPTION_NAME="your-subscription"
$env:APP_LOGIN_USERNAME="admin"
$env:APP_LOGIN_PASSWORD="change-me"
```

如果启用 MySQL 用户存储，还需要：

```powershell
$env:APP_USER_STORE_MODE="mysql"
$env:APP_USER_STORE_JDBC_URL="jdbc:mysql://127.0.0.1:3306/iot_platform?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$env:APP_USER_STORE_USERNAME="db_user"
$env:APP_USER_STORE_PASSWORD="db_password"
```

## 本地启动

进入实际 Maven 模块目录：

```bash
cd iot-onenet-refactor/iot-onenet-refactor
```

开发模式运行：

```bash
mvn spring-boot:run
```

打包运行：

```bash
mvn clean package -DskipTests
java -jar target/iot-onenet-refactor-1.0.0.jar
```

启动后默认访问：

- 登录页：`http://127.0.0.1:8085/login.html`
- 控制台首页：`http://127.0.0.1:8085/console/index.html`
- 健康检查：`GET /api/health`

## 主要接口

认证相关：

- `POST /api/login`
- `POST /api/register`
- `GET /api/check-login`
- `POST /api/logout`

设备相关：

- `GET /api/status`
- `POST /api/ops/led`
- `GET /api/health`

平台日志：

- `GET /api/logs`
- `GET /api/logs/summary`

视频流：

- `GET /api/video/streams`
- `GET /api/video/streams/{streamId}`

AI 检测：

- `POST /api/edge/ai-detections`
- `GET /api/edge/ai-detections/latest`
- `GET /api/edge/ai-detections/history?limit=20`

## 前端页面

静态页面位于 `src/main/resources/static`：

- `login.html`
- `register.html`
- `index.html`
- `preview.html`
- `console/index.html`
- `console/logs.html`
- `console/video.html`
- `console/ai.html`

## Linux 部署

1. 安装 JDK 17 和 Maven。
2. 拉取代码并进入模块目录。
3. 通过环境变量或外部配置文件注入 OneNET、Pulsar、登录账号等配置。
4. 执行打包：

```bash
mvn clean package -DskipTests
```

5. 后台启动：

```bash
nohup java -jar target/iot-onenet-refactor-1.0.0.jar > app.log 2>&1 &
```

6. 检查服务：

```bash
curl http://127.0.0.1:8085/api/health
```

查看进程：

```bash
ps -ef | grep iot-onenet-refactor
```

停止进程：

```bash
pkill -f 'iot-onenet-refactor-1.0.0.jar'
```

## 常见问题

- `401/403`：检查 `ONENET_AUTHORIZATION` 与 `ONENET_PRODUCT_ID` 是否匹配。
- Pulsar 无法消费：检查 `access id`、`secret key`、订阅名和网络连通性。
- 登录数据不持久：确认是否已切换到 `APP_USER_STORE_MODE=mysql` 并填写 JDBC 参数。
- 页面能打开但无数据：优先检查 OneNET 消息是否真正进入 Pulsar，以及应用日志中是否有解析异常。

## 安全说明

- `/api/**` 默认要求已登录会话；登录、注册、会话检查、CSRF 令牌和健康检查接口公开。登录成功会轮换 Session ID，退出会使会话失效。
- 写请求启用 CSRF 校验，包括登录、注册、退出和 LED 控制。先同源请求 `GET /api/csrf`，保存响应 Cookie，再把返回的 `token` 放入 `headerName` 指定的请求头。每次登录/退出后重新获取令牌；内置页面已接入 `js/csrf.js`。
- LED 控制、设备状态和其他受保护 API 使用现有 `logged_in_user` 会话属性授权。普通注册用户仍可控制设备，尚无设备级或管理员角色权限；公开部署前应限制注册或增加角色授权。
- 浏览器页面和 API 应同源部署。登录和设备控制已移除任意来源 CORS 注解；其余控制器的开放 CORS 策略仍需结合部署逐步收紧。
- 密钥、OneNET 授权串和数据库口令通过环境变量或服务器秘密配置注入，禁止提交真实值。
