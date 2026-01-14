# PaiAgent 使用指南

## 快速开始

### 1. 环境准备

**必需软件**:
- JDK 21
- Node.js 18+
- MySQL 8.0
- Maven 3.8+

### 2. 数据库初始化

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE paiagent DEFAULT CHARACTER SET utf8mb4;
USE paiagent;

# 2. 导入表结构
SOURCE backend/src/main/resources/schema.sql;
```

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

访问: http://localhost:8080

API 文档: http://localhost:8080/swagger-ui/index.html

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问: http://localhost:5173

## 功能使用

### 登录系统

- 用户名: `admin`
- 密码: `123`

### 创建工作流

1. **添加节点**
   - 从左侧节点面板拖拽节点到画布
   - 支持的节点: 输入节点、大模型节点、TTS 节点、输出节点

2. **连接节点**
   - 点击源节点的输出端口
   - 拖拽到目标节点的输入端口
   - 形成数据流

3. **配置节点**
   - 点击节点
   - 在右侧配置面板设置参数
   - 例如: 提示词、温度、音色等

4. **保存工作流**
   - 输入工作流名称
   - 点击"保存"按钮

### 调试工作流

1. 点击顶部"调试"按钮
2. 在右侧调试抽屉中输入测试文本
3. 点击"执行工作流"
4. 查看执行结果:
   - 执行状态和进度
   - 每个节点的输入输出
   - 执行日志
   - 最终输出

### 播放音频

如果工作流包含 TTS 节点,执行成功后会在"最终输出"区域显示音频播放器:

- 点击播放按钮播放音频
- 拖拽进度条跳转播放位置
- 点击下载按钮保存音频文件

## 典型工作流示例

### AI 播客生成

**工作流结构**:
```
输入节点 → OpenAI节点 → TTS节点 → 输出节点
```

**节点配置**:

1. **输入节点**: 默认配置

2. **OpenAI 节点**:
   ```
   提示词: 请用通俗易懂的语言介绍: {{input}}
   温度: 0.7
   最大Token: 500
   ```

3. **TTS 节点**:
   ```
   音色: female
   语速: 1.0
   音量: 80
   提供商: simulation (模拟模式)
   ```

4. **输出节点**: 默认配置

**测试执行**:
```
输入: 人工智能的未来发展
输出: 音频文件 (约60秒)
```

## 节点类型说明

### 大模型节点

| 节点 | 说明 | 配置参数 |
|------|------|----------|
| OpenAI | GPT 模型 | API Key, 提示词, 温度 |
| DeepSeek | DeepSeek 模型 | API Key, 提示词, 温度 |
| 通义千问 | 阿里云大模型 | API Key, 提示词, 温度 |

**注意**: 当前为模拟实现,配置真实 API Key 后可调用真实服务

### 工具节点

| 节点 | 说明 | 配置参数 |
|------|------|----------|
| TTS | 文本转语音 | API Key, 音色, 语速, 音量, 提供商 |

**TTS 提供商**:
- `simulation`: 模拟模式 (默认)
- `azure`: Azure Cognitive Services (开发中)
- `aliyun`: 阿里云语音合成 (开发中)

## 常见问题

### 1. 工作流执行失败?

检查以下几点:
- 所有节点是否正确连接
- 节点配置是否完整
- 输入数据是否有效

### 2. 音频无法播放?

- 确认工作流执行成功
- 检查浏览器控制台是否有错误
- 确认音频文件 URL 可访问

### 3. 如何配置真实的大模型 API?

1. 获取 API Key (OpenAI, DeepSeek, 阿里云等)
2. 在节点配置中填入 API Key
3. 后端需要实现真实的 API 调用逻辑

### 4. 如何添加新的节点类型?

参考 [扩展开发文档](#扩展开发)

## 扩展开发

### 新增节点类型

**后端实现**:

1. 创建节点执行器:
```java
@Component
public class MyNodeExecutor implements NodeExecutor {
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) {
        // 执行逻辑
        return output;
    }
    
    @Override
    public String getSupportedNodeType() {
        return "my_node";
    }
}
```

2. 添加节点定义 (SQL):
```sql
INSERT INTO node_definition (node_type, display_name, category, icon) 
VALUES ('my_node', '自定义节点', 'TOOL', '🔧');
```

**前端自动加载** - 无需修改代码,节点会自动显示在节点面板中

## 技术支持

- 项目文档: [README.md](./README.md)
- 设计文档: [.qoder/quests/ai-agent-flow-builder.md](.qoder/quests/ai-agent-flow-builder.md)
- 完成报告: [PROJECT_COMPLETION_REPORT.md](./PROJECT_COMPLETION_REPORT.md)
- API 文档: http://localhost:8080/swagger-ui/index.html

## 附录

### 目录结构

```
PaiAgent-one/
├── backend/              # Spring Boot 后端
│   ├── src/main/java/   # Java 源码
│   ├── src/main/resources/  # 配置文件
│   └── pom.xml          # Maven 配置
├── frontend/            # React 前端
│   ├── src/             # TypeScript 源码
│   ├── package.json     # npm 配置
│   └── vite.config.ts   # Vite 配置
├── audio_output/        # 音频文件存储目录
├── README.md            # 项目说明
└── PROJECT_COMPLETION_REPORT.md  # 完成报告
```

### 端口配置

- 后端: 8080
- 前端: 5173
- MySQL: 3306

### 默认账户

- 用户名: admin
- 密码: 123

---

**最后更新**: 2025-11-23  
**版本**: v1.0
