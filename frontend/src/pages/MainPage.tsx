import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

/**
 * 主页面(临时占位)
 */
const MainPage = () => {
  const { username, clearAuth } = useAuthStore();

  const handleLogout = () => {
    clearAuth();
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">PaiAgent - AI Agent 流图执行面板</h1>
          <div className="flex items-center gap-4">
            <span className="text-gray-700">欢迎, {username}</span>
            <button
              onClick={handleLogout}
              className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
            >
              登出
            </button>
          </div>
        </div>
      </header>
      
      <main className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">工作流编辑器</h2>
          <p className="text-gray-600">工作流编辑器功能正在开发中...</p>
        </div>
      </main>
    </div>
  );
};

/**
 * 路由守卫
 */
const ProtectedMainPage = () => {
  const { isAuthenticated } = useAuthStore();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return <MainPage />;
};

export default ProtectedMainPage;
