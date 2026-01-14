# Spring AI 1.1.2 集成方案

## 目标

将Spring AI 1.1.2集成到PaiAgent项目中，重构现有的LLM通信方式，减少重复代码，支持流式输出。

## 用户选择

- 保留动态配置能力（每个节点可独立配置apiKey/apiUrl/model）
- 使用spring-ai-alibaba集成通义千问
- 支持流式输出（Streaming）

---

## 一、Maven依赖配置

**文件**: `backend/pom.xml`

```xml
<properties>
    <spring-ai.version>1.1.2</spring-ai.version>
    <spring-ai-alibaba.version>1.0.0-M6</spring-ai-alibaba.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Spring AI OpenAI (支持OpenAI和DeepSeek) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Spring AI Alibaba (通义千问) -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
        <version>${spring-ai-alibaba.version}</version>
    </dependency>
</dependencies>
```

---

## 二、架构设计

### 分层结构

```
NodeExecutor (保持不变)
       |
AbstractLLMNodeExecutor (新增抽象基类)
       |
ChatClientFactory (新增动态工厂)
       |
Spring AI ChatClient/ChatModel
```

### 核心类职责

| 类名 | 职责 | 路径 |
|------|------|------|
| `ChatClientFactory` | 根据节点配置动态创建ChatClient | `engine/llm/` |
| `PromptTemplateService` | prompt模板变量替换和参数映射 | `engine/llm/` |
| `AbstractLLMNodeExecutor` | LLM节点执行器抽象基类 | `engine/executor/impl/` |

---

## 三、实现步骤

### 步骤1: 新增 PromptTemplateService

**路径**: `backend/src/main/java/com/paiagent/engine/llm/PromptTemplateService.java`

**职责**:
- 从DeepSeekNodeExecutor:46-87迁移参数映射和模板替换逻辑
- 处理`{{variable}}`格式的模板变量
- 支持`input`和`reference`两种参数类型

### 步骤2: 新增 ChatClientFactory

**路径**: `backend/src/main/java/com/paiagent/engine/llm/ChatClientFactory.java`

**核心功能**:
```java
public ChatClient createClient(String nodeType, String apiUrl, String apiKey, 
                               String model, Double temperature) {
    return switch (nodeType) {
        case "openai", "deepseek" -> createOpenAIClient(apiUrl, apiKey, model, temperature);
        case "qwen" -> createDashScopeClient(apiKey, model, temperature);
        default -> throw new IllegalArgumentException("不支持的节点类型: " + nodeType);
    };
}
```

**关键点**:
- OpenAI/DeepSeek使用`OpenAiApi`+`OpenAiChatModel`，支持自定义baseUrl
- 通义千问使用`DashScopeApi`+`DashScopeChatModel`

### 步骤3: 新增 AbstractLLMNodeExecutor

**路径**: `backend/src/main/java/com/paiagent/engine/executor/impl/AbstractLLMNodeExecutor.java`

**核心流程**:
```java
public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input, 
                                   Consumer<ExecutionEvent> progressCallback) {
    // 1. 提取配置
    LLMNodeConfig config = extractConfig(node);
    
    // 2. 处理prompt模板
    String finalPrompt = promptTemplateService.processTemplate(
        config.getPromptTemplate(), config.getInputParams(), input);
    
    // 3. 创建ChatClient
    ChatClient chatClient = chatClientFactory.createClient(
        getNodeType(), config.getApiUrl(), config.getApiKey(), 
        config.getModel(), config.getTemperature());
    
    // 4. 判断是否流式
    String response;
    if (config.isStreaming() && progressCallback != null) {
        response = executeStreaming(chatClient, finalPrompt, node, progressCallback);
    } else {
        response = chatClient.prompt().user(finalPrompt).call().content();
    }
    
    // 5. 构建输出
    return buildOutput(response, config.getOutputParams());
}
```

### 步骤4: 流式输出实现

