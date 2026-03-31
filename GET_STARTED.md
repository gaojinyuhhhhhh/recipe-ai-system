# AI智能食谱管理系统 - 快速开始指南

## 📦 项目完整说明

这是一个基于 **Spring Boot + Kotlin + 通义千问AI** 的智能食谱管理系统，包含以下核心功能：

1. ✅ **用户管理** - 注册登录、偏好设置、AI画像
2. ✅ **食材管理** - AI拍照识别、智能过期提醒、保存建议
3. ✅ **采购管理** - 智能采购量推荐、食谱食材导入
4. ✅ **食谱社区** - 食谱CRUD、AI质量评级、语音指导
5. ✅ **智能推荐** - 临期食材优先、个性化推荐
6. ✅ **AI定制** - 专属食谱生成、智能优化

---

## 🚀 快速部署

### 1. 环境准备

#### 必需软件
```bash
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
```

#### 可选软件
```bash
- Docker (用于容器化部署)
- Android Studio (开发Android端)
```

### 2. 获取通义千问API Key

1. 访问 [阿里云控制台](https://dashscope.console.aliyun.com/)
2. 注册/登录账号
3. 开通 "灵积模型服务"（DashScope）
4. 创建 API Key
5. 复制API Key备用

**免费额度：**
- 每月 100万 tokens 免费
- 支持图像识别、文本生成
- 足够开发和小规模使用

### 3. 数据库配置

```bash
# 启动MySQL
mysql -u root -p

# 创建数据库
source backend/src/main/resources/db/init.sql
```

或者直接执行：
```sql
CREATE DATABASE recipe_ai CHARACTER SET utf8mb4;
```

### 4. 配置文件修改

编辑 `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/recipe_ai
    username: root
    password: YOUR_MYSQL_PASSWORD  # 改成你的密码

ai:
  tongyi:
    api-key: sk-xxxxxxxxxxxxx  # 改成你的通义千问API Key
```

### 5. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

启动成功后访问：
- API地址: http://localhost:8080/api
- 健康检查: http://localhost:8080/api/health

---

## 📱 Android端开发

### 项目结构
```
android/
├── app/
│   ├── build.gradle.kts           # 依赖配置
│   └── src/main/
│       ├── AndroidManifest.xml    # 权限配置
│       ├── java/com/recipe/
│       │   ├── MainActivity.kt    # 主活动
│       │   ├── ui/                # UI组件
│       │   ├── data/              # 数据层
│       │   └── viewmodel/         # ViewModel
│       └── res/                   # 资源文件
```

### 核心依赖 (build.gradle.kts)

```kotlin
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    
    // Room数据库
    implementation("androidx.room:room-runtime:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Retrofit网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // CameraX拍照
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // Coil图片加载
    implementation("io.coil-kt:coil-compose:2.5.0")
}
```

### API调用示例

```kotlin
// Retrofit接口定义
interface RecipeApiService {
    @Multipart
    @POST("ingredients/recognize")
    suspend fun recognizeIngredient(
        @Header("user-id") userId: Long,
        @Part("imageBase64") imageBase64: String
    ): ApiResponse<List<Ingredient>>
    
    @GET("ingredients")
    suspend fun getIngredients(
        @Header("user-id") userId: Long
    ): ApiResponse<List<Ingredient>>
}

// ViewModel中使用
class IngredientViewModel : ViewModel() {
    fun recognizeImage(imageBase64: String) {
        viewModelScope.launch {
            try {
                val response = apiService.recognizeIngredient(userId, imageBase64)
                if (response.success) {
                    _ingredients.value = response.data
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
```

---

## 🔑 核心API使用示例

### 1. 食材识别

```bash
POST http://localhost:8080/api/ingredients/recognize
Headers:
  user-id: 1
  Content-Type: application/json
  
Body:
{
  "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "西红柿",
      "category": "蔬菜类",
      "freshness": "FRESH",
      "expiryDate": "2024-03-27",
      "storageMethod": "REFRIGERATE",
      "storageAdvice": "建议冷藏保存，3-5天内食用"
    }
  ],
  "message": "识别成功，已添加1个食材"
}
```

### 2. 查询过期提醒

```bash
GET http://localhost:8080/api/ingredients/alerts
Headers:
  user-id: 1

Response:
{
  "success": true,
  "data": [
    {
      "ingredient": {...},
      "priority": "HIGH",
      "message": "生菜明天到期，建议今日食用",
      "quickSolution": "方案1: 凉拌生菜(5分钟)  方案2: 生菜蛋汤(10分钟)"
    }
  ]
}
```

### 3. AI生成食谱

```bash
POST http://localhost:8080/api/ai/generate-recipe
Headers:
  user-id: 1
  Content-Type: application/json
  
Body:
{
  "availableIngredients": ["西红柿", "鸡蛋"],
  "cuisineType": "家常菜",
  "cookingTime": 20,
  "nutritionGoals": ["减脂"]
}

Response:
{
  "success": true,
  "data": {
    "title": "西红柿炒鸡蛋",
    "ingredients": [
      {"name": "西红柿", "quantity": 200, "unit": "g"},
      {"name": "鸡蛋", "quantity": 2, "unit": "个"}
    ],
    "steps": [
      {"step": 1, "content": "打散鸡蛋", "duration": 60},
      {"step": 2, "content": "热油炒鸡蛋", "duration": 90}
    ],
    "nutrition": {
      "calories": "280kcal",
      "protein": "15g"
    }
  }
}
```

---

## 🎯 核心功能实现要点

### 1. 食材过期提醒算法

```kotlin
// 在IngredientService中实现
fun getPriority(): ExpiryPriority {
    val remaining = getRemainingDays() ?: return ExpiryPriority.LOW
    return when {
        remaining <= 2 && freshness == Freshness.WILTING -> ExpiryPriority.HIGH
        remaining in 1..2 -> ExpiryPriority.HIGH
        remaining in 3..4 -> ExpiryPriority.MEDIUM
        remaining in 5..7 -> ExpiryPriority.LOW
        else -> ExpiryPriority.LOW
    }
}
```

### 2. AI Prompt工程技巧

**食材识别Prompt:**
```
你是专业的食材识别专家。请识别图片中的食材，并返回JSON格式。
要求：
1. 准确识别食材名称
2. 判断新鲜度(FRESH/WILTING/SPOILING)
3. 预估重量
4. 推荐保存方式
5. 计算保质天数

返回纯JSON，无其他文字。
```

**食谱生成Prompt:**
```
你是资深营养师+星级厨师。根据用户需求定制食谱。
要求：
1. 食材用量精确到克
2. 步骤包含时间(秒)
3. 调味比例准确
4. 提供营养配比

考虑用户设备: {燃气灶/电磁炉}
```

### 3. 推荐算法逻辑

```kotlin
// 推荐权重计算
fun calculateRecommendationScore(recipe: Recipe, user: User): Double {
    var score = 0.0
    
    // 1. 临期食材优先(权重40%)
    val expiringCount = countExpiringIngredients(recipe, user)
    score += expiringCount * 0.4
    
    // 2. 用户偏好匹配(权重30%)
    if (recipe.cuisine in user.preferences) score += 0.3
    
    // 3. 历史行为(权重20%)
    score += getUserAffinityScore(recipe, user) * 0.2
    
    // 4. 热度(权重10%)
    score += (recipe.favoriteCount / 1000.0) * 0.1
    
    return score
}
```

---

## 🐛 常见问题

### Q1: AI识别失败
**A:** 检查：
1. 通义千问API Key是否正确
2. 图片Base64格式是否正确
3. 网络连接是否正常
4. 免费额度是否用完

### Q2: 数据库连接失败
**A:** 检查：
1. MySQL是否启动
2. 用户名密码是否正确
3. 数据库recipe_ai是否创建
4. 防火墙是否开放3306端口

### Q3: 跨域问题
**A:** 在后端添加CORS配置：
```kotlin
@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("*")
    }
}
```

---

## 📈 性能优化建议

1. **Redis缓存推荐结果**
   - 缓存时长: 1小时
   - 缓存Key: `recommend:user:{userId}`

2. **AI调用限流**
   - 使用令牌桶算法
   - 每用户每分钟最多10次

3. **数据库索引优化**
   - 已在init.sql中添加关键索引
   - 定期分析慢查询

4. **图片压缩**
   - 上传前压缩至500KB以内
   - 使用WebP格式

---

## 🔄 后续扩展方向

### 短期(1-2周)
- [ ] 用户JWT认证
- [ ] 语音烹饪指导
- [ ] 社区互动(评论/收藏)

### 中期(1个月)
- [ ] 智能推荐算法优化
- [ ] 营养分析图表
- [ ] 食材采购电商对接

### 长期(3个月+)
- [ ] AR烹饪指导
- [ ] 社交分享功能
- [ ] 多人家庭协作

---

## 📞 技术支持

- 通义千问文档: https://help.aliyun.com/zh/dashscope/
- Spring Boot文档: https://spring.io/projects/spring-boot
- Jetpack Compose: https://developer.android.com/jetpack/compose

---

## 📄 License

MIT License - 自由使用，保留署名