import api from '../utils/request';

export interface NodeDefinition {
  id: number;
  nodeType: string;
  displayName: string;
  category: string;
  icon: string;
  inputSchema: string;
  outputSchema: string;
  configSchema: string;
}

export interface WorkflowData {
  name: string;
  description?: string;
  flowData: string;
}

export interface Workflow {
  id: number;
  name: string;
  description: string;
  flowData: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 获取所有节点类型
 */
export const getNodeTypes = (): Promise<ApiResult<NodeDefinition[]>> => {
  return api.get('/api/node-types');
};

/**
 * 创建工作流
 */
export const createWorkflow = (data: WorkflowData): Promise<ApiResult<Workflow>> => {
  return api.post('/api/workflows', data);
};

/**
 * 获取工作流列表
 */
export const getWorkflows = (): Promise<ApiResult<Workflow[]>> => {
  return api.get('/api/workflows');
};

/**
 * 获取工作流详情
 */
export const getWorkflow = (id: number): Promise<ApiResult<Workflow>> => {
  return api.get(`/api/workflows/${id}`);
};

/**
 * 更新工作流
 */
export const updateWorkflow = (id: number, data: WorkflowData): Promise<ApiResult<Workflow>> => {
  return api.put(`/api/workflows/${id}`, data);
};

/**
 * 删除工作流
 */
export const deleteWorkflow = (id: number): Promise<ApiResult<void>> => {
  return api.delete(`/api/workflows/${id}`);
};

/**
 * 执行工作流
 */
export const executeWorkflow = (id: number, inputData: string): Promise<ApiResult<any>> => {
  return api.post(`/api/workflows/${id}/execute`, { inputData });
};

export interface ExecutionEvent {
  eventType: string;
  nodeId?: string;
  nodeName?: string;
  status?: string;
  message?: string;
  data?: any;
  timestamp?: number;
}

export const executeWorkflowStream = (
  id: number, 
  inputData: string, 
  onEvent: (event: ExecutionEvent) => void,
  onComplete: () => void,
  onError: (error: Error) => void
) => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    localStorage.removeItem('token');
    window.location.href = '/login';
    onError(new Error('未登录'));
    return null;
  }
  
  const url = `http://localhost:8080/api/workflows/${id}/execute/stream?inputData=${encodeURIComponent(inputData)}&token=${token}`;
  
  const eventSource = new EventSource(url);
  
  let hasReceivedData = false;
  
  eventSource.addEventListener('WORKFLOW_START', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
  });
  
  eventSource.addEventListener('NODE_START', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
  });
  
  eventSource.addEventListener('NODE_SUCCESS', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
  });
  
  eventSource.addEventListener('NODE_PROGRESS', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
  });
  
  eventSource.addEventListener('NODE_ERROR', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
  });
  
  eventSource.addEventListener('WORKFLOW_COMPLETE', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
    eventSource.close();
    onComplete();
  });
  
  eventSource.addEventListener('ERROR', (e) => {
    hasReceivedData = true;
    const event = JSON.parse(e.data) as ExecutionEvent;
    onEvent(event);
    eventSource.close();
    onError(new Error(event.message || '执行失败'));
  });
  
  eventSource.onerror = (e) => {
    eventSource.close();
    
    if (!hasReceivedData) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      onError(new Error('认证失败,请重新登录'));
    } else {
      onError(new Error('连接中断'));
    }
  };
  
  return eventSource;
};