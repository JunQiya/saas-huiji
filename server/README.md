# 星河·会记 SaaS 后端

企业级美业/门店 SaaS「星河·会记」后端，技术栈 Java 17 + Spring Boot 3.2 + Spring Security + JPA + MySQL/H2。
严格遵循 `docs/API.md` 契约，统一响应格式、JWT 鉴权、多租户数据隔离、RBAC 角色控制。

## 技术栈

- Java 17、Spring Boot 3.2.12、Spring Security、Spring Data JPA、Jakarta Validation
- JWT（jjwt 0.12.x）、Lombok、MySQL Connector、H2（dev 内存库）
- 多租户：token 内含 `tenantId`，数据按租户隔离
- RBAC：`@PreAllowed` 注解校验 `TENANT_ADMIN/STORE_MANAGER/STAFF/CASHIER`

## 目录结构

```
server/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/huiji/
    │   ├── HuijiApplication.java
    │   ├── common/        # Result/PageData/ErrorCode/BizException/GlobalExceptionHandler
    │   ├── security/      # JwtUtil/MemberTokenUtil/JwtAuthFilter/SecurityConfig/LoginUserHolder/PreAllowed/RoleAspect
    │   ├── entity/        # 领域实体(BaseEntity + 13 个实体) + converter/
    │   ├── repository/    # JPA Repository(13 个)
    │   ├── dto/           # 请求/响应 DTO
    │   ├── service/       # 业务服务(10 个) + AuditHelper
    │   ├── controller/    # 控制器(10 个, 含 H5)
    │   └── init/          # DataInitializer 演示数据
    └── resources/
        ├── application.yml       # 公共配置(prod MySQL)
        └── application-dev.yml   # dev: H2 内存库 + 自动种子数据
```

## 环境要求

- **JDK 17（必需）**：Lombok 1.18.38 不兼容 JDK 26 的 javac，必须用 JDK 17 运行 Maven。
  查看本机 JDK：`/usr/libexec/java_home -V`
  若默认 JDK 非 17，构建/启动前执行：
  `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`
- Maven 3.6+
- dev 模式无需安装 MySQL，使用 H2 内存库（MySQL 兼容模式）
- prod 模式需 MySQL 5.7+/8.x，库 `huiji`，连接配置见 `application.yml`

## 启动

### dev（零外部依赖，H2 内存库 + 自动种子数据）

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
cd server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包后运行：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
cd server
mvn -DskipTests package
$JAVA_HOME/bin/java -jar target/huiji-server-1.0.0.jar --spring.profiles.active=dev
```

### prod（MySQL）

1. 准备 MySQL：`CREATE DATABASE huiji DEFAULT CHARSET utf8mb4;`
2. 修改 `application.yml` 中 `spring.datasource` 的 url/username/password
3. 使用 `prod` profile 启动（关闭种子数据）：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

或打包后运行：

```bash
mvn -DskipTests package
$JAVA_HOME/bin/java -jar target/huiji-server-1.0.0.jar --spring.profiles.active=prod
```

## 默认账号

| 端 | 账号 | 密码 | 角色 |
|---|---|---|---|
| 后台 | `admin` | `123456` | TENANT_ADMIN（租户管理员） |
| 后台 | `wang mgr` | `123456` | STORE_MANAGER（店长） |
| 后台 | `li staff` | `123456` | STAFF（员工） |
| 后台 | `zhao cash` | `123456` | CASHIER（收银） |
| H5 | 手机号 `13800000001`（张伟） | 验证码 `8888` | 会员 |

- 默认租户：`星河·会记演示`（tenantId=1）
- 门店：`星河·会记 旗舰店`、`星河·会记 静安店`
- 已内置 10 个示例会员、4 张优惠券、3 个营销活动，以及跨 60 天的资金流水，看板数据可直接展示。

## 端口与 CORS

- 服务端口：`8081`
- H2 控制台（dev）：`http://localhost:8081/h2-console`（JDBC URL `jdbc:h2:mem:huiji`，用户 `sa`，无密码）
- CORS 允许来源：
  - 后台管理端 `http://localhost:5173`
  - H5 会员端 `http://localhost:5174`
- 鉴权：除 `POST /api/auth/login`、`/api/h5/**` 外，请求头需带 `Authorization: Bearer <token>`

## 统一响应格式

```jsonc
// 成功
{ "ok": true, "data": ... }
// 分页
{ "ok": true, "data": { "list": [], "total": 0, "page": 1, "size": 20 } }
// 失败
{ "ok": false, "message": "...", "code": "..." }
// 401
{ "ok": false, "code": "SESSION_EXPIRED", "message": "登录已过期" }
```

金额一律以「分」为单位（整数）传输。

## 错误码

`SESSION_EXPIRED`(401) / `FORBIDDEN`(403) / `NOT_FOUND`(404) / `VALIDATION`(422) / `CONFLICT`(409) / `BIZ_ERROR`(400) / `SERVER_ERROR`(500)
