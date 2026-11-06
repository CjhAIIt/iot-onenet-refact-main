# IoT OneNET 重构版

本项目是一个基于 Spring Boot 3（Java 17）的后端服务，用于对接 OneNET 物联网平台的设备属性接口，并通过 Pulsar 消费设备上报消息，完成状态维护与下行控制（如 LED 开关）。

- 核心能力：登录/注册、设备状态查询、LED 下发、Pulsar 消费、消息解密与解析、OneNET OpenAPI 调用、`set_reply` 回填
- 技术栈：Spring Boot 3、Spring Security（会话鉴权与 CSRF）、Jackson、Pulsar Client、Fastjson2、Netty

**代码入口与关键模块**
- 应用入口：[IotOnenetApplication](src/main/java/com/aurora/iotonenet/IotOnenetApplication.java)
- Pulsar 启动引导：[PulsarConsumerBootstrap](src/main/java/com/aurora/iotonenet/bootstrap/PulsarConsumerBootstrap.java)
- Pulsar 消费主流程：[IoTPulsarConsumer](src/main/java/com/aurora/iotonenet/infrastructure/pulsar/consumer/IoTPulsarConsumer.java)
- OneNET API 调用：[OneNetApiService](src/main/java/com/aurora/iotonenet/infrastructure/onenet/OneNetApiService.java)
- 配置绑定：[OneNetProperties](src/main/java/com/aurora/iotonenet/config/OneNetProperties.java) 与 [OneNetPropertiesConfig](src/main/java/com/aurora/iotonenet/config/OneNetPropertiesConfig.java)
- 安全策略：[SecurityConfig](src/main/java/com/aurora/iotonenet/config/SecurityConfig.java)

## 环境要求
- JDK 17+
- Maven 3.9+
- 可访问 OneNET OpenAPI 与 Pulsar 服务的网络
- Windows/macOS/Linux 均可运行

## 快速开始
- 克隆/下载代码后，按需修改 `src/main/resources/application.yml` 的占位配置（见下文“配置说明”）
- 启动服务

```bash
mvn spring-boot:run
# 或
mvn clean package -DskipTests
java -jar target/iot-onenet-refactor-1.0.0.jar
```

- 默认端口：`8082`，服务启动后会自动尝试打开登录页 `http://127.0.0.1:8082/login.html`（见 IoTPulsarConsumer 的 `tryOpenDashboard`）
- 健康检查：`GET /api/health` 返回 `ok`

## 配置说明
项目配置集中在 `application.yml`，并通过 `@ConfigurationProperties(prefix = "onenet")` 绑定到 [OneNetProperties](src/main/java/com/aurora/iotonenet/config/OneNetProperties.java)。

请务必将密钥、授权信息放在安全的外部配置中，避免提交到代码仓库。可通过环境变量、命令行参数或外部 `application.yml` 覆盖。

示例（占位符，请替换为你自己的值）：

```yaml
server:
  port: 8082

onenet:
  product-id: YOUR_PRODUCT_ID
  authorization: YOUR_ONENET_AUTH_HEADER
  property-set-url: https://iot-api.heclouds.com/thingmodel/set-device-property
  property-get-url: https://iot-api.heclouds.com/thingmodel/query-device-property
  timeout-ms: 10000

  pulsar:
    brokerUrl: pulsar+ssl://iot-north-mq.heclouds.com:6651/
    access-id: YOUR_PULSAR_ACCESS_ID
    secret-key: YOUR_PULSAR_SECRET_KEY
    subscription-name: YOUR_SUBSCRIPTION_NAME
    autoOpenDashboard: true

app:
  login:
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
```

覆盖方式示例：
- 通过 JVM 启动参数：`java -jar app.jar --onenet.product-id=xxx --onenet.pulsar.access-id=xxx`
- 通过环境变量：`ONENET_PRODUCT_ID=xxx` 等（需配合 Spring Boot 外部化配置）

## 接口与示例
- 登录/注册相关（允许跨域）
  - `POST /api/login`，[LoginController](src/main/java/com/aurora/iotonenet/api/controller/LoginController.java)
  - `POST /api/register`
  - `GET /api/check-login`
  - `POST /api/logout`
- 设备状态查询
  - `GET /api/status`，[DeviceApiController](src/main/java/com/aurora/iotonenet/api/controller/DeviceApiController.java)
- LED 下发
  - `POST /api/ops/led`，请求体为 `LedOperationRequest`，返回 `LedOperationResponse`
    - DTO 参考：`src/main/java/com/aurora/iotonenet/api/dto` 目录
- 健康检查
  - `GET /api/health`

示例请求：