**在AbstractLLMNodeExecutor中**:
```java
private String executeStreaming(ChatClient chatClient, String prompt, 
                                WorkflowNode node, Consumer<ExecutionEvent> progressCallback) {
    StringBuilder accumulated = new StringBuilder();
    
    chatClient.prompt()
        .user(prompt)
        .stream()
        .content()
        .doOnNext(chunk -> {
            accumulated.append(chunk);
            progressCallback.accept(
                ExecutionEvent.nodeProgress(node.getId(), node.getType(),
                    "生成中...", Map.of("chunk", chunk, "accumulated", accumulated.toString()))
            );
        })
        .blockLast();
    
    return accumulated.toString();
}
```

### 步骤5: 重构现有NodeExecutor

**OpenAINodeExecutor.java**:
```java
@Component
public class OpenAINodeExecutor extends AbstractLLMNodeExecutor {
    @Override
    protected String getNodeType() { return "openai"; }
}
```

**DeepSeekNodeExecutor.java**:
```java
@Component
public class DeepSeekNodeExecutor extends AbstractLLMNodeExecutor {
    @Override
    protected String getNodeType() { return "deepseek"; }
}
```

**QwenNodeExecutor.java**:
```java
@Component
public class QwenNodeExecutor extends AbstractLLMNodeExecutor {
    @Override
    protected String getNodeType() { return "qwen"; }
}
```

---

## 四、文件清单

### 新增文件 (4个)

1. `backend/src/main/java/com/paiagent/engine/llm/ChatClientFactory.java`
2. `backend/src/main/java/com/paiagent/engine/llm/PromptTemplateService.java`
3. `backend/src/main/java/com/paiagent/engine/llm/LLMNodeConfig.java` (配置DTO)
4. `backend/src/main/java/com/paiagent/engine/executor/impl/AbstractLLMNodeExecutor.java`

### 修改文件 (4个)

1. `backend/pom.xml` - 添加Spring AI依赖
2. `backend/src/main/java/com/paiagent/engine/executor/impl/OpenAINodeExecutor.java` - 继承基类
3. `backend/src/main/java/com/paiagent/engine/executor/impl/DeepSeekNodeExecutor.java` - 继承基类
4. `backend/src/main/java/com/paiagent/engine/executor/impl/QwenNodeExecutor.java` - 继承基类

### 无需修改

- `NodeExecutor.java` - 接口保持不变
- `NodeExecutorFactory.java` - 工厂逻辑不变
- `WorkflowEngine.java` - 执行流程不变（已支持progressCallback）
- 前端代码 - API接口和配置格式不变

---

## 五、application.yml配置（可选）

```yaml
spring:
  ai:
    openai:
      # 这些配置仅用于开发测试，实际运行时使用节点配置
      api-key: ${OPENAI_API_KEY:sk-placeholder}
      base-url: https://api.openai.com

# 禁用自动配置，因为我们使用动态创建
paiagent:
  llm:
    default-temperature: 0.7
```

---

## 六、兼容性保证

1. **前端兼容**: 节点配置格式不变（apiUrl、apiKey、model、temperature、prompt）
2. **API兼容**: REST接口不变，响应格式不变
3. **流式兼容**: 复用现有的SSE端点和ExecutionEvent机制
4. **渐进迁移**: 可保留旧实现作为降级方案

---

## 七、验证计划

### 编译验证
```bash
cd backend && ./mvnw clean compile
```

### 单元测试
- 测试PromptTemplateService的模板替换逻辑
- 测试ChatClientFactory创建不同类型Client
- Mock测试AbstractLLMNodeExecutor流程

### 集成测试
1. 创建包含OpenAI/DeepSeek/Qwen节点的工作流
2. 配置真实API Key执行工作流
3. 验证流式输出（通过SSE端点）
4. 检查ExecutionRecord中的nodeResults

### 启动验证
```bash
cd backend && ./mvnw spring-boot:run
# 访问 http://localhost:8080/swagger-ui.html 测试API
```

---

## 八、风险与应对

| 风险 | 应对措施 |
|------|---------|
| Spring AI版本兼容问题 | 保留旧RestTemplate实现作为降级 |
| DashScope SDK版本冲突 | 在pom.xml中排除传递依赖 |
| 首次ChatClient创建延迟 | 后续可增加连接池缓存 |
