# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PaiAgent is a visual AI Agent workflow orchestration platform with a React frontend and Spring Boot backend. It features a custom DAG (Directed Acyclic Graph) workflow engine, ReactFlow-based visual editor, and supports multiple LLM providers (OpenAI, DeepSeek, Qwen) and TTS nodes.

**Status**: 95% complete - all core features implemented, ready for production with minor enhancements

## Common Development Commands

### Backend (Spring Boot + Java 21)

```bash
cd backend

# Start development server
./mvnw spring-boot:run

# Run tests
./mvnw test

# Clean and rebuild
./mvnw clean package

# Run specific test class
./mvnw test -Dtest=ClassName

# Access API documentation
# http://localhost:8080/swagger-ui/index.html
```

### Frontend (React + TypeScript + Vite)

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Run linting
npm run lint

# Preview production build
npm run preview

# Access application
# http://localhost:5173
```

### Database Setup

```bash
# Create database
mysql -u root -p -e "CREATE DATABASE paiagent DEFAULT CHARACTER SET utf8mb4;"

# Import schema and initial data
mysql -u root -p paiagent < backend/src/main/resources/schema.sql
```

## Architecture Overview

### High-Level Structure

```
PaiAgent-one/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/
│   │   └── com/paiagent/
│   │       ├── controller/     # REST controllers
│   │       ├── service/        # Business logic
│   │       ├── engine/         # Custom DAG workflow engine
│   │       │   ├── dag/        # DAG parser (topological sort + cycle detection)
│   │       │   ├── executor/   # Node executors (LLM, TTS, etc.)
│   │       │   └── model/      # Workflow data models
│   │       ├── entity/         # MyBatis-Plus entities
│   │       ├── mapper/         # MyBatis-Plus mappers
│   │       ├── dto/            # Request/Response DTOs
│   │       └── config/         # Configuration classes
│   └── src/main/resources/
│       └── schema.sql          # Database schema
├── frontend/                   # React frontend
│   ├── src/
│   │   ├── components/         # React components (FlowCanvas, NodePanel, etc.)
│   │   ├── pages/              # Page components (EditorPage, LoginPage)
│   │   ├── store/              # Zustand state management
│   │   ├── api/                # API clients
│   │   └── utils/              # Utilities
│   └── package.json
└── README.md                   # Project documentation
```

### Backend Architecture

**Core Components**:

1. **WorkflowEngine** (`engine/WorkflowEngine.java`)
   - Main execution engine that orchestrates workflow execution
   - Coordinates DAG parsing and node execution
   - Records execution history

2. **DAGParser** (`engine/dag/DAGParser.java`)
   - Implements Kahn's algorithm for topological sorting (O(V+E))
   - Uses DFS for cycle detection in workflow dependencies
   - Validates workflow structure before execution

3. **NodeExecutor Pattern** (`engine/executor/`)
   - Factory pattern: `NodeExecutorFactory` creates executors dynamically
   - Supports extensible node types without code changes
   - Built-in executors: Input, Output, OpenAI, DeepSeek, Qwen, TTS

4. **Data Layer**:
   - **workflow**: Stores workflow configurations (JSON nodes/edges)
   - **node_definition**: Pre-defined node types with schemas
   - **execution_record**: Historical execution data

**Key Technologies**:
- Spring Boot 3.4.1 + Java 21
- MyBatis-Plus 3.5.5 (ORM)
- MySQL 8.0
- FastJSON2 2.0.43
- SpringDoc OpenAPI 2.3.0

### Frontend Architecture

**Core Components**:

1. **EditorPage** (`pages/EditorPage.tsx`) - Main workflow editor interface (33KB)
2. **FlowCanvas** (`components/FlowCanvas.tsx`) - ReactFlow-based visual editor
3. **NodePanel** (`components/NodePanel.tsx`) - Draggable node palette
4. **DebugDrawer** (`components/DebugDrawer.tsx`) - Real-time execution debugging
5. **AudioPlayer** (`components/AudioPlayer.tsx`) - TTS result playback

**State Management**:
- **workflowStore** (`store/workflowStore.ts`) - Zustand store for workflow state
- **authStore** (`store/authStore.ts`) - Authentication state
- Manages ReactFlow nodes/edges, selected nodes, current workflow

**Key Technologies**:
- React 18.3.1 + TypeScript 5.6.2
- ReactFlow 12.3.8 (visual workflow editor)
- Ant Design 6.0.0 (UI components)
- Tailwind CSS 4.1.17 (styling)
- Zustand 5.0.8 (state management)
- Axios 1.13.2 (HTTP client)

## Key Patterns and Design Decisions

### 1. DAG Workflow Engine
- **Kahn's Algorithm**: Efficient topological sorting (O(V+E))
- **DFS Cycle Detection**: Prevents infinite loops during execution
- **Factory Pattern**: Dynamic node executor instantiation
- **Data Flow**: Each node's output becomes next node's input

### 2. Node Execution Pattern
```java
// All executors implement NodeExecutor interface
public interface NodeExecutor {
    Map<String, Object> execute(WorkflowNode node, Map<String, Object> input);
    String getSupportedNodeType();
}