```bash
# 查询设备状态
curl -s http://127.0.0.1:8082/api/status

# 下发 LED 开关
curl -s -X POST http://127.0.0.1:8082/api/ops/led \
  -H "Content-Type: application/json" \
  -d '{"deviceName":"YourDeviceName","led":true}'
```

## 消息处理与工作流
- 服务启动后，Pulsar 消费者由 [PulsarConsumerBootstrap](src/main/java/com/aurora/iotonenet/bootstrap/PulsarConsumerBootstrap.java) 初始化，线程名 `pulsar-consumer-thread`
- 主流程见 [IoTPulsarConsumer](src/main/java/com/aurora/iotonenet/infrastructure/pulsar/consumer/IoTPulsarConsumer.java)：
  - 校验 `accessId / secretKey / subscriptionName`
  - 创建消费者并监听消息
  - 解密：使用 [AESBase64Utils](src/main/java/com/aurora/iotonenet/infrastructure/pulsar/auth/AESBase64Utils.java) 从 `iotMessage.data` 中解密原始报文
  - 解析：使用 [OriginalMessageParser](src/main/java/com/aurora/iotonenet/infrastructure/pulsar/parser/OriginalMessageParser.java) 判定类型为 `PROPERTY_REPORT` 或 `SET_REPLY`
  - 处理：
    - `PROPERTY_REPORT` → [DeviceMessageHandler](src/main/java/com/aurora/iotonenet/infrastructure/pulsar/handler/DeviceMessageHandler.java) 更新设备状态
    - `SET_REPLY` → 回填请求结果（根据是否含 `deviceId` 分支）

- 下发 LED 属性时，[OneNetApiService](src/main/java/com/aurora/iotonenet/infrastructure/onenet/OneNetApiService.java) 调用 OneNET `thingmodel/set-device-property` 并解析响应，提取操作 `id` 作为后续 `set_reply` 对应标识

## 目录结构
```text
com.aurora.iotonenet
├── api
│   ├── controller
│   ├── dto
│   └── exception
├── application
│   └── service
├── bootstrap
├── config
└── infrastructure
    ├── onenet
    └── pulsar
        ├── auth
        ├── client
        ├── consumer
        ├── handler
        ├── model
        └── parser
```
静态页面位于 `src/main/resources/static`，包含 `login.html / register.html / index.html / preview.html` 等。

## 构建与运行
- 本地开发：`mvn spring-boot:run`
- 打包：`mvn clean package -DskipTests`，生成 `target/iot-onenet-refactor-1.0.0.jar`
- 生产运行建议：
  - 使用外部化配置文件或环境变量注入密钥
  - 通过 `--server.port=` 指定端口
  - 结合进程管理工具（如 systemd / Windows 服务）

## 安全与跨域

- `/api/**` 默认要求已登录会话；登录、注册、会话检查、CSRF 令牌和健康检查接口公开。登录成功会轮换 Session ID，退出会使会话失效。
- 写请求启用 CSRF 校验，包括登录、注册、退出和 LED 控制。先同源请求 `GET /api/csrf`，保存响应 Cookie，再把返回的 `token` 放入 `headerName` 指定的请求头。每次登录/退出后重新获取令牌；内置页面已接入 `js/csrf.js`。
- LED 控制、设备状态和其他受保护 API 使用现有 `logged_in_user` 会话属性授权。普通注册用户仍可控制设备，尚无设备级或管理员角色权限；公开部署前应限制注册或增加角色授权。
- 浏览器页面和 API 应同源部署。登录和设备控制已移除任意来源 CORS 注解；其余控制器的开放 CORS 策略仍需结合部署逐步收紧。
- 密钥、OneNET 授权串和数据库口令通过环境变量或服务器秘密配置注入，禁止提交真实值。

## 常见问题
- 浏览器未自动打开
  - 检查 `onenet.pulsar.autoOpenDashboard` 或直接访问 `http://127.0.0.1:8082/login.html`
- Pulsar 鉴权失败
  - 校验 `accessId / secretKey / subscriptionName` 是否正确、是否有订阅权限
- OneNET API 401/403
  - 检查 `authorization` 头与 `product-id` 是否匹配设备
- 收不到设备上报
  - 确认设备已接入 OneNET 并将上报消息投递到相应 Pulsar 主题/订阅

## 许可证
未设置许可证。如需开源或分发，请根据实际需求添加合适的 LICENSE。


## 安全回归测试

```bash
mvn -Dfile.encoding=UTF-8 -Dtest=ApiSecurityTest test
```

使用内存用户存储和模拟设备服务，不连接 OneNET、Pulsar 或真实硬件。
