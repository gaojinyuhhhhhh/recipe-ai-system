package com.recipe.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recipe.ui.components.PrimaryButton
import com.recipe.ui.components.RecipeTextField
import com.recipe.ui.components.RecipePasswordField
import com.recipe.ui.components.TextActionButton
import com.recipe.viewmodel.AuthState
import com.recipe.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val loginState by authViewModel.loginState.collectAsState()
    val isLoading = loginState is AuthState.Loading
    val isError = loginState is AuthState.Error
    val errorMessage = if (loginState is AuthState.Error) {
        (loginState as AuthState.Error).message
    } else null

    LaunchedEffect(loginState) {
        if (loginState is AuthState.Success) {
            onRegisterSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "注册账号",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        authViewModel.resetState()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
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
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 标题
            Text(
                text = "创建你的账号",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "开启智能烹饪之旅",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 用户名
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

            // 手机号
            RecipeTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "手机号（可选）",
                placeholder = "请输入手机号",
                leadingIcon = Icons.Default.Phone,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 密码
            RecipePasswordField(
                value = password,
                onValueChange = { password = it },
                label = "密码",
                placeholder = "请输入密码",
                enabled = !isLoading,
                isError = isError && errorMessage?.contains("密码") == true,
                errorMessage = if (errorMessage?.contains("密码") == true) errorMessage else null
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 确认密码
            RecipePasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "确认密码",
                placeholder = "请再次输入密码",
                enabled = !isLoading,
                isError = isError && errorMessage?.contains("密码") == true,
                errorMessage = if (errorMessage?.contains("密码") == true) errorMessage else null
            )

            if (isError && errorMessage != null && !errorMessage.contains("密码")) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 注册按钮
            PrimaryButton(
                text = if (isLoading) "注册中..." else "注册",
                onClick = {
                    authViewModel.register(username, password, confirmPassword, phone)
                },
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
            )

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已有账号？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextActionButton(
                    text = "返回登录",
                    onClick = {
                        authViewModel.resetState()
                        onNavigateBack()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
