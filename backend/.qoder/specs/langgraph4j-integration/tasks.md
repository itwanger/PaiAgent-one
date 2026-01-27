# LangGraph4j 集成 - 实施任务清单

## 📋 任务概览

**项目名称**: PaiAgent LangGraph4j 集成  
**总预估时间**: 12-17 天  
**优先级**: P0 (核心功能)  
**负责人**: 待分配

---

## ✅ Phase 1: 基础设施搭建 (2-3 天)

### 任务 1.1: 添加 Maven 依赖

**预估时间**: 30 分钟

**文件**: `pom.xml`

**操作步骤**:
```xml
<!-- 在 <dependencies> 标签内添加以下依赖 -->

<!-- LangGraph4j Core (Java 8+ 兼容版本) -->
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-core-jdk8</artifactId>
    <version>1.1.5</version>
</dependency>

<!-- LangGraph4j Spring AI 集成 -->
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-spring-ai</artifactId>
    <version>1.8.0-beta3</version>
</dependency>
```

**验收标准**:
- [ ] Maven 依赖下载成功
- [ ] 项目编译无错误
- [ ] IDEA/Eclipse 可以正常解析 LangGraph4j 类

---

### 任务 1.2: 创建包结构

**预估时间**: 15 分钟

**操作步骤**:
```bash
# 创建新包
mkdir -p src/main/java/com/paiagent/engine/langgraph
```

**文件结构**:
```
com.paiagent.engine.langgraph/
├── WorkflowState.java           (待创建)
├── GraphBuilder.java            (待创建)
├── NodeAdapter.java             (待创建)
├── StateManager.java            (待创建)
└── LangGraphWorkflowEngine.java (待创建)
```

**验收标准**:
- [ ] 包结构创建成功
- [ ] 包名符合项目规范

---

### 任务 1.3: 创建 WorkflowState 模型类

**预估时间**: 1-2 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/WorkflowState.java`

**实现要点**:
```java
@Data
public class WorkflowState {
    private String currentNodeId;
    private Map<String, Object> globalContext = new HashMap<>();
    private Map<String, NodeOutput> nodeOutputs = new HashMap<>();
    private String status = "RUNNING";
    private String errorMessage;
    private Long startTime;
    private String inputData;
    
    // 核心方法
    public void updateNodeOutput(String nodeId, Map<String, Object> output, String status) { ... }
    public Map<String, Object> getNodeOutput(String nodeId) { ... }
    public Map<String, Object> getPreviousNodeOutput() { ... }
    
    @Data
    public static class NodeOutput {
        private String nodeId;
        private Map<String, Object> output;
        private String status;
        private Long timestamp;
    }
}
```

**参考**: 设计文档 § 4.3

**验收标准**:
- [ ] 类编译通过
- [ ] 包含所有必需字段
- [ ] 实现 updateNodeOutput、getNodeOutput、getPreviousNodeOutput 方法
- [ ] 使用 Lombok @Data 注解

---

### 任务 1.4: 验证 LangGraph4j 依赖加载

**预估时间**: 1 小时

**文件**: `src/test/java/com/paiagent/engine/langgraph/LangGraphBasicTest.java`

**测试代码**:
```java
@SpringBootTest
class LangGraphBasicTest {
    
    @Test
    void testLangGraphDependency() throws Exception {
        // 验证可以创建 StateGraph
        StateGraph<WorkflowState> graph = new StateGraph<>(WorkflowState.class);
        
        // 添加一个简单节点
        graph.addNode("test", state -> {
            state.put("message", "Hello LangGraph");
            return state;
        });
        
        // 编译图
        CompiledGraph<WorkflowState> compiled = graph.compile();
        
        // 验证执行
        WorkflowState result = compiled.invoke(new WorkflowState());
        assertEquals("Hello LangGraph", result.get("message"));
    }
}
```

**验收标准**:
- [ ] 测试代码编译通过
- [ ] 测试执行成功
- [ ] LangGraph4j API 可正常调用

---

## ✅ Phase 2: 引擎抽象层开发 (2 天)

### 任务 2.1: 创建 WorkflowExecutor 接口

**预估时间**: 1 小时

**文件**: `src/main/java/com/paiagent/engine/WorkflowExecutor.java`

**实现要点**:
```java
public interface WorkflowExecutor {
    ExecutionResponse execute(Workflow workflow, String inputData);
    
    ExecutionResponse executeWithCallback(
        Workflow workflow, 
        String inputData, 
        Consumer<ExecutionEvent> eventCallback
    );
    