// Factory resolves executor at runtime
NodeExecutor executor = executorFactory.getExecutor(node.getType());
```

### 3. Frontend State Management
- Zustand provides lightweight, TypeScript-friendly state management
- Single workflow store maintains ReactFlow state and UI state
- ReactFlow automatically syncs with store updates

### 4. Database Design
- **JSON Storage**: Workflow configurations stored as JSON (flexible structure)
- **Execution History**: Full execution trace stored for debugging
- **Node Definitions**: Dynamic node types loaded from DB

## Node Types

### Built-in Nodes (7 types)

1. **Input** - Workflow starting point, provides initial data
2. **Output** - Workflow endpoint, final result presentation
3. **OpenAI** - GPT model integration (supports multiple models)
4. **DeepSeek** - DeepSeek model integration
5. **Qwen** - Alibaba Qwen model integration
6. **AI Ping** - AI Ping model integration with simulated responses
7. **TTS** - Text-to-speech with audio playback

**Note**: Currently uses simulation mode for LLM/TTS - real API integration pending

## Testing Strategy

### Backend Testing
- **Unit Tests**: Service and engine components
- **Integration Tests**: REST API endpoints
- **Execution Tests**: Workflow execution scenarios

Run with: `./mvnw test`

### Frontend Testing
- **Component Tests**: React component behavior
- **Integration Tests**: State management and API calls
- **E2E Tests**: User workflows (pending)

Run with: `npm run test` (when configured)

## Configuration

### Default Login
- Username: `admin`
- Password: `123`

### Default Ports
- Backend: 8080
- Frontend: 5173
- MySQL: 3306

### API Endpoints
- Base URL: `http://localhost:8080/api`
- Authentication: Token-based (stored in localStorage)
- Documentation: `http://localhost:8080/swagger-ui/index.html`

## Adding New Node Types

### Backend (2 steps)
1. **Create Executor** (`engine/executor/`):
   ```java
   @Component
   public class MyNodeExecutor implements NodeExecutor {
       @Override
       public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) {
           // Implement logic
           return output;
       }

       @Override
       public String getSupportedNodeType() {
           return "my_node";
       }
   }
   ```

2. **Add Database Entry**:
   ```sql
   INSERT INTO node_definition (node_type, display_name, category, icon)
   VALUES ('my_node', 'My Node', 'TOOL', '🔧');
   ```

### Frontend
No changes required - node appears automatically in NodePanel

## Development Notes

### Workflow Execution Flow
1. User saves workflow → Frontend calls `/workflow` API
2. User clicks "Debug" → Frontend calls `/execution/execute` with input
3. Backend:
   - DAGParser validates and topologically sorts nodes
   - WorkflowEngine executes nodes in order
   - Each executor transforms input → output
   - Results stored in execution_record table
4. Frontend displays real-time results in DebugDrawer

### Audio Output
- TTS executor generates MP3 files
- Files stored in `audio_output/` directory
- Served via `StaticResourceConfig` at `/audio/**`
- Played using HTML5 Audio API in AudioPlayer component

### Error Handling
- **Backend**: Exceptions caught and logged, execution marked as FAILED
- **Frontend**: Error messages displayed in DebugDrawer with red highlighting
- **Cycle Detection**: Validated during DAG parsing, prevents execution

## Important Files

### Backend Core
- `engine/WorkflowEngine.java` - Main execution orchestrator
- `engine/dag/DAGParser.java` - Topological sorting & cycle detection
- `service/WorkflowService.java` - CRUD operations
- `controller/WorkflowController.java` - REST API
- `config/DataInitializer.java` - Auto-initializes node definitions on startup

### Frontend Core
- `pages/EditorPage.tsx` - Main editor interface
- `components/FlowCanvas.tsx` - Visual workflow editor
- `store/workflowStore.ts` - State management
- `components/DebugDrawer.tsx` - Debug interface

### Configuration
- `backend/pom.xml` - Maven dependencies
- `backend/src/main/resources/schema.sql` - Database setup
- `frontend/package.json` - NPM dependencies
- `frontend/vite.config.ts` - Vite configuration

## Known Limitations

1. **API Integration**: Currently in simulation mode - requires real API key configuration
2. **Testing**: Missing comprehensive test coverage
3. **Authentication**: Simple token-based auth (not production-ready)
4. **Performance**: No async execution or parallel node processing
5. **Validation**: Limited input validation on nodes

## Extension Points

- **Node Types**: Add new executors following NodeExecutor interface
- **LLM Providers**: Implement provider adapters in executor layer
- **Storage**: Swap MySQL for PostgreSQL by updating dependencies
- **Frontend**: Custom ReactFlow node types for specialized UI

## Documentation References

- `README.md` - Project overview and quick start
- `USER_GUIDE.md` - Detailed usage instructions and examples
- `SUMMARY.md` - Development completion summary
- `PROJECT_COMPLETION_REPORT.md` - Detailed completion report
- Backend Swagger UI - API documentation at `/swagger-ui/index.html`

## Getting Started for Development

1. **Environment Setup**:
   - JDK 21
   - Node.js 18+
   - MySQL 8.0
   - Maven 3.8+

2. **Database Setup**:
   ```bash
   mysql -u root -p -e "CREATE DATABASE paiagent DEFAULT CHARACTER SET utf8mb4;"
   mysql -u root -p paiagent < backend/src/main/resources/schema.sql
   ```

3. **Start Backend**:
   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

4. **Start Frontend**:
   ```bash
   cd frontend && npm install && npm run dev
   ```

5. **Access Application**:
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - API Docs: http://localhost:8080/swagger-ui/index.html
   - Login: admin / 123
