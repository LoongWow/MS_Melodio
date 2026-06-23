import axios from 'axios';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000,
});

// Request interceptor
request.interceptors.request.use(
  (config) => {
    // You can add global headers here if needed
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
request.interceptors.response.use(
  (response) => {
    // Some API responses might put the error inside the data payload (e.g., Netease)
    // We can handle generic success here.
    return response;
  },
  (error) => {
    if (error.response) {
      const { status } = error.response;

      if (status === 401) {
        // Clear local storage data on authentication failure
        localStorage.removeItem('music_userId');
        localStorage.removeItem('user_nickname');
        localStorage.removeItem('netease_cookie');
        localStorage.removeItem('netease_cookie_timestamp');
        localStorage.removeItem('genre_cache');

        alert('登录状态已失效，请重新登录！');

        // Redirect to login page
        window.location.href = '/login';
      } else if (status === 500) {
        alert('服务器出现错误，请稍后再试！');
      } else {
        alert(`网络请求错误 (状态码: ${status})`);
      }
    } else {
      alert('网络连接失败，请检查网络设置。');
    }

    return Promise.reject(error);
  }
);

export default request;
