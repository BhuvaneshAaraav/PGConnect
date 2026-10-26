const API_ROOT = import.meta.env.VITE_API_ROOT || '';

async function parseResponse(response) {
  const payload = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(payload?.message || `Request failed (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return payload;
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const method = (options.method || 'GET').toUpperCase();
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = await fetch(`${API_ROOT}/api/auth/csrf`, { credentials: 'include' }).then(parseResponse);
    if (csrf?.token) headers.set(csrf.headerName || 'X-XSRF-TOKEN', csrf.token);
  }
  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    method,
    headers,
    credentials: 'include',
  });
  return parseResponse(response);
}

export async function apiUpload(path, fields, onProgress = () => {}) {
  const csrf = await fetch(`${API_ROOT}/api/auth/csrf`, { credentials: 'include' }).then(parseResponse);
  const body = new FormData();
  Object.entries(fields).forEach(([key, value]) => body.append(key, value));
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open('POST', `${API_ROOT}${path}`);
    request.withCredentials = true;
    if (csrf?.token) request.setRequestHeader(csrf.headerName || 'X-XSRF-TOKEN', csrf.token);
    request.upload.onprogress = (event) => { if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100)); };
    request.onload = () => {
      const payload = request.status === 204 ? null : (() => { try { return JSON.parse(request.responseText); } catch { return null; } })();
      if (request.status >= 200 && request.status < 300) resolve(payload);
      else { const error = new Error(payload?.message || `Request failed (${request.status})`); error.status = request.status; reject(error); }
    };
    request.onerror = () => reject(new Error('Upload failed. Check your connection and try again.'));
    request.send(body);
  });
}

export function queryString(values) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, value);
  });
  return params.toString();
}
