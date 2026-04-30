# 识材智厨——智能厨房助手

一款基于 **通义千问大模型** 的智能食谱管理系统，包含 Android 客户端 + Spring Boot 后端，实现食材管理、AI 食谱生成、拍照识别食材、智能推荐、采购清单等功能。

---

## 功能概览

| 模块 | 主要功能 |
|------|--------|
| **食材管理** | 手动/批量/拍照添加食材，按新鲜度/类别分组展示，过期提醒，消耗标记 |
| **AI 食谱生成** | 根据已有食材智能生成菜谱，支持自定义口味、难度、菜系等参数 |
| **拍照识别** | 基于通义千问视觉模型，拍照自动识别食材并推断保质期、保存方式 |
| **食谱社区** | 浏览/搜索/创建/收藏食谱，评论互动，AI 优化建议 |
| **智能推荐** | 基于用户食材库和偏好，智能推荐可做的菜品组合 |
| **采购清单** | 从食谱一键导入食材，标记完成后同步到食材库 |
| **AI 对话助手** | 厨房相关的自由对话，结合用户食材库提供个性化建议 |
| **烹饪模式** | 分步语音播报烹饪指导，步骤倒计时提醒，语音聊天交互 |
| **用户行为学习** | 自动分析用户偏好画像，推荐越用越准 |

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 主框架 |
| Kotlin | 1.9.21 | 开发语言 |
| Java | 17 | JDK 版本 |
| MySQL | 8.x | 数据库 |
| Redis | 6.x+ | 缓存（推荐结果、会话等） |
| Spring Security + JWT | — | 认证鉴权 |
| Spring Data JPA | — | ORM |
| OkHttp | 4.12.0 | AI API 调用 |

### Android 客户端

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.20 | 开发语言 |
| Jetpack Compose | 1.5.4 | 声明式 UI 框架 |
| Material3 | 1.1.2 | 设计系统 |
| Retrofit2 | 2.9.0 | 网络请求 |
| Room | 2.6.0 | 本地数据库 |
| CameraX | 1.3.0 | 拍照识别 |
| Navigation Compose | 2.7.5 | 页面导航 |
| Coil | 2.5.0 | 图片加载 |

### AI 能力

| 模型 | 用途 |
|------|------|
| `qwen-turbo` | 文本生成（食谱生成、对话、推荐等） |
| `qwen-vl-plus` | 视觉识别（拍照识别食材） |
| `cosyvoice-v1` | 语音合成（烹饪模式语音播报） |

---

## 快速开始（从零运行完整项目）

下面是完整的从零搭建步骤，**按顺序执行**即可在本地跑通整个项目。

---

### 第一步：安装必需软件

#### 1.1 安装 JDK 17

