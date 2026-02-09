## 项目名称：PaiAgent - 企业级 AI 工作流可视化编排平台

**项目介绍**：基于 LangGraph4j + Spring AI 的企业级 AI 工作流平台，支持通过可视化拖拽界面编排多种大模型（OpenAI、DeepSeek、通义千问）和工具节点，使用 DAG 引擎按拓扑顺序执行复杂 AI 任务，实现零代码构建 AI Agent 应用。

**技术栈**：Java 21、Spring Boot 3.4.1、Spring AI 1.0.0-M5、LangGraph4j 1.8、React 18、TypeScript、ReactFlow

**核心职责**：

项目名称：PaiAgent - 企业级 AI 工作流可视化编排平台

**项目介绍**：基于 LangGraph4j + Spring AI 的企业级 AI 工作流平台，支持通过可视化拖拽界面编排多种大模型（OpenAI、DeepSeek、通义千问）和工具节点，使用 DAG 引擎按拓扑顺序执行复杂 AI 任务，实现零代码构建 AI Agent 应用。

**技术栈**：Java 21、Spring Boot 3.4.1、Spring AI 1.0.0-M5、LangGraph4j 1.8

**核心职责**：

- 基于 LangGraph4j StateGraph 构建工作流引擎，GraphBuilder 负责节点注册和边连接，NodeAdapter 将现有执行器适配为 LangGraph 异步节点执行，StateManager 管理节点间状态传递，实现支持状态图、条件分支的高级工作流编排
- **Spring AI 多模型统一接入架构**：设计 ChatClientFactory 动态工厂，运行时根据节点配置（apiUrl/apiKey/model/temperature）动态创建 OpenAI 兼容的 ChatClient 实例，通过 OpenAiChatOptions 统一配置模型参数，实现 OpenAI、DeepSeek、通义千问等多厂商 LLM 的无缝切换
- **使用模板方法模式重构 LLM 节点执行器**，抽象 AbstractLLMNodeExecutor 基类封装配置提取、模板处理、API 调用、输出构建的通用流程，子类仅需实现 getNodeType() 方法，将 5 个 LLM 节点执行器代码从 800+ 行精简至每个约 10 行
- 实现 Prompt 支持模板变量替换，可通过 `{{variable}}` 解析 input 静态值和 reference 动态引用两种参数类型，支持从上游节点输出中自动获取参数值，实现节点间数据流的灵活映射
- 实现 DAG 工作流解析引擎，基于 Kahn 算法的拓扑排序确定节点执行顺序，DFS 深度优先搜索检测循环依赖防止死锁，支持一对多、多对一的节点连接方式
- 基于 Spring SseEmitter 实现工作流执行的实时反馈，设计 ExecutionEvent 事件模型，通过 `Consumer<ExecutionEvent>` 回调机制将 LLM 流式生成内容实时推送到前端调试面板
- **ReactFlow 可视化流程编辑器**：基于 @xyflow/react 构建专业流程图编辑器，支持节点拖拽、连线配置、参数编辑，配合 Zustand 实现工作流编辑状态的轻量级管理