    default String getEngineType() {
        return "unknown";
    }
}
```

**参考**: 设计文档 § 4.1

**验收标准**:
- [ ] 接口定义完整
- [ ] 方法签名与现有 WorkflowEngine 兼容
- [ ] 包含默认方法 getEngineType()

---

### 任务 2.2: 重构 WorkflowEngine 实现接口

**预估时间**: 30 分钟

**文件**: `src/main/java/com/paiagent/engine/WorkflowEngine.java`

**修改内容**:
```java
@Service
public class WorkflowEngine implements WorkflowExecutor {
    
    // 保持原有代码不变
    
    @Override
    public String getEngineType() {
        return "legacy";
    }
}
```

**验收标准**:
- [ ] WorkflowEngine 实现 WorkflowExecutor 接口
- [ ] 原有方法保持不变
- [ ] getEngineType() 返回 "legacy"
- [ ] 项目编译通过
- [ ] 现有测试用例全部通过

---

### 任务 2.3: 开发 EngineSelector 引擎选择器

**预估时间**: 3-4 小时

**文件**: `src/main/java/com/paiagent/engine/EngineSelector.java`

**实现要点**:
```java
@Component
public class EngineSelector {
    @Autowired private WorkflowEngine legacyEngine;
    @Autowired private LangGraphWorkflowEngine langGraphEngine;
    
    public WorkflowExecutor selectEngine(Workflow workflow) {
        if (shouldUseLangGraph(workflow)) {
            return langGraphEngine;
        }
        return legacyEngine;
    }
    
    private boolean shouldUseLangGraph(Workflow workflow) {
        // 规则 1: 显式配置优先 (metadata.engineType)
        // 规则 2: 自动检测高级节点 (condition, loop, agent, tool)
    }
}
```

**参考**: 设计文档 § 4.2

**验收标准**:
- [ ] 实现 selectEngine() 方法
- [ ] 实现 shouldUseLangGraph() 判断逻辑
- [ ] 支持 metadata.engineType 显式配置
- [ ] 支持自动检测高级节点类型
- [ ] 默认返回旧引擎（向后兼容）
- [ ] 添加日志记录选择结果

---

### 任务 2.4: 修改 ExecutionController 使用选择器

**预估时间**: 2 小时

**文件**: `src/main/java/com/paiagent/controller/ExecutionController.java`

**修改前**:
```java
@Autowired
private WorkflowEngine workflowEngine;

public Result<ExecutionResponse> executeWorkflow(...) {
    ExecutionResponse response = workflowEngine.execute(workflow, request.getInputData());
}
```

**修改后**:
```java
@Autowired
private EngineSelector engineSelector;

public Result<ExecutionResponse> executeWorkflow(...) {
    WorkflowExecutor executor = engineSelector.selectEngine(workflow);
    ExecutionResponse response = executor.execute(workflow, request.getInputData());
}
```

**同时修改**:
- `executeWorkflow()` 方法
- `executeWorkflowStream()` 方法

**验收标准**:
- [ ] 注入 EngineSelector
- [ ] 替换直接调用 workflowEngine 为动态选择
- [ ] 同步和流式执行都使用选择器
- [ ] 编译通过
- [ ] **关键验证**: 手动测试现有工作流，确保执行正常（应该走旧引擎）

---

### 任务 2.5: 编写 EngineSelector 单元测试

**预估时间**: 2 小时

**文件**: `src/test/java/com/paiagent/engine/EngineSelectorTest.java`

**测试用例**:
```java
@SpringBootTest
class EngineSelectorTest {
    
    @Autowired
    private EngineSelector selector;
    
    @Test
    void shouldSelectLegacyEngineByDefault() {
        Workflow workflow = createSimpleWorkflow(); // 无 metadata
        WorkflowExecutor executor = selector.selectEngine(workflow);
        assertEquals("legacy", executor.getEngineType());
    }
    
    @Test
    void shouldSelectLangGraphWhenExplicitlyConfigured() {
        Workflow workflow = createWorkflowWithMetadata("engineType", "langgraph");
        WorkflowExecutor executor = selector.selectEngine(workflow);
        assertEquals("langgraph", executor.getEngineType());
    }
    
    @Test
    void shouldSelectLangGraphWhenContainsAdvancedNode() {
        Workflow workflow = createWorkflowWithConditionNode();
        WorkflowExecutor executor = selector.selectEngine(workflow);
        assertEquals("langgraph", executor.getEngineType());
    }
    
