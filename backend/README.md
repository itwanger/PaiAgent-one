# PaiAgent Backend

AI Agent 流图执行面板后端服务

## 技术栈

- Java 21
- Spring Boot 3.4.1
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Maven 3.8+

## 快速开始

### 1. 数据库初始化

确保 MySQL 已安装并启动,然后执行以下命令创建数据库和表:

```bash
mysql -u root -p < src/main/resources/schema.sql
```

或手动执行 `schema.sql` 中的 SQL 语句。

### 2. 配置数据库连接

修改 `src/main/resources/application.yml` 中的数据库配置:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/paiagent?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 3. 运行项目

```bash
./mvnw spring-boot:run
```

或使用 IDE 运行 `PaiAgentApplication.java`

### 4. 访问 API 文档

启动成功后,访问: http://localhost:8080/swagger-ui.html

## 默认账户

- 用户名: admin
- 密码: 123

## API 接口

### 认证接口

- POST /api/auth/login - 用户登录
- POST /api/auth/logout - 用户登出
- GET /api/auth/current - 获取当前用户信息

## 项目结构

```
src/main/java/com/paiagent/
├── common/           # 通用类
├── config/           # 配置类
├── controller/       # 控制器
├── dto/              # 数据传输对象
├── entity/           # 实体类
├── interceptor/      # 拦截器
├── mapper/           # MyBatis Mapper
└── service/          # 服务层
```
