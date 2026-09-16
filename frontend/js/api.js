/**
 * API Service Client
 * Routes all requests through the Spring Cloud API Gateway (Port 8085)
 */
const GATEWAY_URL = 'http://localhost:8085';

export const api = {
  // Service Discovery & Direct Health Checks
  async checkHealth(url) {
    try {
      const start = performance.now();
      const res = await fetch(url, { method: 'GET', mode: 'cors' });
      const latency = Math.round(performance.now() - start);
      return { ok: res.ok, status: res.status, latency };
    } catch (err) {
      return { ok: false, error: err.message, latency: 0 };
    }
  },

  // Products (Product Service via Gateway)
  async getProducts() {
    const res = await fetch(`${GATEWAY_URL}/api/products`);
    if (!res.ok) throw new Error(`Failed to fetch products: ${res.statusText}`);
    return await res.json();
  },

  async getProductById(id) {
    const res = await fetch(`${GATEWAY_URL}/api/products/${id}`);
    if (!res.ok) throw new Error(`Product not found with id ${id}`);
    return await res.json();
  },

  async checkStock(id) {
    const res = await fetch(`${GATEWAY_URL}/api/products/${id}/stock`);
    if (!res.ok) throw new Error(`Failed to check stock for id ${id}`);
    return await res.json();
  },

  async createProduct(productData) {
    const res = await fetch(`${GATEWAY_URL}/api/products`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(productData)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to create product');
    }
    return await res.json();
  },

  // Users (User Service via Gateway)
  async getUsers() {
    const res = await fetch(`${GATEWAY_URL}/api/users`);
    if (!res.ok) throw new Error(`Failed to fetch users: ${res.statusText}`);
    return await res.json();
  },

  async createUser(userData) {
    const res = await fetch(`${GATEWAY_URL}/api/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userData)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to register customer');
    }
    return await res.json();
  },

  // Orders (Order Orchestration & SAGA via Gateway)
  async getOrders() {
    const res = await fetch(`${GATEWAY_URL}/api/orders`);
    if (!res.ok) throw new Error(`Failed to fetch orders: ${res.statusText}`);
    return await res.json();
  },

  async createOrder(orderPayload) {
    const res = await fetch(`${GATEWAY_URL}/api/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(orderPayload)
    });
    
    const data = await res.json();
    if (!res.ok && res.status !== 400 && res.status !== 503) {
      throw new Error(data.message || 'Failed to place order');
    }
    return data;
  },

  async cancelOrder(orderId) {
    const res = await fetch(`${GATEWAY_URL}/api/orders/${orderId}/cancel`, {
      method: 'PUT'
    });
    if (!res.ok) throw new Error(`Failed to cancel order ${orderId}`);
    return await res.json();
  },

  // Payments (Payment Service via Gateway)
  async getPayments() {
    const res = await fetch(`${GATEWAY_URL}/api/payments`);
    if (!res.ok) throw new Error(`Failed to fetch payments: ${res.statusText}`);
    return await res.json();
  }
};