    @Test
    void shouldSelectLegacyWhenExplicitlyConfigured() {
        Workflow workflow = createWorkflowWithMetadata("engineType", "legacy");
        WorkflowExecutor executor = selector.selectEngine(workflow);
        assertEquals("legacy", executor.getEngineType());
    }
}
```

**验收标准**:
- [ ] 至少 4 个测试用例
- [ ] 覆盖默认选择、显式配置、自动检测等场景
- [ ] 所有测试通过

---

## ✅ Phase 3: 核心适配器开发 (3-4 天)

### 任务 3.1: 开发 StateManager

**预估时间**: 3 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/StateManager.java`

**实现要点**:
```java
@Component
public class StateManager {
    
    public MemorySaver createMemorySaver() {
        return new MemorySaver();
    }
    
    public WorkflowState initializeState(String inputData) {
        // 创建并初始化状态对象
    }
    
    public ExecutionRecord saveExecutionRecord(WorkflowState state, Long flowId, int duration) {
        // 将状态转换为 ExecutionRecord
    }
}
```

**参考**: 设计文档 § 4.6

**验收标准**:
- [ ] 实现 createMemorySaver()
- [ ] 实现 initializeState()
- [ ] 实现 saveExecutionRecord()
- [ ] 确保 ExecutionRecord 格式与旧引擎一致
- [ ] 添加日志记录

---

### 任务 3.2: 开发 NodeAdapter

**预估时间**: 4-5 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/NodeAdapter.java`

**实现要点**:
```java
@Component
public class NodeAdapter {
    @Autowired
    private NodeExecutorFactory executorFactory;
    
    private ThreadLocal<Consumer<ExecutionEvent>> eventCallbackHolder = new ThreadLocal<>();
    
    public void setEventCallback(Consumer<ExecutionEvent> callback) {
        eventCallbackHolder.set(callback);
    }
    
    public void clearEventCallback() {
        eventCallbackHolder.remove();
    }
    
    public NodeAction<WorkflowState> adaptNode(WorkflowNode node) throws Exception {
        NodeExecutor executor = executorFactory.getExecutor(node.getType());
        return wrapExecutor(executor, node);
    }
    
    private NodeAction<WorkflowState> wrapExecutor(NodeExecutor executor, WorkflowNode node) {
        return state -> {
            // 1. 发送 NODE_START 事件
            // 2. 提取输入（从 state.getPreviousNodeOutput()）
            // 3. 调用 executor.execute(node, input, callback)
            // 4. 更新 state.updateNodeOutput()
            // 5. 发送 NODE_SUCCESS 事件
            // 6. 返回 Map.of("state", state)
        };
    }
}
```

**参考**: 设计文档 § 4.5

**验收标准**:
- [ ] 实现 adaptNode() 方法
- [ ] 实现 wrapExecutor() 方法
- [ ] 正确处理输入输出转换
- [ ] 正确传递 SSE 事件回调
- [ ] 异常处理完善（catch 后更新 state 并抛出）
- [ ] 添加详细的调试日志

---

### 任务 3.3: 开发 GraphBuilder

**预估时间**: 4-5 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/GraphBuilder.java`

**实现要点**:
```java
@Component
public class GraphBuilder {
    @Autowired
    private NodeAdapter nodeAdapter;
    
    public StateGraph<WorkflowState> buildGraph(WorkflowConfig config) throws Exception {
        StateGraph<WorkflowState> graph = new StateGraph<>(WorkflowState.class);
        
        addNodes(graph, config.getNodes());
        addEdges(graph, config.getEdges());
        configureFlow(graph, config);
        
        return graph;
    }
    
    private void addNodes(StateGraph<WorkflowState> graph, List<WorkflowNode> nodes) { ... }
    private void addEdges(StateGraph<WorkflowState> graph, List<WorkflowEdge> edges) { ... }
    private void configureFlow(StateGraph<WorkflowState> graph, WorkflowConfig config) { ... }
}
```

**参考**: 设计文档 § 4.4

**验收标准**:
- [ ] 实现 buildGraph() 主方法
- [ ] 实现 addNodes() 添加节点
- [ ] 实现 addEdges() 添加边
- [ ] 实现 configureFlow() 设置入口/出口
- [ ] 自动识别入口节点（type="input"）
- [ ] 自动识别出口节点（type="output"）
- [ ] 支持条件边（预留扩展点，当前当作普通边处理）
- [ ] 添加详细日志

---

### 任务 3.4: 编写适配器单元测试

**预估时间**: 3-4 小时

**文件**:
- `src/test/java/com/paiagent/engine/langgraph/StateManagerTest.java`
- `src/test/java/com/paiagent/engine/langgraph/NodeAdapterTest.java`
- `src/test/java/com/paiagent/engine/langgraph/GraphBuilderTest.java`

**测试内容**:

