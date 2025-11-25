# AI Agent 流图执行面板

一个可视化的 AI Agent 工作流编排与执行平台。

## 项目概述

本项目是一个企业级的 AI 工作流编排平台,支持通过可视化拖拽方式组合大模型节点和工具节点,构建自定义的 AI 处理流程。

### 核心功能

- 🎨 **可视化流程编辑器**: 拖拽式节点组合,直观的流程设计
- 🤖 **多大模型支持**: 支持 OpenAI、DeepSeek、通义千问等多种大模型
- 🔧 **工具节点扩展**: 内置超拟人音频合成等工具,支持自定义扩展
- 🐛 **实时调试功能**: 内置调试面板,实时测试工作流执行效果
- ⚡ **DAG 工作流引擎**: 自研轻量级引擎,高效的节点调度执行

## 技术栈

### 前端
- React 18 + TypeScript
- Vite (构建工具)
- ReactFlow (流程图编辑器)
- Ant Design / Tailwind CSS (UI 组件库)

### 后端
- Spring Boot 3.x
- MySQL / PostgreSQL
- Maven

### 工作流引擎
- 自研 DAG 引擎
- 支持节点编排和执行调度

## 项目结构

```
PaiAgent-one/
├── backend/          # Spring Boot 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
├── frontend/         # React 前端项目
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── utils/
│   ├── package.json
│   └── vite.config.ts
└── README.md
```

## 快速开始

### 后端启动

```bash
cd backend
./mvnw spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

## 开发计划

- [x] 项目初始化
- [x] 后端项目初始化 (Spring Boot 3.x + Java 21)
- [x] 数据库表结构设计
- [x] 用户认证功能 (简单 Token 认证)
- [x] 前端项目初始化 (React + TypeScript + Vite)
- [x] 登录/登出功能
- [x] 后端 DAG 引擎开发 (拓扑排序 + 循环检测)
- [x] 节点执行器实现 (输入/输出/OpenAI/TTS)
- [x] 工作流执行引擎
- [x] 工作流管理 API (CRUD + 执行)
- [x] 前端流程编辑器开发 (ReactFlow)
- [x] 节点面板和画布功能
- [x] 工作流保存和执行
- [x] 调试功能实现 (调试抽屉 + 结果展示 + 日志输出)
- [x] TTS 节点和音频播放器集成
- [ ] 集成测试

**当前完成度: 95%**

更多详情请查看:
- [PROGRESS.md](./PROGRESS.md) - 开发进度
- [SUMMARY.md](./SUMMARY.md) - 项目总结

## License

MIT
