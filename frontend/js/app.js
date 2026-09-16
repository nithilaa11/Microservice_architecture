import { api } from './api.js';

// Application State
const state = {
  activeUser: null,
  users: [],
  products: [],
  orders: [],
  selectedProduct: null,
  activeTab: 'catalog',
  sagaInProgress: false
};

// Icons by product keywords
function getProductIcon(name = '') {
  const lower = name.toLowerCase();
  if (lower.includes('phone') || lower.includes('iphone')) return '📱';
  if (lower.includes('headphone') || lower.includes('sony') || lower.includes('audio')) return '🎧';
  if (lower.includes('laptop') || lower.includes('dell') || lower.includes('macbook')) return '💻';
  if (lower.includes('mouse') || lower.includes('logitech') || lower.includes('keyboard')) return '🖱️';
  if (lower.includes('watch')) return '⌚';
  return '📦';
}

// Format currency
function formatUSD(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

// Toast Notifications
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Log to Live API Inspector
function logApi(title, payload) {
  const inspector = document.getElementById('api-inspector-log');
  if (!inspector) return;
  const timestamp = new Date().toLocaleTimeString();
  const jsonStr = typeof payload === 'string' ? payload : JSON.stringify(payload, null, 2);
  inspector.textContent = `[${timestamp}] ${title}\n${jsonStr}\n\n` + inspector.textContent;
}

// Microservices Health Checks
async function checkAllServices() {
  const services = [
    { id: 'status-eureka', url: 'http://localhost:8761' },
    { id: 'status-gateway', url: 'http://localhost:8085/api/products' },
    { id: 'status-user', url: 'http://localhost:8081/api/users' },
    { id: 'status-product', url: 'http://localhost:8082/api/products' },
    { id: 'status-order', url: 'http://localhost:8083/api/orders' },
    { id: 'status-payment', url: 'http://localhost:8084/api/payments' }
  ];

  for (const s of services) {
    const el = document.getElementById(s.id);
    if (!el) continue;
    try {
      const res = await api.checkHealth(s.url);
      if (res.ok || res.status === 200 || res.status === 404) {
        el.className = 'service-status-badge status-up';
        el.innerHTML = `<span class="pulsing-dot" style="width:6px;height:6px"></span> UP (${res.latency || 10}ms)`;
      } else {
        el.className = 'service-status-badge status-down';
        el.textContent = 'DOWN';
      }
    } catch {
      el.className = 'service-status-badge status-down';
      el.textContent = 'DOWN';
    }
  }
}

// Load Users
async function loadUsers() {
  try {
    const users = await api.getUsers();
    state.users = users;
    const select = document.getElementById('user-select');
    select.innerHTML = '';
    
    users.forEach(u => {
      const opt = document.createElement('option');
      opt.value = u.id;
      opt.textContent = `${u.name} (${u.email})`;
      select.appendChild(opt);
    });

    if (users.length > 0) {
      state.activeUser = users[0];
      select.value = users[0].id;
      updateActiveUserUI();
    }
  } catch (err) {
    console.error('Error loading users:', err);
    showToast('Failed to connect to User Service via Gateway', 'error');
  }
}

function updateActiveUserUI() {
  if (!state.activeUser) return;
  const avatar = document.getElementById('user-avatar');
  const initials = state.activeUser.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  avatar.textContent = initials;
}

// Load Products
async function loadProducts() {
  const grid = document.getElementById('product-grid');
  grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">Fetching catalog from Product Service...</div>';

  try {
    const products = await api.getProducts();
    state.products = products;
    renderProducts(products);
    logApi('GET /api/products Success', products);
  } catch (err) {
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: #ef4444;">
      <p style="font-size: 1.2rem; font-weight: 600;">Unable to connect to Product Service</p>
      <p style="font-size: 0.85rem; margin-top: 0.5rem; color: var(--text-secondary);">${err.message}</p>
    </div>`;
  }
}

function renderProducts(products) {
  const grid = document.getElementById('product-grid');
  grid.innerHTML = '';

  if (products.length === 0) {
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">No products found in catalog.</div>';
    return;
  }

  products.forEach(p => {
    const card = document.createElement('div');
    card.className = 'product-card';

    const inStock = p.quantity > 0;
    const stockClass = p.quantity > 10 ? 'stock-in' : p.quantity > 0 ? 'stock-low' : 'stock-out';
    const stockText = p.quantity > 0 ? `${p.quantity} in stock` : 'Out of Stock';

    card.innerHTML = `
      <div class="product-media">
        <span class="product-icon-art">${getProductIcon(p.name)}</span>
        <span class="stock-tag ${stockClass}" id="stock-tag-${p.id}">${stockText}</span>
      </div>
      <div class="product-body">
        <h3 class="product-title">${escapeHtml(p.name)}</h3>
        <p class="product-desc">${escapeHtml(p.description || 'High-performance authentic device guaranteed.')}</p>
        <div class="product-meta">
          <div class="product-price">
            <span class="product-price-currency">$</span>${p.price.toFixed(2)}
          </div>
          <div style="display:flex; gap:0.5rem;">
            <button class="btn-icon" title="Check Live Stock" id="btn-check-stock-${p.id}">
              🔍
            </button>
            <button class="btn-buy" id="btn-buy-${p.id}" ${!inStock ? 'disabled' : ''}>
              🛒 Order Now
            </button>
          </div>
        </div>
      </div>
    `;

    // Event listeners
    card.querySelector(`#btn-check-stock-${p.id}`).addEventListener('click', () => handleCheckStock(p.id));
    const buyBtn = card.querySelector(`#btn-buy-${p.id}`);
    if (buyBtn) {
      buyBtn.addEventListener('click', () => openCheckoutModal(p));
    }

    grid.appendChild(card);
  });
}

