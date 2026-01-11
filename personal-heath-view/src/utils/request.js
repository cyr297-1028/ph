import axios from "axios"
import { getToken } from "@/utils/storage.js";

const URL_API = 'http://localhost:21090/api/personal-heath/v1.0'

const request = axios.create({
  baseURL: URL_API,
  // 将 30000 (30秒) 改为 600000 (10分钟)
  // 因为大模型推理非常耗时，必须给足够的时间等待
  timeout: 600000
});

//全局拦截器
request.interceptors.request.use(config => {
  const token = getToken();
  if (token !== null) {
    config.headers["token"] = token;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

export default request;