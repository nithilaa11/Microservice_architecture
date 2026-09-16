-- =======================================================
-- Microservices Architecture - E-Commerce Database Setup
-- Database Per Service Pattern Demonstration
-- =======================================================

-- 1. USER SERVICE DATABASE
CREATE DATABASE IF NOT EXISTS ecommerce_users;
USE ecommerce_users;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL
);

-- Sample Data for User Service
INSERT INTO users (name, email, phone) VALUES
('Alice Johnson', 'alice@example.com', '9876543210'),
('Bob Smith', 'bob@example.com', '9876543211'),
('Charlie Brown', 'charlie@example.com', '9876543212')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- -------------------------------------------------------

-- 2. PRODUCT / INVENTORY SERVICE DATABASE
CREATE DATABASE IF NOT EXISTS ecommerce_products;
USE ecommerce_products;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL
);

-- Sample Data for Product Service
INSERT INTO products (name, description, price, quantity) VALUES
('Apple iPhone 15', 'Latest 128GB Midnight Blue flagship smartphone', 799.99, 50),
('Sony WH-1000XM5', 'Wireless Noise Cancelling Headphones, Black', 349.99, 30),
('Dell XPS 15 Laptop', '15.6 inch OLED, Intel Core i7, 16GB RAM, 512GB SSD', 1499.99, 15),
('Logitech MX Master 3S', 'Ergonomic performance wireless mouse with quiet clicks', 99.99, 100)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- -------------------------------------------------------

-- 3. ORDER SERVICE DATABASE
CREATE DATABASE IF NOT EXISTS ecommerce_orders;
USE ecommerce_orders;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------

-- 4. PAYMENT SERVICE DATABASE
CREATE DATABASE IF NOT EXISTS ecommerce_payments;
USE ecommerce_payments;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