// Check Live Stock
async function handleCheckStock(productId) {
  try {
    const stock = await api.checkStock(productId);
    logApi(`GET /api/products/${productId}/stock`, stock);
    showToast(`Stock for "${stock.productName}": ${stock.availableQuantity} units available`, 'info');
    
    // Update local card tag
    const tag = document.getElementById(`stock-tag-${productId}`);
    if (tag) {
      const stockClass = stock.availableQuantity > 10 ? 'stock-in' : stock.availableQuantity > 0 ? 'stock-low' : 'stock-out';
      tag.className = `stock-tag ${stockClass}`;
      tag.textContent = stock.availableQuantity > 0 ? `${stock.availableQuantity} in stock` : 'Out of Stock';
    }
  } catch (err) {
    showToast(`Stock check failed: ${err.message}`, 'error');
  }
}

// Load Orders
async function loadOrders() {
  const tbody = document.getElementById('orders-table-body');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 2rem; color:var(--text-muted);">Fetching orders...</td></tr>';

  try {
    const orders = await api.getOrders();
    state.orders = orders;
    renderOrders(orders);
    logApi('GET /api/orders Success', orders);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2rem; color:#ef4444;">Failed to load orders: ${err.message}</td></tr>`;
  }
}

function renderOrders(orders) {
  const tbody = document.getElementById('orders-table-body');
  tbody.innerHTML = '';

  if (orders.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 2rem; color:var(--text-muted);">No orders placed yet. Purchase an item to see the SAGA workflow!</td></tr>';
    return;
  }

  // Show newest orders first
  const sorted = [...orders].reverse();

  sorted.forEach(o => {
    const tr = document.createElement('tr');
    const dateFormatted = o.orderDate ? new Date(o.orderDate).toLocaleString() : 'Just now';
    const statusClass = o.status || 'PENDING';

    tr.innerHTML = `
      <td><strong>#${o.orderId}</strong></td>
      <td>
        <div style="font-weight:600;">${escapeHtml(o.userName || 'Customer')}</div>
        <div style="font-size:0.75rem; color:var(--text-muted);">${escapeHtml(o.userEmail || '')}</div>
      </td>
      <td>
        <div style="font-weight:500;">${escapeHtml(o.productName || `Product #${o.productId}`)}</div>
        <div style="font-size:0.75rem; color:var(--text-muted);">Qty: ${o.quantity}</div>
      </td>
      <td style="font-weight:700; color:white;">${formatUSD(o.totalAmount || 0)}</td>
      <td><span class="status-pill ${statusClass}">${statusClass}</span></td>
      <td><span class="code-badge">${o.paymentReference || o.paymentStatus || 'N/A'}</span></td>
      <td style="font-size:0.8rem; color:var(--text-secondary);">${dateFormatted}</td>
    `;
    tbody.appendChild(tr);
  });
}

// Checkout Modal & SAGA Orchestration
function openCheckoutModal(product) {
  state.selectedProduct = product;
  document.getElementById('checkout-product-title').textContent = product.name;
  document.getElementById('checkout-product-price').textContent = `$${product.price.toFixed(2)}`;
  document.getElementById('checkout-user-name').textContent = state.activeUser ? state.activeUser.name : 'Unknown';
  document.getElementById('checkout-quantity').value = '1';
  document.getElementById('checkout-quantity').max = product.quantity;
  document.getElementById('checkout-simulate-failure').checked = false;
  
  updateCheckoutTotal();
  resetSagaNodes();
  document.getElementById('checkout-modal').classList.add('active');
}

function closeCheckoutModal() {
  document.getElementById('checkout-modal').classList.remove('active');
  state.selectedProduct = null;
}

function updateCheckoutTotal() {
  if (!state.selectedProduct) return;
  const qty = parseInt(document.getElementById('checkout-quantity').value) || 1;
  const total = (state.selectedProduct.price * qty).toFixed(2);
  document.getElementById('checkout-total-price').textContent = `$${total}`;
}

function resetSagaNodes() {
  for (let i = 1; i <= 4; i++) {
    const node = document.getElementById(`saga-node-${i}`);
    if (node) {
      node.className = 'saga-step-node';
    }
  }
}

async function executeSagaCheckout() {
  if (!state.selectedProduct || !state.activeUser) {
    showToast('Please select a valid user and product', 'error');
    return;
  }

  const qty = parseInt(document.getElementById('checkout-quantity').value) || 1;
  const paymentMethod = document.getElementById('checkout-payment-method').value;
  const simulateFailure = document.getElementById('checkout-simulate-failure').checked;

  const btn = document.getElementById('btn-confirm-order');
  btn.disabled = true;
  btn.textContent = 'Orchestrating SAGA...';

  // Step 1: User Verification
  const n1 = document.getElementById('saga-node-1');
  const n2 = document.getElementById('saga-node-2');
  const n3 = document.getElementById('saga-node-3');
  const n4 = document.getElementById('saga-node-4');

  n1.className = 'saga-step-node active';
  await sleep(400);
  n1.className = 'saga-step-node success';

  // Step 2: Product Stock Check & Reservation
  n2.className = 'saga-step-node active';
  await sleep(400);
  n2.className = 'saga-step-node success';

  // Step 3: Payment Service Processing
  n3.className = 'saga-step-node active';

  const orderPayload = {
    userId: state.activeUser.id,
    productId: state.selectedProduct.id,
    quantity: qty,
    paymentMethod: paymentMethod,
    simulatePaymentFailure: simulateFailure
  };

  logApi('POST /api/orders (SAGA Initiated)', orderPayload);

  try {
    const result = await api.createOrder(orderPayload);
    logApi('POST /api/orders Response', result);

    if (result.status === 'CONFIRMED') {
      n3.className = 'saga-step-node success';
      n4.className = 'saga-step-node success';
      n4.querySelector('.saga-step-title').textContent = 'Order Confirmed';
      showToast(`🎉 Order #${result.orderId} Confirmed! Txn: ${result.paymentReference}`, 'success');
    } else {
      // Payment Failed - SAGA Compensation Triggered
      n3.className = 'saga-step-node failed';
      n4.className = 'saga-step-node failed';
      n4.querySelector('.saga-step-title').textContent = 'SAGA Compensated (Stock Restored)';
      showToast(`⚠️ Payment Failed: ${result.message || 'Product stock was restored by SAGA compensation'}`, 'error');
    }

    // Refresh products and orders
    await loadProducts();
    await loadOrders();

    setTimeout(() => {
      closeCheckoutModal();
      // Switch to orders tab
      switchTab('orders');
    }, 1800);

  } catch (err) {
    n3.className = 'saga-step-node failed';
    n4.className = 'saga-step-node failed';
    showToast(`Order failed: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Confirm & Place Order';
  }
}

// Tab Switching
function switchTab(tabId) {
  state.activeTab = tabId;
  document.querySelectorAll('.tab-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.tab === tabId);
  });
  document.querySelectorAll('.tab-pane').forEach(p => {
    p.classList.toggle('active', p.id === `tab-${tabId}`);
  });

  if (tabId === 'catalog') loadProducts();
  if (tabId === 'orders') loadOrders();
}

// User Registration Modal
function openUserModal() {
  document.getElementById('user-modal').classList.add('active');
}
function closeUserModal() {
  document.getElementById('user-modal').classList.remove('active');
}

async function handleRegisterUser(e) {
  e.preventDefault();
  const name = document.getElementById('reg-user-name').value.trim();
  const email = document.getElementById('reg-user-email').value.trim();
  const phone = document.getElementById('reg-user-phone').value.trim();

  if (!name || !email) {
    showToast('Name and email are required', 'error');
    return;
  }

  try {
    const created = await api.createUser({ name, email, phone });
    logApi('POST /api/users Created', created);
    showToast(`User "${created.name}" registered successfully!`, 'success');
    closeUserModal();
    await loadUsers();
  } catch (err) {
    showToast(`Registration failed: ${err.message}`, 'error');
  }
}

// Product Creation Modal
function openProductModal() {
  document.getElementById('product-modal').classList.add('active');
}
function closeProductModal() {
  document.getElementById('product-modal').classList.remove('active');
}

async function handleCreateProduct(e) {
  e.preventDefault();
  const name = document.getElementById('new-prod-name').value.trim();
  const description = document.getElementById('new-prod-desc').value.trim();
  const price = parseFloat(document.getElementById('new-prod-price').value);
  const quantity = parseInt(document.getElementById('new-prod-quantity').value);

  try {
    const created = await api.createProduct({ name, description, price, quantity });
    logApi('POST /api/products Created', created);
    showToast(`Product "${created.name}" added to catalog!`, 'success');
    closeProductModal();
    await loadProducts();
  } catch (err) {
    showToast(`Failed to add product: ${err.message}`, 'error');
  }
}

// Utilities
function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, tag => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
  }[tag] || tag));
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Initialization on DOMContentLoaded
window.addEventListener('DOMContentLoaded', () => {
  // Check services status
  checkAllServices();
  setInterval(checkAllServices, 15000); // Poll health every 15s

  // Initial data loading
  loadUsers();
  loadProducts();
  loadOrders();

  // Navigation tabs
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  // User Selector change
  document.getElementById('user-select').addEventListener('change', (e) => {
    const user = state.users.find(u => u.id == e.target.value);
    if (user) {
      state.activeUser = user;
      updateActiveUserUI();
      showToast(`Switched active customer to: ${user.name}`, 'info');
    }
  });

  // Product Search filter
  document.getElementById('product-search').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    const filtered = state.products.filter(p => 
      p.name.toLowerCase().includes(q) || (p.description && p.description.toLowerCase().includes(q))
    );
    renderProducts(filtered);
  });

  // Checkout modal events
  document.getElementById('btn-close-checkout').addEventListener('click', closeCheckoutModal);
  document.getElementById('checkout-quantity').addEventListener('input', updateCheckoutTotal);
  document.getElementById('btn-confirm-order').addEventListener('click', executeSagaCheckout);

  // User registration modal events
  document.getElementById('btn-open-user-modal').addEventListener('click', openUserModal);
  document.getElementById('btn-close-user-modal').addEventListener('click', closeUserModal);
  document.getElementById('user-form').addEventListener('submit', handleRegisterUser);

  // Product creation modal events
  document.getElementById('btn-open-product-modal').addEventListener('click', openProductModal);
  document.getElementById('btn-close-product-modal').addEventListener('click', closeProductModal);
  document.getElementById('product-form').addEventListener('submit', handleCreateProduct);

  // SAGA Visualizer interactive test buttons
  document.getElementById('btn-test-saga-success')?.addEventListener('click', () => {
    if (state.products.length > 0) openCheckoutModal(state.products[0]);
  });
  document.getElementById('btn-test-saga-failure')?.addEventListener('click', () => {
    if (state.products.length > 0) {
      openCheckoutModal(state.products[0]);
      document.getElementById('checkout-simulate-failure').checked = true;
    }
  });
});