**StateManagerTest**:
```java
@Test
void testInitializeState() {
    WorkflowState state = stateManager.initializeState("test input");
    assertEquals("test input", state.getInputData());
    assertEquals("RUNNING", state.getStatus());
    assertNotNull(state.getStartTime());
}

@Test
void testSaveExecutionRecord() {
    WorkflowState state = createCompletedState();
    ExecutionRecord record = stateManager.saveExecutionRecord(state, 1L, 1000);
    assertEquals("SUCCESS", record.getStatus());
    assertEquals(1000, record.getDuration());
}
```

**NodeAdapterTest**:
```java
@Test
void testAdaptNode() throws Exception {
    WorkflowNode node = createTestNode("input");
    NodeAction<WorkflowState> action = nodeAdapter.adaptNode(node);
    assertNotNull(action);
}

@Test
void testWrapExecutorWithSuccess() throws Exception {
    // 模拟节点执行成功
}

@Test
void testWrapExecutorWithFailure() throws Exception {
    // 模拟节点执行失败
}
```

**GraphBuilderTest**:
```java
@Test
void testBuildSimpleGraph() throws Exception {
    WorkflowConfig config = createSimpleConfig(); // input -> llm -> output
    StateGraph<WorkflowState> graph = graphBuilder.buildGraph(config);
    assertNotNull(graph);
}

@Test
void testAutoDetectEntryAndExitNodes() throws Exception {
    // 验证自动识别入口/出口节点
}
```

**验收标准**:
- [ ] 每个组件至少 3 个测试用例
- [ ] 覆盖正常和异常场景
- [ ] 所有测试通过
- [ ] 测试覆盖率 ≥ 80%

---

## ✅ Phase 4: LangGraph 引擎实现 (3 天)

### 任务 4.1: 实现 LangGraphWorkflowEngine 核心逻辑

**预估时间**: 5-6 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/LangGraphWorkflowEngine.java`

**实现要点**:
```java
@Service
public class LangGraphWorkflowEngine implements WorkflowExecutor {
    @Autowired private GraphBuilder graphBuilder;
    @Autowired private StateManager stateManager;
    @Autowired private NodeAdapter nodeAdapter;
    @Autowired private ExecutionRecordMapper executionRecordMapper;
    
    @Override
    public String getEngineType() {
        return "langgraph";
    }
    
    @Override
    public ExecutionResponse execute(Workflow workflow, String inputData) {
        return executeWithCallback(workflow, inputData, null);
    }
    
    @Override
    public ExecutionResponse executeWithCallback(
            Workflow workflow, 
            String inputData, 
            Consumer<ExecutionEvent> eventCallback) {
        
        try {
            // 1. 解析配置
            // 2. 设置事件回调
            // 3. 构建图
            // 4. 编译图
            // 5. 初始化状态
            // 6. 执行图
            // 7. 持久化记录
            // 8. 返回响应
        } catch (Exception e) {
            // 异常处理和失败记录
        } finally {
            // 清理 ThreadLocal
        }
    }
}
```

**参考**: 设计文档 § 4.7

**验收标准**:
- [ ] 实现 execute() 和 executeWithCallback()
- [ ] 完整的执行流程（解析 → 构建 → 编译 → 执行 → 持久化）
- [ ] 异常处理完善
- [ ] ThreadLocal 清理正确
- [ ] 添加详细日志
- [ ] getEngineType() 返回 "langgraph"

---

### 任务 4.2: 实现 SSE 事件流支持

**预估时间**: 2-3 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/LangGraphWorkflowEngine.java`

**实现要点**:
```java
// 在 executeWithCallback 中
if (eventCallback != null) {
    nodeAdapter.setEventCallback(eventCallback);
    eventCallback.accept(ExecutionEvent.workflowStart(null));
}

// 执行过程中，NodeAdapter 会通过 ThreadLocal 自动转发事件：
// - NODE_START
// - NODE_PROGRESS (LLM 流式输出的 chunk)
// - NODE_SUCCESS
// - NODE_ERROR

// 执行完成后
if (eventCallback != null) {
    eventCallback.accept(ExecutionEvent.workflowComplete(
        finalState.getStatus(),
        finalState.getPreviousNodeOutput(),
        duration
    ));
}
```

**验收标准**:
- [ ] 发送 WORKFLOW_START 事件
- [ ] 通过 NodeAdapter 转发 NODE_* 事件
- [ ] 发送 WORKFLOW_COMPLETE 事件
- [ ] 事件格式与现有 ExecutionEvent 完全一致
- [ ] 支持 LLM 流式输出的 chunk 事件

---

### 任务 4.3: 实现执行记录持久化

