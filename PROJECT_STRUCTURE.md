# AI智能食谱管理系统 - 项目架构文档

## 📋 项目概述

基于AI的智能食谱管理系统，支持食材管理、智能推荐、社区分享、个性化定制等功能

## 🏗️ 技术栈

### 后端

- **框架**: Spring Boot 3.x
- **语言**: Kotlin
- **数据库**: MySQL 8.0 + Redis
- **AI接口**: 通义千问 (免费额度)
- **对象存储**: 阿里云OSS (可选本地存储)
- **认证**: JWT Token

### 前端 (Android)

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **本地数据库**: Room
- **网络请求**: Retrofit + OkHttp
- **图片加载**: Coil
- **相机**: CameraX

## 📁 项目结构

```
recipe-ai-system/
├── backend/                          # 后端项目
│   ├── src/main/kotlin/com/recipe/
│   │   ├── RecipeApplication.kt     # 启动类
│   │   ├── config/                  # 配置类
│   │   │   ├── SecurityConfig.kt    # 安全配置
│   │   │   ├── RedisConfig.kt       # Redis配置
│   │   │   └── AiConfig.kt          # AI接口配置
│   │   ├── controller/              # 控制器层
│   │   │   ├── UserController.kt    # 用户管理
│   │   │   ├── IngredientController.kt  # 食材管理
│   │   │   ├── ShoppingController.kt    # 采购管理
│   │   │   ├── RecipeController.kt      # 食谱社区
│   │   │   └── AiController.kt          # AI功能
│   │   ├── service/                 # 业务逻辑层
│   │   │   ├── UserService.kt
│   │   │   ├── IngredientService.kt
│   │   │   ├── ShoppingService.kt
│   │   │   ├── RecipeService.kt
│   │   │   ├── RecommendService.kt      # 智能推荐
│   │   │   └── AiService.kt             # AI服务
│   │   ├── repository/              # 数据访问层
│   │   │   ├── UserRepository.kt
│   │   │   ├── IngredientRepository.kt
│   │   │   ├── ShoppingRepository.kt
│   │   │   └── RecipeRepository.kt
│   │   ├── entity/                  # 实体类
│   │   │   ├── User.kt
│   │   │   ├── Ingredient.kt
│   │   │   ├── ShoppingItem.kt
│   │   │   └── Recipe.kt
│   │   ├── dto/                     # 数据传输对象
│   │   ├── ai/                      # AI模块
│   │   │   ├── TongYiAiClient.kt    # 通义千问客户端
│   │   │   ├── IngredientRecognizer.kt  # 食材识别
│   │   │   ├── RecipeGenerator.kt       # 食谱生成
│   │   │   └── VoiceGuideService.kt     # 语音指导
│   │   └── util/                    # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml          # 配置文件
│   │   └── db/migration/            # 数据库迁移脚本
│   └── pom.xml                      # Maven依赖
│
├── android/                         # Android项目
│   ├── app/src/main/
│   │   ├── java/com/recipe/
│   │   │   ├── MainActivity.kt      # 主活动
│   │   │   ├── ui/                  # UI层
│   │   │   │   ├── user/            # 用户模块UI
│   │   │   │   ├── ingredient/      # 食材模块UI
│   │   │   │   ├── shopping/        # 采购模块UI
│   │   │   │   ├── recipe/          # 食谱模块UI
│   │   │   │   └── ai/              # AI功能UI
│   │   │   ├── data/                # 数据层
│   │   │   │   ├── local/           # 本地数据库(Room)
│   │   │   │   ├── remote/          # 网络请求(Retrofit)
│   │   │   │   └── repository/      # 仓库模式
│   │   │   ├── viewmodel/           # ViewModel层
│   │   │   └── util/                # 工具类
│   │   └── res/                     # 资源文件
│   └── build.gradle.kts             # Gradle配置
│
└── docs/                            # 文档
    ├── API.md                       # API文档
    ├── DATABASE.md                  # 数据库设计
    └── DEPLOYMENT.md                # 部署文档
```

## 🔧 核心模块说明

### 1. 用户管理模块

- 注册/登录（JWT认证）
- 偏好标签设置
- AI学习画像管理

### 2. 食材管理模块

- AI拍照识别 + 手动录入
- 智能过期提醒（多优先级）
- 保存方式建议

### 3. 采购管理模块

- 智能采购量推荐
- 食谱食材一键导入
- 批量同步至食材库

### 4. 食谱社区模块

- 食谱CRUD
- AI质量评级(S/A/B/C)
- 语音烹饪指导

### 5. 智能推荐模块

- 临期食材优先推荐
- AI自学习算法
- 个性化权重调整

### 6. AI定制模块

- 多模态食材识别
- 专属食谱生成
- 智能优化建议

## 🚀 快速开始

### 后端启动

```bash
cd backend
mvn spring-boot:run
```

### Android编译

```bash
cd android
./gradlew assembleDebug
```

## 📝 AI接口配置

### 通义千问免费额度

- 每月100万tokens免费额度
- 支持图像识别、文本生成
- 需要注册阿里云账号获取API Key

配置位置: `backend/src/main/resources/application.yml`

## 🔐 环境变量

```yaml
TONGYI_API_KEY=your_api_key
MYSQL_HOST=localhost
MYSQL_PORT=3306
REDIS_HOST=localhost
```

## 📊 数据库设计

详见 `docs/DATABASE.md`

## 🌐 API文档

详见 `docs/API.md`

## 📦 部署说明

详见 `docs/DEPLOYMENT.md`