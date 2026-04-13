package com.recipe.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.recipe.R
import com.recipe.ui.components.PrimaryButton
import com.recipe.ui.components.RecipeTextField
import com.recipe.ui.components.RecipePasswordField
import com.recipe.ui.components.TextActionButton
import com.recipe.viewmodel.AuthState
import com.recipe.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState by authViewModel.loginState.collectAsState()
    val isLoading = loginState is AuthState.Loading
    val isError = loginState is AuthState.Error
    val errorMessage = if (loginState is AuthState.Error) {
        (loginState as AuthState.Error).message
    } else null

    // 登录成功后跳转
    LaunchedEffect(loginState) {
        if (loginState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 120.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo区域 - 大标题风格
        AsyncImage(
            model = R.drawable.logo_new,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "智材识厨",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "您的智能烹饪助手",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(56.dp))

        // 用户名输入框
        RecipeTextField(
            value = username,
            onValueChange = { username = it },
            label = "用户名",
            placeholder = "请输入用户名",
            leadingIcon = Icons.Default.Person,
            enabled = !isLoading,
            isError = isError
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 密码输入框
        RecipePasswordField(
            value = password,
            onValueChange = { password = it },
            label = "密码",
            placeholder = "请输入密码",
            enabled = !isLoading,
            isError = isError,
            errorMessage = errorMessage
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 登录按钮
        PrimaryButton(
            text = if (isLoading) "登录中..." else "登录",
            onClick = { authViewModel.login(username, password) },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 注册链接
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "还没有账号？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextActionButton(
                text = "立即注册",
                onClick = {
                    authViewModel.resetState()
                    onNavigateToRegister()
                }
            )
        }
    }
}