**预估时间**: 2 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/LangGraphWorkflowEngine.java`

**实现要点**:
```java
// 执行成功后
ExecutionRecord record = stateManager.saveExecutionRecord(finalState, workflow.getId(), duration);
executionRecordMapper.insert(record);

// 执行失败后
ExecutionRecord failedRecord = createFailedRecord(workflow.getId(), inputData, e, duration);
executionRecordMapper.insert(failedRecord);
```

**验收标准**:
- [ ] 成功和失败都要记录
- [ ] ExecutionRecord 格式与旧引擎一致
- [ ] 包含所有必要字段（inputData, outputData, status, nodeResults, errorMessage, duration）
- [ ] nodeResults JSON 格式正确
- [ ] 数据库插入成功

---

### 任务 4.4: 处理边界情况和异常

**预估时间**: 2-3 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/LangGraphWorkflowEngine.java`

**边界情况**:
1. 空工作流（无节点）
2. 单节点工作流
3. 无边工作流（节点孤立）
4. 找不到入口/出口节点
5. 节点执行超时
6. LLM API 调用失败
7. 执行记录保存失败

**验收标准**:
- [ ] 所有边界情况都有明确的错误处理
- [ ] 错误信息清晰易懂
- [ ] 不会导致系统崩溃
- [ ] 失败时也要记录执行记录
- [ ] 添加相应的单元测试

---

## ✅ Phase 5: 集成测试 (2-3 天)

### 任务 5.1: 编写端到端集成测试

**预估时间**: 4-5 小时

**文件**: `src/test/java/com/paiagent/engine/langgraph/LangGraphIntegrationTest.java`

**测试用例**:
```java
@SpringBootTest
class LangGraphIntegrationTest {
    
    @Autowired
    private LangGraphWorkflowEngine engine;
    
    @Autowired
    private WorkflowService workflowService;
    
    @Test
    void testSimpleWorkflow() {
        // 创建测试工作流: input -> openai -> output
        Workflow workflow = createTestWorkflow("langgraph");
        
        ExecutionResponse response = engine.execute(workflow, "测试输入");
        
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getExecutionId());
        assertEquals(3, response.getNodeResults().size());
    }
    
    @Test
    void testSSEStreamingExecution() throws Exception {
        List<ExecutionEvent> events = new ArrayList<>();
        
        ExecutionResponse response = engine.executeWithCallback(
            workflow, 
            "测试输入", 
            events::add
        );
        
        // 验证事件序列
        assertTrue(events.stream().anyMatch(e -> "WORKFLOW_START".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "NODE_START".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "NODE_SUCCESS".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "WORKFLOW_COMPLETE".equals(e.getEventType())));
    }
    
    @Test
    void testExecutionRecordPersistence() {
        ExecutionResponse response = engine.execute(workflow, "测试输入");
        
        ExecutionRecord record = executionRecordMapper.selectById(response.getExecutionId());
        
        assertNotNull(record);
        assertEquals("SUCCESS", record.getStatus());
        assertNotNull(record.getInputData());
        assertNotNull(record.getOutputData());
    }
    
    @Test
    void testNodeExecutionFailure() {
        // 创建会失败的工作流（错误的 API Key）
        Workflow workflow = createFailedWorkflow();
        
        ExecutionResponse response = engine.execute(workflow, "测试输入");
        
        assertEquals("FAILED", response.getStatus());
        assertNotNull(response.getExecutionId());
    }
    
    @Test
    void testLLMStreamingWithChunks() throws Exception {
        List<ExecutionEvent> events = new ArrayList<>();
        
        ExecutionResponse response = engine.executeWithCallback(
            workflowWithStreamingLLM, 
            "讲一个笑话", 
            events::add
        );
        
        // 验证有 NODE_PROGRESS 事件（chunk）
        long progressEvents = events.stream()
            .filter(e -> "NODE_PROGRESS".equals(e.getEventType()))
            .count();
        
        assertTrue(progressEvents > 0, "应该有流式输出的 chunk 事件");
    }
}
```

**验收标准**:
- [ ] 至少 5 个集成测试用例
- [ ] 覆盖正常执行、SSE 流式、持久化、失败场景
- [ ] 所有测试通过
- [ ] 测试数据自动清理

---

### 任务 5.2: 验证引擎选择逻辑

**预估时间**: 2-3 小时

**文件**: `src/test/java/com/paiagent/engine/EngineIntegrationTest.java`

