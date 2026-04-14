package com.recipe.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recipe.data.local.TokenManager
import com.recipe.ui.components.SecondaryButton
import com.recipe.ui.components.SettingsListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onNavigateToMyRecipes: () -> Unit = {},
    onNavigateToMyFavorites: () -> Unit = {},
    onNavigateToMyComments: () -> Unit = {},
    onNavigateToPreferences: () -> Unit = {},
    onNavigateToAiProfile: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "我的",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 用户信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 用户信息
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = TokenManager.getNickname() ?: "用户",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "@${TokenManager.getUsername() ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 功能菜单
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsListItem(
                    icon = Icons.Default.MenuBook,
                    title = "我的食谱",
                    subtitle = "管理本地和收藏的食谱",
                    onClick = onNavigateToMyRecipes
                )
                
                Divider(modifier = Modifier.padding(horizontal = 20.dp))
                
                SettingsListItem(
                    icon = Icons.Default.Favorite,
                    title = "我的收藏",
                    subtitle = "查看收藏的社区食谱",
                    onClick = onNavigateToMyFavorites
                )
                
                Divider(modifier = Modifier.padding(horizontal = 20.dp))
                
                SettingsListItem(
                    icon = Icons.Default.Comment,
                    title = "我的评论",
                    subtitle = "查看发表的评论",
                    onClick = onNavigateToMyComments
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI 功能区域
            Text(
                text = "AI 功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsListItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI烹饪画像",
                    subtitle = "基于偏好生成个性化推荐",
                    onClick = onNavigateToAiProfile
                )
                
                Divider(modifier = Modifier.padding(horizontal = 20.dp))
                
                SettingsListItem(
                    icon = Icons.Default.Settings,
                    title = "偏好设置",
                    subtitle = "调整口味和饮食偏好",
                    onClick = onNavigateToPreferences
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 退出登录按钮
            SecondaryButton(
                text = "退出登录",
                onClick = onLogout,
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
