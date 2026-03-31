#!/bin/bash

# AI智能食谱管理系统 - API测试脚本
# 使用方法: chmod +x test_api.sh && ./test_api.sh

BASE_URL="http://localhost:8080/api"
USER_ID=1

echo "=========================================="
echo "  AI智能食谱管理系统 - API测试"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "\n${YELLOW}测试: $description${NC}"
    echo "URL: $method $BASE_URL$endpoint"
    
    if [ -n "$data" ]; then
        response=$(curl -s -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "user-id: $USER_ID" \
            -d "$data")
    else
        response=$(curl -s -X $method "$BASE_URL$endpoint" \
            -H "user-id: $USER_ID")
    fi
    
    # 检查是否成功
    if echo "$response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ 成功${NC}"
    else
        echo -e "${RED}✗ 失败${NC}"
    fi
    
    # 打印响应(格式化)
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
}

# 1. 健康检查
echo -e "\n${YELLOW}========== 1. 系统检查 ==========${NC}"
test_api "GET" "/health" "" "健康检查"
test_api "GET" "/info" "" "系统信息"

# 2. 用户管理
echo -e "\n${YELLOW}========== 2. 用户管理 ==========${NC}"
test_api "POST" "/users/register" '{
  "username": "testuser",
  "password": "password123",
  "phone": "13800138000"
}' "用户注册"

test_api "POST" "/users/login" '{
  "username": "testuser",
  "password": "password123"
}' "用户登录"

test_api "GET" "/users/me" "" "获取用户信息"

test_api "PUT" "/users/preferences" '{
  "preferences": "{\"cuisines\":[\"川菜\",\"粤菜\"],\"tastes\":[\"微辣\"],\"diet\":\"减脂\"}"
}' "设置用户偏好"

# 3. 食材管理
echo -e "\n${YELLOW}========== 3. 食材管理 ==========${NC}"
test_api "POST" "/ingredients" '{
  "name": "西红柿",
  "category": "蔬菜类",
  "quantity": 500,
  "unit": "g",
  "purchaseDate": "2024-03-20",
  "storageMethod": "REFRIGERATE"
}' "手动添加食材"

test_api "POST" "/ingredients/batch" '{
  "ingredients": "鸡蛋, 葱, 姜, 蒜"
}' "批量添加食材"

test_api "GET" "/ingredients" "" "查询所有食材"

test_api "GET" "/ingredients/expiring" "" "查询临期食材"

test_api "GET" "/ingredients/alerts" "" "获取过期提醒"

# 4. 采购管理
echo -e "\n${YELLOW}========== 4. 采购管理 ==========${NC}"
test_api "POST" "/shopping" '{
  "name": "牛肉",
  "category": "肉蛋类",
  "quantity": 500,
  "unit": "g"
}' "添加采购项"

test_api "GET" "/shopping" "" "查询采购清单"

# 5. AI功能
echo -e "\n${YELLOW}========== 5. AI功能 ==========${NC}"
test_api "POST" "/ai/generate-recipe" '{
  "availableIngredients": ["西红柿", "鸡蛋"],
  "cuisineType": "家常菜",
  "cookingTime": 20,
  "difficulty": "EASY",
  "nutritionGoals": ["减脂"],
  "servings": 2
}' "AI生成食谱"

test_api "POST" "/ai/quick-recipe" '{
  "ingredients": ["西红柿", "鸡蛋", "葱"],
  "timeLimit": 15
}' "AI快速食谱"

# 6. 食谱社区
echo -e "\n${YELLOW}========== 6. 食谱社区 ==========${NC}"
test_api "POST" "/recipes" '{
  "title": "西红柿炒鸡蛋",
  "description": "经典家常菜",
  "ingredients": "[{\"name\":\"西红柿\",\"quantity\":200,\"unit\":\"g\"},{\"name\":\"鸡蛋\",\"quantity\":2,\"unit\":\"个\"}]",
  "steps": "[{\"step\":1,\"content\":\"打散鸡蛋\",\"duration\":60},{\"step\":2,\"content\":\"热油炒鸡蛋\",\"duration\":90}]",
  "cookingTime": 15,
  "difficulty": "EASY",
  "cuisine": "家常菜",
  "tags": "[\"快手\",\"下饭\"]"
}' "创建食谱"

test_api "GET" "/recipes/search?keyword=鸡蛋" "" "搜索食谱"

test_api "GET" "/recipes/hot?limit=5" "" "热门食谱"

# 7. 智能推荐
echo -e "\n${YELLOW}========== 7. 智能推荐 ==========${NC}"
test_api "GET" "/recommend?limit=10" "" "获取推荐"

test_api "GET" "/recommend/smart-combine" "" "智能组合临期食材"

echo -e "\n${GREEN}=========================================="
echo "  测试完成!"
echo -e "==========================================${NC}"