**测试用例**:
```java
@SpringBootTest
class EngineIntegrationTest {
    
    @Autowired
    private ExecutionController controller;
    
    @Autowired
    private WorkflowService workflowService;
    
    @Test
    void testLegacyEngineExecution() {
        // 创建不带 engineType 的工作流
        Workflow workflow = createSimpleWorkflow(null);
        workflow = workflowService.save(workflow);
        
        ExecutionRequest request = new ExecutionRequest();
        request.setInputData("测试输入");
        
        Result<ExecutionResponse> result = controller.executeWorkflow(workflow.getId(), request);
        
        assertEquals(200, result.getCode());
        assertEquals("SUCCESS", result.getData().getStatus());
    }
    
    @Test
    void testLangGraphEngineExecution() {
        // 创建显式设置 engineType: "langgraph" 的工作流
        Workflow workflow = createWorkflowWithEngineType("langgraph");
        workflow = workflowService.save(workflow);
        
        ExecutionRequest request = new ExecutionRequest();
        request.setInputData("测试输入");
        
        Result<ExecutionResponse> result = controller.executeWorkflow(workflow.getId(), request);
        
        assertEquals(200, result.getCode());
        assertEquals("SUCCESS", result.getData().getStatus());
    }
    
    @Test
    void testSSEStreamWithBothEngines() {
        // 测试旧引擎的 SSE
        testSSEStream(createSimpleWorkflow(null));
        
        // 测试新引擎的 SSE
        testSSEStream(createWorkflowWithEngineType("langgraph"));
    }
}
```

**验收标准**:
- [ ] 测试旧引擎仍然正常工作
- [ ] 测试新引擎可以正确执行
- [ ] 测试引擎选择逻辑正确
- [ ] 两种引擎的输出格式一致

---

### 任务 5.3: 验证现有工作流兼容性

**预估时间**: 3-4 小时

**操作步骤**:
1. 运行项目中所有现有的测试用例
2. 手动测试现有的示例工作流
3. 对比新旧引擎的执行结果
4. 验证前端无需改动

**文件**:
- `src/test/java/com/paiagent/PaiAgentApplicationTests.java`
- 其他已有测试类

**测试清单**:
- [ ] 运行 `mvn test`，确保所有测试通过
- [ ] 手动测试：创建工作流（不设置 engineType）
- [ ] 手动测试：执行工作流（应该走旧引擎）
- [ ] 手动测试：查看执行记录（格式应该与之前一致）
- [ ] 手动测试：SSE 流式执行（前端展示正常）
- [ ] 手动测试：创建工作流（设置 engineType: "langgraph"）
- [ ] 手动测试：执行工作流（应该走新引擎）
- [ ] 手动测试：对比两种引擎的输出（应该格式一致）

**验收标准**:
- [ ] 所有现有测试用例通过
- [ ] 现有工作流执行正常
- [ ] 执行记录格式一致
- [ ] SSE 事件格式一致
- [ ] 前端无需任何改动
- [ ] **核心指标：100% 向后兼容**

---

## ✅ Phase 6: 文档和示例 (1-2 天)

### 任务 6.1: 编写使用文档

**预估时间**: 3-4 小时

**文件**: `docs/langgraph-integration-guide.md`

**文档结构**:
```markdown
# LangGraph4j 集成使用指南

## 1. 概述
- 什么是 LangGraph4j
- 为什么引入 LangGraph4j
- 与旧引擎的区别

## 2. 快速开始
- 如何创建 LangGraph 工作流
- 如何指定引擎类型
- 简单示例

## 3. 工作流配置
- metadata 配置项
- engineType 参数说明
- 节点类型支持

## 4. 高级特性（预留）
- 条件分支（未来支持）
- 循环节点（未来支持）
- Agent 节点（未来支持）

## 5. 故障排查
- 常见问题和解决方案
- 日志查看指南

## 6. API 参考
- 引擎选择规则
- 执行记录格式
- SSE 事件格式
```

**验收标准**:
- [ ] 文档完整清晰
- [ ] 包含示例代码
- [ ] 包含配置说明
- [ ] 包含故障排查指南

---

### 任务 6.2: 提供示例工作流

**预估时间**: 2 小时

**文件**: `docs/examples/`

**示例内容**:

**example-1-simple.json** (简单示例)
```json
{
  "metadata": {
    "name": "简单 LangGraph 工作流",
    "engineType": "langgraph"
  },
  "nodes": [...],
  "edges": [...]
}
```

**example-2-streaming.json** (流式输出示例)
```json
{
  "metadata": {
    "name": "LLM 流式输出示例",
    "engineType": "langgraph"
  },
  "nodes": [
    {
      "id": "llm",
      "type": "openai",
      "data": {
        "streaming": true,
        ...
      }
    }
  ]
}
```

**验收标准**:
- [ ] 至少 2 个完整的示例
- [ ] 示例可以直接导入使用
- [ ] 示例覆盖常见场景

