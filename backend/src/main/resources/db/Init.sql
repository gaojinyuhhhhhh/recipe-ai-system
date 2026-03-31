-- AI智能食谱管理系统 - 数据库初始化脚本
-- 数据库: recipe_ai

CREATE DATABASE IF NOT EXISTS recipe_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE recipe_ai;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    phone VARCHAR(11) UNIQUE COMMENT '手机号',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    preferences TEXT COMMENT '偏好标签JSON',
    ai_profile TEXT COMMENT 'AI学习画像JSON',
    family_size INT DEFAULT 1 COMMENT '家庭人数',
    cooking_frequency INT DEFAULT 7 COMMENT '烹饪频率(次/周)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '账号状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 食材表
CREATE TABLE IF NOT EXISTS ingredients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '食材名称',
    category VARCHAR(50) COMMENT '类别',
    quantity DOUBLE COMMENT '数量',
    unit VARCHAR(20) COMMENT '单位',
    freshness VARCHAR(20) NOT NULL DEFAULT 'FRESH' COMMENT '新鲜度',
    purchase_date DATE NOT NULL COMMENT '购买日期',
    expiry_date DATE COMMENT '保质期',
    shelf_life INT COMMENT '保质天数',
    storage_method VARCHAR(20) COMMENT '保存方式',
    storage_advice TEXT COMMENT '保存建议',
    image_url VARCHAR(255) COMMENT '图片URL',
    notes TEXT COMMENT '备注',
    is_consumed BOOLEAN DEFAULT FALSE COMMENT '是否已消耗',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材表';

-- 采购清单表
CREATE TABLE IF NOT EXISTS shopping_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '食材名称',
    category VARCHAR(50) COMMENT '类别',
    quantity DOUBLE COMMENT '推荐采购量',
    unit VARCHAR(20) COMMENT '单位',
    ai_advice TEXT COMMENT 'AI采购建议',
    recipe_id BIGINT COMMENT '关联食谱ID',
    is_completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_completed (is_completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购清单表';

-- 食谱表
CREATE TABLE IF NOT EXISTS recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '创建者ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    description TEXT COMMENT '描述',
    cover_image VARCHAR(255) COMMENT '封面图',
    ingredients TEXT NOT NULL COMMENT '食材清单JSON',
    steps TEXT NOT NULL COMMENT '步骤JSON',
    cooking_time INT COMMENT '烹饪时长(分钟)',
    difficulty VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '难度',
    cuisine VARCHAR(50) COMMENT '菜系',
    tags TEXT COMMENT '标签JSON',
    ai_rating VARCHAR(10) COMMENT 'AI评级',
    ai_rating_detail TEXT COMMENT 'AI评分详情JSON',
    ai_suggestion TEXT COMMENT 'AI优化建议',
    is_ai_optimized BOOLEAN DEFAULT FALSE COMMENT '是否AI优化版',
    original_recipe_id BIGINT COMMENT '原始食谱ID',
    view_count BIGINT DEFAULT 0 COMMENT '浏览量',
    favorite_count BIGINT DEFAULT 0 COMMENT '收藏量',
    comment_count BIGINT DEFAULT 0 COMMENT '评论量',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_cuisine (cuisine),
    INDEX idx_difficulty (difficulty),
    INDEX idx_public (is_public),
    FULLTEXT idx_search (title, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食谱表';

-- 食谱评论表
CREATE TABLE IF NOT EXISTS recipe_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL COMMENT '食谱ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    rating INT COMMENT '评分1-5',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_recipe_id (recipe_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食谱评论表';

-- 食谱收藏表
CREATE TABLE IF NOT EXISTS recipe_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL COMMENT '食谱ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_recipe (user_id, recipe_id),
    INDEX idx_recipe_id (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食谱收藏表';

-- 插入测试数据
INSERT INTO users (username, password, nickname, family_size, cooking_frequency) VALUES
('testuser', '$2a$10$abcdefghijklmnopqrstuvwxyz', '测试用户', 3, 5),
('admin', '$2a$10$adminpasswordhash', '管理员', 1, 7);

INSERT INTO ingredients (user_id, name, category, freshness, purchase_date, expiry_date, shelf_life, storage_method) VALUES
(1, '西红柿', '蔬菜类', 'FRESH', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 5, 'REFRIGERATE'),
(1, '鸡蛋', '肉蛋类', 'FRESH', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 30, 'REFRIGERATE'),
(1, '生菜', '蔬菜类', 'WILTING', DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 'REFRIGERATE');

COMMIT;