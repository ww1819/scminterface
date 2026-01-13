// 公共工具函数

// 获取token
function getToken() {
    return localStorage.getItem('token');
}

// 设置请求头
function getHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
}

// 通用请求函数
async function request(url, options = {}) {
    const defaultOptions = {
        headers: getHeaders()
    };
    
    const finalOptions = { ...defaultOptions, ...options };
    
    if (finalOptions.body && typeof finalOptions.body === 'object') {
        finalOptions.body = JSON.stringify(finalOptions.body);
    }

    try {
        const response = await fetch(url, finalOptions);
        const result = await response.json();
        
        if (result.code === 401) {
            // token过期，跳转到登录页
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            window.location.href = '/login.html';
            return null;
        }
        
        return result;
    } catch (error) {
        console.error('Request error:', error);
        return { code: 500, msg: '网络错误，请稍后重试' };
    }
}

// GET请求
async function get(url) {
    return request(url, { method: 'GET' });
}

// POST请求
async function post(url, data) {
    return request(url, {
        method: 'POST',
        body: data
    });
}

// DELETE请求
async function del(url) {
    return request(url, { method: 'DELETE' });
}

// 显示消息提示
function showMessage(message, type = 'success') {
    // 简单的alert实现，可以后续替换为更美观的提示组件
    if (type === 'success') {
        alert(message);
    } else {
        alert(message);
    }
}