---

### 任务 6.3: 更新 API 文档

**预估时间**: 2 小时

**文件**: `src/main/java/com/paiagent/controller/ExecutionController.java`

**更新 Swagger 注释**:
```java
@Operation(
    summary = "执行工作流",
    description = "支持两种执行引擎：\n" +
                  "1. legacy: 传统 DAG 引擎（默认）\n" +
                  "2. langgraph: 基于 LangGraph4j 的新引擎\n\n" +
                  "引擎选择规则：\n" +
                  "- 工作流配置中设置 metadata.engineType\n" +
                  "- 包含高级节点（condition, loop, agent）时自动使用新引擎\n" +
                  "- 默认使用旧引擎确保兼容性"
)
@PostMapping("/{id}/execute")
public Result<ExecutionResponse> executeWorkflow(...) { ... }
```

**验收标准**:
- [ ] 更新所有相关接口的注释
- [ ] 说明引擎选择机制
- [ ] Swagger UI 展示正确

---

### 任务 6.4: 编写迁移指南

**预估时间**: 2 小时

**文件**: `docs/migration-guide.md`

**文档结构**:
```markdown
# 工作流迁移指南

## 1. 迁移必要性评估
- 哪些工作流需要迁移
- 哪些工作流不需要迁移

## 2. 迁移步骤
- 备份现有工作流
- 添加 metadata.engineType: "langgraph"
- 测试执行
- 对比结果

## 3. 迁移注意事项
- 执行记录格式变化（无）
- API 接口变化（无）
- 前端改动（无）

## 4. 回滚方案
- 如何切换回旧引擎
- 如何恢复数据

## 5. 常见问题
- Q&A
```

**验收标准**:
- [ ] 提供清晰的迁移步骤
- [ ] 说明迁移风险和注意事项
- [ ] 提供回滚方案

---

## ✅ Phase 7: 发布和监控 (1 天)

### 任务 7.1: 性能基准测试

**预估时间**: 3-4 小时

**文件**: `src/test/java/com/paiagent/benchmark/EngineBenchmarkTest.java`

**测试内容**:
```java
@SpringBootTest
class EngineBenchmarkTest {
    
    @Test
    void benchmarkLegacyEngine() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            legacyEngine.execute(workflow, "测试输入");
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("旧引擎平均耗时: " + (duration / 100) + "ms");
    }
    
    @Test
    void benchmarkLangGraphEngine() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            langGraphEngine.execute(workflow, "测试输入");
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("新引擎平均耗时: " + (duration / 100) + "ms");
    }
}
```

**验收标准**:
- [ ] 测试执行时间
- [ ] 测试内存占用
- [ ] 新引擎性能与旧引擎持平（±10%）
- [ ] 记录性能指标到文档

---

### 任务 7.2: 代码审查

**预估时间**: 2-3 小时

**审查清单**:
- [ ] 代码符合项目规范
- [ ] 注释完整清晰
- [ ] 异常处理完善
- [ ] 日志记录合理
- [ ] 无安全漏洞
- [ ] 无性能问题
- [ ] 测试覆盖充分
- [ ] 文档完整准确

**审查工具**:
- SonarQube（如有）
- IDEA 代码检查
- 人工 Code Review

**验收标准**:
- [ ] 通过 SonarQube 扫描（无严重问题）
- [ ] 团队 Code Review 通过
- [ ] 所有评审意见已修复

---

### 任务 7.3: 制定发布计划

**预估时间**: 2 小时

**文件**: `docs/release-plan.md`

**发布策略**:
```markdown
# 发布计划

## 阶段 1: 内部测试 (1 周)
- 团队内部使用新引擎
- 收集反馈和问题
- 修复 Bug

## 阶段 2: 灰度发布 (1-2 周)
- 小范围用户测试
- 监控执行情况
- 根据反馈优化

## 阶段 3: 全量发布
- 所有用户可用
- 文档和培训
- 持续监控

## 回滚方案
- 如何快速切换回旧引擎
- 数据恢复策略
```

**验收标准**:
- [ ] 发布计划清晰
- [ ] 包含灰度策略
- [ ] 包含回滚方案
- [ ] 明确发布时间点

---

### 任务 7.4: 添加监控和日志

**预估时间**: 2-3 小时

**文件**: `src/main/java/com/paiagent/engine/langgraph/LangGraphWorkflowEngine.java`

