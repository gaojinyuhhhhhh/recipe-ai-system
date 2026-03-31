# API文档 - AI智能食谱管理系统

## 基础信息
- Base URL: `http://localhost:8080/api`
- 认证方式: Header中携带 `user-id` (实际项目应使用JWT Token)
- Content-Type: `application/json`

---

## 1. 用户管理模块

### 1.1 用户注册
```
POST /users/register
Content-Type: application/json

Request Body:
{
  "username": "testuser",
  "password": "password123",
  "phone": "13800138000"
}

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "testuser",
    "phone": "13800138000"
  },
  "message": "注册成功"
}
```

### 1.2 用户登录
```
POST /users/login

Request:
{
  "username": "testuser",
  "password": "password123"
}

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "testuser",
    "token": "mock_token_1"
  },
  "message": "登录成功"
}
```

### 1.3 获取用户信息
```
GET /users/me
Headers:
  user-id: 1
```

### 1.4 更新用户信息
```
PUT /users/me
Headers:
  user-id: 1

Body:
{
  "nickname": "美食家",
  "familySize": 3,
  "cookingFrequency": 5
}
```

### 1.5 设置用户偏好
```
PUT /users/preferences
Headers:
  user-id: 1

Body:
{
  "preferences": "{\"cuisines\":[\"川菜\",\"粤菜\"],\"tastes\":[\"微辣\"],\"diet\":\"减脂\"}"
}
```

---

## 2. 食材管理模块

### 2.1 AI拍照识别食材
```
POST /ingredients/recognize
Headers:
  user-id: 1

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
      "shelfLife": 7,
      "storageMethod": "REFRIGERATE",
      "storageAdvice": "建议冷藏保存，3-5天内食用"
    }
  ],
  "message": "识别成功，已添加1个食材"
}
```

### 2.2 手动添加食材
```
POST /ingredients
Headers:
  user-id: 1

Body:
{
  "name": "鸡蛋",
  "category": "肉蛋类",
  "quantity": 12,
  "unit": "个",
  "purchaseDate": "2024-03-20",
  "storageMethod": "REFRIGERATE"
}
```

### 2.3 批量快速录入
```
POST /ingredients/batch
Headers:
  user-id: 1

Body:
{
  "ingredients": "西红柿, 鸡蛋, 葱, 姜, 蒜"
}
```

### 2.4 查询所有食材
```
GET /ingredients
Headers:
  user-id: 1
```

### 2.5 查询临期食材
```
GET /ingredients/expiring
Headers:
  user-id: 1
```

### 2.6 获取过期提醒
```
GET /ingredients/alerts
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

### 2.7 更新食材
```
PUT /ingredients/{id}
Headers:
  user-id: 1

Body:
{
  "name": "西红柿",
  "quantity": 500,
  "unit": "g"
}
```

### 2.8 删除食材
```
DELETE /ingredients/{id}
Headers:
  user-id: 1
```

### 2.9 标记为已消耗
```
PUT /ingredients/{id}/consume
Headers:
  user-id: 1
```

---

## 3. 采购管理模块

### 3.1 手动添加采购项
```
POST /shopping
Headers:
  user-id: 1

Body:
{
  "name": "牛肉",
  "category": "肉蛋类",
  "quantity": 500,
  "unit": "g"
}
```

### 3.2 从食谱导入食材
```
POST /shopping/import-recipe/{recipeId}
Headers:
  user-id: 1
```

### 3.3 批量合并多个食谱
```
POST /shopping/merge-recipes
Headers:
  user-id: 1

Body:
{
  "recipeIds": [1, 2, 3]
}

Response:
{
  "success": true,
  "data": {
    "items": [...],
    "groupedByCategory": {
      "蔬菜类": [...],
      "肉蛋类": [...]
    },
    "recommendedOrder": [...]
  }
}
```

### 3.4 查询采购清单
```
GET /shopping?completed=false
Headers:
  user-id: 1
```

### 3.5 批量勾选完成
```
PUT /shopping/complete
Headers:
  user-id: 1

Body:
{
  "itemIds": [1, 2, 3]
}
```

### 3.6 批量同步到食材库
```
POST /shopping/sync-to-ingredients
Headers:
  user-id: 1

Body:
{
  "itemIds": [1, 2, 3]
}
```

---

## 4. 食谱社区模块

### 4.1 创建食谱
```
POST /recipes
Headers:
  user-id: 1

