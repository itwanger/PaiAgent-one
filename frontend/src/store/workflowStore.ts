import { create } from 'zustand';
import { Node, Edge } from '@xyflow/react';

interface WorkflowState {
  nodes: Node[];
  edges: Edge[];
  selectedNode: Node | null;
  currentWorkflowId: number | null;
  setNodes: (nodes: Node[]) => void;
  setEdges: (edges: Edge[]) => void;
  setSelectedNode: (node: Node | null) => void;
  setCurrentWorkflowId: (id: number | null) => void;
  addNode: (node: Node) => void;
  updateNode: (id: string, data: any) => void;
  deleteNode: (id: string) => void;
  clear: () => void;
}

const defaultNodes: Node[] = [
  {
    id: 'input-default',
    type: 'default',
    position: { x: 250, y: 100 },
    data: { label: '输入', type: 'input' },
  },
  {
    id: 'output-default',
    type: 'default',
    position: { x: 250, y: 300 },
    data: { label: '输出', type: 'output' },
  },
];

export const useWorkflowStore = create<WorkflowState>((set) => ({
  nodes: defaultNodes,
  edges: [],
  selectedNode: null,
  currentWorkflowId: null,
  
  setNodes: (nodes) => set({ nodes }),
  
  setEdges: (edges) => set({ edges }),
  
  setSelectedNode: (node) => set({ selectedNode: node }),
  
  setCurrentWorkflowId: (id) => set({ currentWorkflowId: id }),
  
  addNode: (node) => set((state) => ({
    nodes: [...state.nodes, node]
  })),
  
  updateNode: (id, data) => set((state) => ({
    nodes: state.nodes.map((node) =>
      node.id === id ? { ...node, data: { ...node.data, ...data } } : node
    )
  })),
  
  deleteNode: (id) => set((state) => ({
    nodes: state.nodes.filter((node) => node.id !== id),
    edges: state.edges.filter((edge) => edge.source !== id && edge.target !== id)
  })),
  
  clear: () => set({
    nodes: defaultNodes,
    edges: [],
    selectedNode: null,
    currentWorkflowId: null
  })
}));
