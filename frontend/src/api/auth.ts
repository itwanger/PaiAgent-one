import api from '../utils/request';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    username: string;
  };
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 用户登录
 */
export const login = (data: LoginRequest): Promise<ApiResult<LoginResponse>> => {
  return api.post('/api/auth/login', data);
};

/**
 * 用户登出
 */
export const logout = (): Promise<ApiResult<void>> => {
  return api.post('/api/auth/logout');
};

/**
 * 获取当前用户信息
 */
export const getCurrentUser = (): Promise<ApiResult<{ username: string; authenticated: boolean }>> => {
  return api.get('/api/auth/current');
};
