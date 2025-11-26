# AGENTS.md

This file provides guidance to Qoder (qoder.com) when working with code in this repository.

## Project Overview

PaiAgent-one is an enterprise-grade AI workflow orchestration platform with visual flow editor. It uses a custom DAG (Directed Acyclic Graph) engine to execute workflows composed of LLM nodes (OpenAI, DeepSeek, Qwen) and tool nodes (TTS, etc.).

## Build & Development Commands

### Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run              # Start backend server (port 8080)
./mvnw clean package                # Build JAR package
./mvnw test                         # Run tests
```

### Frontend (React + Vite)
```bash
cd frontend
npm install                         # Install dependencies
npm run dev                         # Start dev server (port 5173)
npm run build                       # Build for production (TypeScript check + Vite build)
npm run lint                        # Run ESLint
npm run preview                     # Preview production build
```

### Database Setup
```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

Update database credentials in `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/paiagent?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## Architecture

### Backend Structure (`backend/src/main/java/com/paiagent/`)

**Core DAG Engine (`engine/`):**
- `WorkflowEngine.java`: Main orchestration engine that executes workflows end-to-end
- `dag/DAGParser.java`: Parses workflow config into DAG, performs topological sorting using Kahn's algorithm, and detects cycles using DFS
- `executor/NodeExecutor.java`: Interface for all node executors
- `executor/NodeExecutorFactory.java`: Factory pattern to get executors by node type
- `executor/impl/`: Concrete implementations (InputNodeExecutor, OutputNodeExecutor, OpenAINodeExecutor, TTSNodeExecutor)
- `model/`: Data models (WorkflowConfig, WorkflowNode, WorkflowEdge)

**Application Layers:**
- `controller/`: REST API endpoints
- `service/`: Business logic layer
- `mapper/`: MyBatis-Plus data access layer
- `entity/`: Database entities (Workflow, ExecutionRecord, NodeDefinition, User)
- `dto/`: Data transfer objects
- `config/`: Configuration classes (WebMvcConfig, MyBatisConfig)
- `interceptor/`: AuthInterceptor for token-based authentication
- `common/`: Common utilities and result wrappers

### Frontend Structure (`frontend/src/`)

**Core Components:**
- `components/FlowCanvas.tsx`: ReactFlow-based visual workflow editor
- `components/NodePanel.tsx`: Draggable node palette (LLM/Tool categories)
- `components/DebugDrawer.tsx`: Execution debugging panel with real-time logs and results
- `components/AudioPlayer.tsx`: Audio playback component for TTS output

**Pages:**
- `pages/LoginPage.tsx`: Authentication page
- `pages/MainPage.tsx`: Workflow list management
- `pages/EditorPage.tsx`: Main workflow editor with canvas, node panel, and debug drawer

**State Management (Zustand):**
- `store/authStore.ts`: User authentication state (token, user info)
- `store/workflowStore.ts`: Workflow editing state (nodes, edges, selected workflow)

**API Layer:**
- `api/`: Axios-based API client for backend communication
- `utils/request.ts`: Axios instance with auth interceptors (base URL: http://localhost:8080)

### Database Schema

Tables: `workflow`, `node_definition`, `execution_record`, `user`

Key features:
- JSON columns for workflow config (`flow_data`), execution results (`node_results`)
- Logical deletion using `deleted` field
- Pre-seeded node definitions for OpenAI, DeepSeek, Qwen, and TTS

## Workflow Execution Flow

1. User designs workflow in ReactFlow canvas (frontend)
2. Frontend serializes nodes/edges to JSON and saves via API
3. Backend stores workflow config in `workflow.flow_data`
4. On execution:
   - `WorkflowEngine` parses JSON into `WorkflowConfig`
   - `DAGParser` validates (cycle detection) and sorts nodes topologically
   - Engine executes nodes sequentially, passing output of node N as input to node N+1
   - Each node result is recorded in `ExecutionRecord.node_results`
5. Frontend displays execution results in `DebugDrawer` with logs and output data

## Key Technologies

- **Backend**: Spring Boot 3.4.1, Java 21, MyBatis-Plus 3.5.5, MySQL, FastJSON2
- **Frontend**: React 18, TypeScript, Vite, ReactFlow (@xyflow/react), Ant Design, Tailwind CSS, Zustand
- **Authentication**: Simple token-based auth (default: admin/123)
- **API Docs**: Swagger UI at http://localhost:8080/swagger-ui.html

## Development Notes

- Backend API requires authentication token in `Authorization` header
- Frontend stores token in Zustand store and localStorage
- ReactFlow node types must match backend `NodeExecutor` implementations
- Node executors follow a common interface: `execute(WorkflowNode node, Map<String, Object> input) -> Map<String, Object>`
- DAG engine uses Kahn's algorithm for topological sort and DFS for cycle detection
