'use strict';

/* URL base da API: local em dev, Render em produção */
const API_BASE = (function () {
  const host = window.location.hostname;
  if (host === 'localhost' || host === '127.0.0.1') {
    return '';
  }
  return 'https://library-management-az9g.onrender.com';
})();

/* Armazenamento da sessão (token JWT + dados do usuário) */
const Session = {
  KEY: 'library.session',

  load() {
    try {
      const raw = localStorage.getItem(this.KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (error) {
      return null;
    }
  },

  save(session) {
    localStorage.setItem(this.KEY, JSON.stringify(session));
  },

  clear() {
    localStorage.removeItem(this.KEY);
  }
};

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/* Cliente HTTP mínimo para a API */
const Api = {
  async request(path, options = {}) {
    const { method = 'GET', body, auth = true } = options;
    const headers = { Accept: 'application/json' };

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }

    const session = Session.load();
    if (auth && session && session.token) {
      headers.Authorization = 'Bearer ' + session.token;
    }

    let response;
    try {
      response = await fetch(API_BASE + path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined
      });
    } catch (error) {
      throw new ApiError('Não foi possível conectar ao servidor.', 0);
    }

    if (response.status === 401 && auth) {
      Session.clear();
      throw new ApiError('Sua sessão expirou. Faça login novamente.', 401);
    }

    if (!response.ok) {
      throw new ApiError(await readErrorResponse(response), response.status);
    }

    if (response.status === 204) {
      return null;
    }

    const text = await response.text();
    return text ? JSON.parse(text) : null;
  },

  get(path) {
    return this.request(path);
  },

  post(path, body) {
    return this.request(path, { method: 'POST', body });
  },

  put(path, body) {
    return this.request(path, { method: 'PUT', body });
  },

  delete(path) {
    return this.request(path, { method: 'DELETE' });
  }
};

async function readErrorResponse(response) {
  const text = await response.text();
  if (!text) {
    return 'Erro ' + response.status;
  }

  try {
    const data = JSON.parse(text);
    if (typeof data === 'string') {
      return data;
    }
    if (data.errors && typeof data.errors === 'object') {
      const messages = Object.values(data.errors).filter(Boolean);
      if (messages.length) {
        return messages.join('; ');
      }
    }
    return data.detail || data.message || data.error || data.title || text;
  } catch (error) {
    return text;
  }
}