-- ==============================================================
-- Real-Time Chat Application - Database Schema
-- Compatible with MySQL 8.0+ / MariaDB
-- Character set: utf8mb4 (supports emojis and international text)
-- ==============================================================

CREATE DATABASE IF NOT EXISTS realtime_chat
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE realtime_chat;

-- --------------------------------------------------------------
-- 1. Table: users
-- Stores registered accounts and hashed credentials (BCrypt)
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------
-- 2. Table: conversations
-- Represents 1-to-1 direct chats or multi-user group chat rooms
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------
-- 3. Table: conversation_participants
-- Junction table mapping users to the conversations they belong to
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_cp_conversation 
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_cp_user 
        FOREIGN KEY (user_id) REFERENCES users (id) 
        ON DELETE CASCADE,
    INDEX idx_cp_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------
-- 4. Table: messages
-- Stores chat history linked to conversations and senders
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_conversation 
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender 
        FOREIGN KEY (sender_id) REFERENCES users (id) 
        ON DELETE CASCADE,
    INDEX idx_messages_convo_created (conversation_id, created_at),
    INDEX idx_messages_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