- 下载：[Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) 或 [Adoptium OpenJDK 17](https://adoptium.net/)
- 安装后打开终端验证：
  ```bash
  java -version
  # 输出应包含 "17.x.x"
  ```
- 确认 `JAVA_HOME` 环境变量已设置，指向 JDK 安装目录

#### 1.2 安装 Maven（3.8+）

- 下载：[Maven 官网](https://maven.apache.org/download.cgi)，选择 Binary zip archive
- 解压到任意目录（如 `D:\apache-maven-3.9.6`）
- 添加环境变量：
  - `M2_HOME` = Maven 解压目录
  - `Path` 中追加 `%M2_HOME%\bin`
- 验证：
  ```bash
  mvn -version
  # 输出应包含 Maven 版本和 Java 17
  ```

#### 1.3 安装 MySQL 8.x

- 下载：[MySQL Community Server](https://dev.mysql.com/downloads/mysql/)，推荐 MSI 安装包
- 安装时**记住你设置的 root 密码**（后面配置要用）
- 安装完成后确认 MySQL 服务已启动：
  ```bash
  mysql -u root -p
  # 输入密码后能进入 mysql> 命令行即可
  ```

#### 1.4 安装 Redis

- **Windows 推荐方案**：
  - 方案一：使用 [Memurai](https://www.memurai.com/)（Windows 原生 Redis 兼容实现）
  - 方案二：使用 [tporadowski/redis](https://github.com/tporadowski/redis/releases) Windows 移植版
  - 方案三：通过 WSL2 安装 Linux 版 Redis
- 安装后验证：
  ```bash
  redis-cli ping
  # 输出 PONG 表示正常
  ```
- 如果你给 Redis 设了密码，记住它；如果没设密码，后面配置可以留空

#### 1.5 安装 Android Studio

- 下载：[Android Studio 官网](https://developer.android.com/studio)（建议 Hedgehog 2023.1.1 或更新版本）
- 安装后首次启动，按向导完成 SDK 安装
- 打开 `SDK Manager`（Settings → Languages & Frameworks → Android SDK）：
  - **SDK Platforms** 页签：勾选 `Android 15.0 (VanillaIceCream)` — API Level 35
  - **SDK Tools** 页签：确认已安装 `Android SDK Build-Tools`、`Android SDK Platform-Tools`
  - 点击 Apply 下载

#### 1.6 获取通义千问 API Key

1. 访问 [通义千问 DashScope 控制台](https://dashscope.console.aliyun.com/)
2. 注册/登录阿里云账号
3. 开通 **DashScope 灵积模型服务**
4. 进入 **API Key 管理** 页面，点击「创建新的 API Key」
5. 复制生成的 `sk-xxxx...` 密钥，**妥善保存**

---

### 第二步：克隆项目

```bash
git clone https://github.com/gaojinyuhhhhhh/recipe-ai-system.git
cd recipe-ai-system
```

---

### 第三步：启动后端服务

#### 3.1 初始化数据库

打开 MySQL 客户端（命令行 或 Navicat/DataGrip 等工具），执行初始化脚本：

```bash
mysql -u root -p < backend/src/main/resources/db/Init.sql
```

或者手动操作：
1. 连接 MySQL：`mysql -u root -p`，输入密码
2. 复制 `backend/src/main/resources/db/Init.sql` 文件的全部内容
3. 粘贴到 MySQL 命令行中执行
4. 验证：
   ```sql
   SHOW DATABASES;
   -- 应能看到 recipe_ai
   
   USE recipe_ai;
   SHOW TABLES;
   -- 应能看到 users, ingredients, recipes, shopping_items, recipe_comments, recipe_favorites 共6张表
   ```

#### 3.2 创建本地配置文件

> **关键步骤**：`application-local.yml` 包含密码和 API Key 等敏感信息，已被 `.gitignore` 排除，**不会出现在仓库中**，需要你手动创建。

在 `backend/src/main/resources/` 目录下**新建文件** `application-local.yml`，内容如下：

```yaml
# 本地开发环境配置（此文件不要提交到 Git）

spring:
  # 修改为你的 MySQL root 密码
  datasource:
    password: 你的MySQL密码

  # 修改为你的 Redis 密码（如果没设密码，整个 redis 段可以删掉）
  data:
    redis:
      password: 你的Redis密码

# 通义千问 API Key（第一步获取的 sk-xxxx）
ai:
  tongyi:
    api-key: sk-你的API密钥
```

将上面 3 处 **「你的xxx」** 替换为你的真实值。

#### 3.3 启动后端

```bash
cd backend

# 方式一：Maven 插件直接启动（推荐，开发调试用）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 方式二：先编译打包，再用 java 运行
mvn clean package -DskipTests
java -jar target/recipe-ai-backend-1.0.0.jar --spring.profiles.active=local
```

#### 3.4 验证后端启动成功

看到类似以下日志说明启动成功：
```
Started RecipeApplicationKt in x.xxx seconds
```

在浏览器或终端验证：
```bash
# 健康检查
curl http://localhost:8080/api/health

# 或直接浏览器打开 http://localhost:8080/api/health
# 返回类似 {"success":true, ...} 即为正常
```

如果启动失败，检查：
- MySQL 服务是否在运行、密码是否正确
- Redis 服务是否在运行、密码是否正确
- `application-local.yml` 是否在正确的目录下
- 端口 8080 是否被占用（可在 `application-local.yml` 中添加 `server.port: 你的端口` 覆盖）

---

### 第四步：运行 Android 客户端

#### 4.1 用 Android Studio 打开项目

1. 打开 Android Studio
2. 选择 `Open` → 选择 `recipe-ai-system/android` 目录
3. 等待 Gradle Sync 完成（首次会比较久，需要下载依赖）
   - 如果提示 Gradle Wrapper 相关文件缺失，Android Studio 会自动提示创建
   - Gradle 版本为 **8.9**，AGP 版本为 **8.5.0**（已配置阿里云镜像加速下载）
4. Sync 完成后，项目目录结构正常显示，无红色报错即可

> **Sync 失败常见处理**：
> - 如果报 SDK 路径问题：Android Studio 会自动生成 `local.properties` 并填入你本机 SDK 路径
> - 如果报网络超时：项目已配置阿里云 Maven 镜像，检查你的网络连接即可

#### 4.2 配置后端连接地址

编辑 `android/app/src/main/java/com/recipe/data/remote/ApiService.kt`，修改 `BASE_URL`：

```kotlin
companion object {
    // ▼▼▼ 根据你的运行方式选择一个，取消注释 ▼▼▼

    // 【Android 模拟器】连接本机后端（10.0.2.2 是模拟器访问宿主机的特殊 IP）
    const val BASE_URL = "http://10.0.2.2:8080/api/"

    // 【真机 USB/WiFi 调试】改为你电脑的局域网 IP
    // const val BASE_URL = "http://192.168.x.x:8080/api/"
}
```

**如何查看电脑局域网 IP（真机调试需要）**：
```bash
# Windows
ipconfig
# 找到「无线局域网适配器 WLAN」下的 IPv4 地址，如 192.168.1.100

# Mac/Linux
ifconfig | grep inet
```

#### 4.3 网络安全配置（真机调试额外步骤）

如果使用**真机调试**，还需要在 `android/app/src/main/res/xml/network_security_config.xml` 中添加你的电脑 IP，确保 HTTP 明文流量被允许：

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">192.168.x.x</domain>  <!-- 替换为你的电脑IP -->
    </domain-config>
</network-security-config>
```

> **注意**：确保手机和电脑连接在**同一 WiFi 网络**下。

#### 4.4 运行 App

**方式一：Android Studio 图形化运行（推荐）**
1. 连接手机（开启 USB 调试）或启动模拟器
2. 顶部工具栏选择目标设备
3. 点击绿色 ▶ Run 按钮
4. 等待编译安装完成，App 自动打开

**方式二：命令行构建**
```bash
cd android

# 构建 Debug APK
.\gradlew.bat assembleDebug

# 构建产物位于：android/app/build/outputs/apk/debug/app-debug.apk
# 手动安装到手机即可
```

#### 4.5 验证 App 运行正常

1. App 打开后进入登录页面
2. 点击「注册」创建新账号（或使用测试账号 `testuser`，注意：Init.sql 中的密码是加密的哈希值，建议自行注册新账号）
3. 登录后进入主界面，能看到食材管理、食谱社区、采购清单等模块
4. 尝试添加一个食材 → 进入 AI 生成食谱 → 验证 AI 功能正常

---

### 第五步：防火墙放行（真机调试必需）

如果真机调试时 App 网络请求失败，需要放行后端端口：

**Windows**：
1. 打开「Windows 防火墙」→「高级设置」→「入站规则」→「新建规则」
2. 选择「端口」→ TCP → 特定本地端口输入 `8080`
3. 选择「允许连接」→ 全部勾选 → 命名如 `Recipe AI Backend`

---

## 项目结构

```
recipe-ai-system/
├── android/                    # Android 客户端
│   ├── app/src/main/
│   │   ├── java/com/recipe/
│   │   │   ├── data/           # 数据层（Model, API, Room, Repository）
│   │   │   ├── ui/             # UI 层（Screen, Dialog, Components）
│   │   │   ├── viewmodel/      # ViewModel 层（核心业务逻辑）
│   │   │   ├── util/           # 工具类
│   │   │   └── MainActivity.kt # 导航入口
│   │   ├── res/                # 资源文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts        # 模块级构建配置
│   └── settings.gradle.kts     # 项目设置（含阿里云镜像配置）
│
├── backend/                    # Spring Boot 后端
│   ├── src/main/
│   │   ├── kotlin/com/recipe/
│   │   │   ├── ai/             # AI 能力封装（通义千问客户端、食材识别、食谱生成、语音合成）
│   │   │   ├── config/         # 配置（Security, JWT, Redis）
│   │   │   ├── controller/     # REST 控制器（9个）
│   │   │   ├── dto/            # 数据传输对象
│   │   │   ├── entity/         # JPA 实体
│   │   │   ├── event/          # 事件（用户行为记录）
│   │   │   ├── repository/     # 数据仓库
│   │   │   └── service/        # 业务逻辑（7个）
│   │   └── resources/
│   │       ├── db/Init.sql     # 数据库初始化脚本（6张表）
│   │       ├── application.yml # 主配置
│   │       └── application-local.yml  # 本地敏感配置（需手动创建，不在Git中）
│   └── pom.xml                 # Maven 构建配置
│
└── README.md
```

---

## API 接口概览

后端基础地址：`http://localhost:8080/api/`

| 模块 | 路径前缀 | 主要接口 |
|------|----------|----------|
| 用户认证 | `/users/` | 注册、登录、个人信息、偏好设置、修改密码 |
| 食材管理 | `/ingredients/` | 增删改查、批量添加、过期提醒、分组查询、拍照识别 |
| 食谱社区 | `/recipes/` | CRUD、搜索、收藏、评论、克隆、AI优化、本地同步 |
| 采购清单 | `/shopping/` | 增删、批量完成、同步到食材库、食谱导入 |
| AI 功能 | `/ai/` | 食谱生成、对话、食材识别、食材信息推断 |
| 智能推荐 | `/recommend/` | 基于用户行为+食材状态的智能推荐 |
| 用户行为 | `/user-behavior/` | 用户画像、偏好分析、智能偏好建议 |
| 健康检查 | `/health` | 服务健康状态 |

> 所有接口（除注册/登录/健康检查外）需在 Header 中携带 `Authorization: Bearer <token>`。

---

## 快速验证

后端启动后，可用以下命令验证接口：

```bash
# 健康检查
curl http://localhost:8080/api/health

# 注册新用户
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test\",\"password\":\"123456\"}"

# 登录（返回 token）
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test\",\"password\":\"123456\"}"
```

> **Windows PowerShell 用户注意**：PowerShell 中 `curl` 实际是 `Invoke-WebRequest` 的别名，语法不同。可以直接用浏览器访问 `http://localhost:8080/api/health` 验证，或使用 Postman / Apifox 等工具测试接口。

---

## 常见问题

### Q: Gradle Sync 失败 / 依赖下载超时？

项目已在 `settings.gradle.kts` 中配置了阿里云 Maven 镜像加速。如果仍然失败：
1. 检查网络连接是否正常
2. 在 Android Studio 中 File → Invalidate Caches → 重启
3. 删除 `android/.gradle` 和 `~/.gradle/caches` 目录后重新 Sync

### Q: Android 真机调试网络请求失败？

按以下顺序排查：
1. 确认手机和电脑在同一 WiFi
2. 确认 `ApiService.kt` 中 `BASE_URL` 使用了正确的电脑 IP
3. 确认 `network_security_config.xml` 中添加了该 IP
4. 确认 Windows 防火墙允许 8080 端口入站（见「第五步」）
5. 在手机浏览器中直接访问 `http://电脑IP:8080/api/health` 确认能通

### Q: 后端启动报数据库连接失败？

1. 确认 MySQL 服务已启动：`mysql -u root -p` 能正常连接
2. 确认已执行 `Init.sql` 创建了 `recipe_ai` 数据库
3. 确认 `application-local.yml` 中的数据库密码与你 MySQL 的 root 密码一致
4. 确认 MySQL 端口是默认的 3306（如果改过端口，需在 `application-local.yml` 中覆盙 `datasource.url`）

### Q: 后端启动报 Redis 连接失败？

1. 确认 Redis 服务已启动：`redis-cli ping` 返回 PONG
2. 如果 Redis 设了密码，确认 `application-local.yml` 中密码正确
3. 如果 Redis 没设密码，确保 `application-local.yml` 中的 `password` 字段为空或删除该配置项

### Q: AI 功能返回错误？

1. 确认 `application-local.yml` 中配置了有效的通义千问 API Key（`sk-` 开头）
2. 确认网络可以访问 `https://dashscope.aliyuncs.com`
3. 确认 DashScope 账户有足够的调用额度（新用户有免费额度）

### Q: 模拟器无法连接后端？

1. 确认后端已启动且能访问 `http://localhost:8080/api/health`
2. 确认 `ApiService.kt` 中 `BASE_URL` 使用的是 `10.0.2.2`（模拟器专用地址）
3. 确认 `network_security_config.xml` 中包含 `10.0.2.2`

---

## 代码注释说明

项目核心代码已添加详细注释，重点覆盖以下文件：

### Android 客户端

| 文件路径 | 注释内容 |
|---------|--------|
| `viewmodel/RecipeViewModel.kt` | 类职责、数据流、syncStatus状态机、社区/本地食谱操作 |
| `viewmodel/IngredientViewModel.kt` | 食材分组逻辑、AI识别流程、乐观更新模式 |
| `viewmodel/ShoppingViewModel.kt` | 采购流程、AI智能入库、批量操作 |
| `viewmodel/RecipeDetailViewModel.kt` | AI生成/导入/发布流程、多格式解析 |
| `viewmodel/ChatViewModel.kt` | 对话上下文维护、会话管理 |
| `MainActivity.kt` | 导航架构、Base64参数编码 |
| `data/remote/ApiService.kt` | 接口分区说明（6大模块） |

### 后端

| 文件路径 | 注释内容 |
|---------|--------|
| `service/RecipeService.kt` | 食谱CRUD、搜索、收藏、评论业务 |
| `service/IngredientService.kt` | 食材管理、过期提醒、分组查询 |
| `service/AiService.kt` | AI食谱生成、对话、识别、推荐 |
| `config/SecurityConfig.kt` | JWT认证配置、权限控制 |