**关键日志记录点**:
```java
log.info("LangGraph 引擎执行开始 - workflowId: {}, engineType: {}", workflow.getId(), getEngineType());
log.debug("构建状态图 - 节点数: {}, 边数: {}", nodes.size(), edges.size());
log.debug("编译状态图完成");
log.info("状态图执行完成 - workflowId: {}, status: {}, duration: {}ms", workflow.getId(), finalState.getStatus(), duration);
log.error("LangGraph 引擎执行失败 - workflowId: {}, error: {}", workflow.getId(), e.getMessage(), e);
```

**监控指标**（如有监控系统）:
- 引擎选择分布（legacy vs langgraph）
- 执行成功率
- 执行耗时分布
- 错误类型统计

**验收标准**:
- [ ] 添加关键日志记录点
- [ ] 日志级别合理（INFO/DEBUG/ERROR）
- [ ] 日志格式统一
- [ ] 日志包含关键上下文信息
- [ ] 配置监控指标（如有）

---

## 📊 总体验收标准

### 功能验收
- [ ] 新引擎可以正确执行工作流
- [ ] 引擎选择逻辑正确
- [ ] SSE 事件流正常
- [ ] 执行记录持久化正常
- [ ] 现有工作流 100% 兼容
- [ ] API 接口不变
- [ ] 前端无需改动

### 质量验收
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] 所有集成测试通过
- [ ] 代码审查通过
- [ ] 性能与旧引擎持平（±10%）
- [ ] 无安全漏洞
- [ ] 文档完整

### 发布验收
- [ ] 内部测试通过
- [ ] 灰度发布顺利
- [ ] 监控和日志完善
- [ ] 回滚方案可行

---

## 📁 文件清单

### 新增文件 (13 个)
```
src/main/java/com/paiagent/engine/
├── WorkflowExecutor.java                              (接口)
├── EngineSelector.java                                (选择器)
└── langgraph/
    ├── WorkflowState.java                             (状态模型)
    ├── GraphBuilder.java                              (图构建器)
    ├── NodeAdapter.java                               (节点适配器)
    ├── StateManager.java                              (状态管理器)
    └── LangGraphWorkflowEngine.java                   (核心引擎)

src/test/java/com/paiagent/
├── engine/
│   ├── EngineSelectorTest.java                        (单元测试)
│   ├── EngineIntegrationTest.java                     (集成测试)
│   └── langgraph/
│       ├── StateManagerTest.java                      (单元测试)
│       ├── NodeAdapterTest.java                       (单元测试)
│       ├── GraphBuilderTest.java                      (单元测试)
│       ├── LangGraphIntegrationTest.java              (集成测试)
│       └── LangGraphBasicTest.java                    (依赖验证)
└── benchmark/
    └── EngineBenchmarkTest.java                       (性能测试)

docs/
├── langgraph-integration-guide.md                     (使用指南)
├── migration-guide.md                                 (迁移指南)
├── release-plan.md                                    (发布计划)
└── examples/
    ├── example-1-simple.json                          (示例 1)
    └── example-2-streaming.json                       (示例 2)
```

### 修改文件 (3 个)
```
pom.xml                                                (添加依赖)
src/main/java/com/paiagent/engine/WorkflowEngine.java (实现接口)
src/main/java/com/paiagent/controller/ExecutionController.java (使用选择器)
```

---

## 🎯 关键里程碑

| 里程碑 | 预计完成时间 | 交付物 |
|--------|-------------|--------|
| **M1: 基础设施就绪** | Day 3 | 依赖添加完成、基础类创建 |
| **M2: 引擎抽象层完成** | Day 5 | 接口定义、选择器开发、Controller 改造 |
| **M3: 适配器开发完成** | Day 9 | NodeAdapter、GraphBuilder、StateManager |
| **M4: 引擎实现完成** | Day 12 | LangGraphWorkflowEngine 可执行 |
| **M5: 测试通过** | Day 15 | 所有单元测试和集成测试通过 |
| **M6: 文档完善** | Day 17 | 使用文档、示例、迁移指南 |
| **M7: 发布就绪** | Day 18 | 性能测试、代码审查、发布计划 |

---

## ⚠️ 风险提示

| 风险 | 应对措施 |
|------|----------|
| **LangGraph4j API 不熟悉** | 提前阅读官方文档，编写 POC 验证 |
| **适配器逻辑复杂** | 分步实现，充分测试 |
| **旧工作流兼容性问题** | 优先验证兼容性，出问题立即回滚 |
| **性能不达预期** | 性能测试前置，及时优化 |
| **开发周期超期** | 分阶段交付，核心功能优先 |

---

## 📞 联系方式

**技术支持**: Design Agent  
**代码审查**: 待指定  
**项目经理**: 待指定

---

**任务状态**: 🟡 待开始  
**最后更新**: 2026-01-26