Body:
{
  "title": "西红柿炒鸡蛋",
  "description": "经典家常菜",
  "ingredients": "[{\"name\":\"西红柿\",\"quantity\":200,\"unit\":\"g\"},{\"name\":\"鸡蛋\",\"quantity\":2,\"unit\":\"个\"}]",
  "steps": "[{\"step\":1,\"content\":\"打散鸡蛋\",\"duration\":60}]",
  "cookingTime": 15,
  "difficulty": "EASY",
  "cuisine": "家常菜",
  "tags": "[\"快手\",\"下饭\"]"
}

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "title": "西红柿炒鸡蛋",
    "aiRating": "A",
    "aiSuggestion": "建议增加生姜去腥\n调整火候控制"
  },
  "message": "创建成功，AI评级: A级良好"
}
```

### 4.2 AI优化食谱
```
POST /recipes/{id}/optimize
Headers:
  user-id: 1

Response:
{
  "success": true,
  "data": {
    "id": 2,
    "title": "西红柿炒鸡蛋 (AI优化版)",
    "isAiOptimized": true,
    "originalRecipeId": 1
  },
  "message": "已生成AI优化版食谱"
}
```

### 4.3 查询食谱详情
```
GET /recipes/{id}
Headers:
  user-id: 1

Response:
{
  "success": true,
  "data": {
    "recipe": {...},
    "canMake": true,
    "missingIngredients": [],
    "isFavorited": false
  }
}
```

### 4.4 搜索食谱
```
GET /recipes/search?keyword=鸡蛋&cuisine=家常菜&difficulty=EASY&tags=快手,减脂
```

### 4.5 热门食谱
```
GET /recipes/hot?limit=10
```

### 4.6 查询我的食谱
```
GET /recipes/my
Headers:
  user-id: 1
```

### 4.7 查询收藏的食谱
```
GET /recipes/favorites
Headers:
  user-id: 1
```

### 4.8 收藏/取消收藏
```
POST /recipes/{id}/favorite
Headers:
  user-id: 1

Response:
{
  "success": true,
  "data": true,
  "message": "已收藏"
}
```

### 4.9 添加评论
```
POST /recipes/{id}/comments
Headers:
  user-id: 1

Body:
{
  "content": "很好吃！",
  "rating": 5
}
```

---

## 5. AI功能模块

### 5.1 AI定制食谱
```
POST /ai/generate-recipe
Headers:
  user-id: 1

Body:
{
  "availableIngredients": ["西红柿", "鸡蛋"],
  "cuisineType": "家常菜",
  "taste": "微辣",
  "cookingTime": 20,
  "difficulty": "EASY",
  "dietaryRestrictions": ["不吃海鲜"],
  "nutritionGoals": ["减脂", "低糖"],
  "cookingEquipment": "燃气灶",
  "servings": 2
}

Response:
{
  "success": true,
  "data": {
    "title": "西红柿炒鸡蛋",
    "description": "经典快手菜",
    "ingredients": [...],
    "steps": [...],
    "nutrition": {
      "calories": "280kcal",
      "protein": "15g",
      "carbs": "12g",
      "fat": "18g"
    }
  },
  "message": "AI食谱生成成功"
}
```

### 5.2 AI快速食谱
```
POST /ai/quick-recipe
Headers:
  user-id: 1

Body:
{
  "ingredients": ["西红柿", "鸡蛋", "葱"],
  "timeLimit": 15
}
```

---

## 6. 智能推荐模块

### 6.1 获取个性化推荐
```
GET /recommend?limit=20
Headers:
  user-id: 1

Response:
{
  "success": true,
  "data": {
    "all": [...],  // 综合推荐
    "expiringIngredients": [...],  // 临期食材快用
    "personalized": [...],  // 我的口味专属
    "hot": [...],  // 热门食谱
    "scores": {
      "1": 85.5,
      "2": 78.3
    }
  }
}
```

### 6.2 智能组合临期食材
```
GET /recommend/smart-combine
Headers:
  user-id: 1

Response:
{
  "success": true,
  "data": [
    {
      "recipe": {...},
      "matchedIngredients": [...],
      "matchCount": 3
    }
  ]
}
```

---

## 7. 系统接口

### 7.1 健康检查
```
GET /health

Response:
{
  "status": "UP",
  "service": "recipe-ai-backend",
  "version": "1.0.0",
  "timestamp": "2024-03-20T10:30:00"
}
```

### 7.2 系统信息
```
GET /info

Response:
{
  "name": "AI智能食谱管理系统",
  "description": "基于通义千问AI的智能食谱管理平台",
  "version": "1.0.0",
  "features": [
    "AI食材识别",
    "智能过期提醒",
    "AI食谱生成",
    "智能推荐",
    "食谱社区"
  ]
}
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 测试建议

1. 先测试健康检查: `GET /health`
2. 注册用户: `POST /users/register`
3. 登录获取token: `POST /users/login`
4. 添加食材: `POST /ingredients`
5. AI生成食谱: `POST /ai/generate-recipe`
6. 获取推荐: `GET /recommend`

---

## Postman Collection

可以将以上API导入Postman进行测试，详见项目中的 `postman_collection.json` 文件(需要单独生成)。
