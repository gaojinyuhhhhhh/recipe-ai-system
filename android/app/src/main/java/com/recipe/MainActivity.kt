package com.recipe

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipe.data.local.AppDatabase
import com.recipe.data.local.TokenManager
import com.recipe.ui.IngredientListScreen
import com.recipe.ui.ai.AiChatScreen
import com.recipe.ui.ai.RecommendScreen
import com.recipe.ui.ai.RecipeDetailFromRecommendScreen
import com.recipe.ui.camera.CameraScreen
import com.recipe.ui.camera.RecognitionResultScreen
import com.recipe.ui.recipe.CookingModeScreen
import com.recipe.ui.recipe.CreateRecipeScreen
import com.recipe.ui.recipe.EditRecipeScreen
import com.recipe.ui.recipe.EditLocalRecipeScreen
import com.recipe.ui.recipe.LocalRecipeDetailScreen
import com.recipe.ui.recipe.RecipeDetailScreen
import com.recipe.ui.recipe.RecipeScreen
import com.recipe.ui.shopping.ShoppingScreen
import com.recipe.ui.theme.RecipeAITheme
import com.recipe.ui.user.*
import com.recipe.viewmodel.AuthViewModel
import com.recipe.viewmodel.IngredientViewModel
import com.recipe.util.CookingNotificationManager
import com.recipe.viewmodel.CookingSessionHolder

/**
 * 底部导航项定义
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Ingredients : BottomNavItem("ingredients", "食材", Icons.Filled.Kitchen, Icons.Outlined.Kitchen)
    data object Recipes : BottomNavItem("recipes", "食谱", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    data object Shopping : BottomNavItem("shopping", "购物", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
    data object Profile : BottomNavItem("profile", "我的", Icons.Filled.Person, Icons.Outlined.Person)
}

/**
 * 应用主入口
 * 初始化 TokenManager 并设置 Compose UI
 */
class MainActivity : ComponentActivity() {

    // 用于通知点击时传递导航目标给 Compose
    private var _navigateTo = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化TokenManager
        TokenManager.init(applicationContext)
        // 创建通知渠道（必须在发送通知前调用）
        CookingNotificationManager.createChannel(applicationContext)

        // 检查是否从通知点击进入，获取目标路由
        _navigateTo.value = intent?.getStringExtra(CookingNotificationManager.EXTRA_NAVIGATE_TO)

        setContent {
            RecipeAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecipeApp(navigateToState = _navigateTo)
                }
            }
        }
    }

    /**
     * Activity已在前台时，通知点击会触发 onNewIntent 而非 onCreate
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val target = intent.getStringExtra(CookingNotificationManager.EXTRA_NAVIGATE_TO)
        if (target != null) {
            _navigateTo.value = target
        }
    }
}

/**
 * 应用核心组合函数 — 统一管理导航、底部Tab、页面路由
 *
 * 导航架构：
 * - 4个主 Tab：食材库 / 食谱 / 购物 / 我的
 * - 子页面通过 NavHost 路由跳转，底部导航栏仅在主 Tab 页面显示
 * - 底部导航使用 popUpTo(startDestination) + saveState/restoreState 模式
 *   避免重复创建页面并保留各Tab状态
 *
 * 路由参数编码：
 * - AI食谱详情页使用 Base64 编码中文参数，避免导航参数中包含特殊字符导致闪退
 */
@Composable
fun RecipeApp(navigateToState: MutableState<String?> = mutableStateOf(null)) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authViewModel: AuthViewModel = viewModel()
    val ingredientViewModel: IngredientViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val ingredientsList by ingredientViewModel.ingredients.collectAsState()
    val navigateTo by navigateToState

    val bottomNavItems = listOf(
        BottomNavItem.Ingredients,
        BottomNavItem.Recipes,
        BottomNavItem.Shopping,
        BottomNavItem.Profile
    )

    // 只在主Tab页面显示底部导航
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // 根据登录状态决定起始页面：已登录→食材库，未登录→登录页
    val startDestination = if (isLoggedIn) BottomNavItem.Ingredients.route else "login"

    // 处理通知点击跳转：登录后自动导航到烹饪界面
    LaunchedEffect(isLoggedIn, navigateTo) {
        if (isLoggedIn && navigateTo == "cooking_mode") {
            // 检查CookingSessionHolder是否有数据（防止空数据导航）
            if (CookingSessionHolder.steps.isNotEmpty()) {
                navController.navigate("cooking_mode") {
                    launchSingleTop = true
                }
            }
            // 消费一次性导航事件
            navigateToState.value = null
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { 
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 登录页面（登录成功后清除登录页回退栈，防止返回键回到登录页）
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        navController.navigate(BottomNavItem.Ingredients.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // 注册页面
            composable("register") {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(BottomNavItem.Ingredients.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // 主Tab页面
            composable(BottomNavItem.Ingredients.route) {
                IngredientListScreen(
                    viewModel = ingredientViewModel,
                    onNavigateToCamera = { navController.navigate("camera") },
                    onNavigateToRecommend = { navController.navigate("ai_recommend") },
                    onNavigateToChat = { navController.navigate("ai_chat") }
                )
            }
            composable(BottomNavItem.Recipes.route) {
                RecipeScreen(
                    onNavigateToDetail = { recipeId ->
                        navController.navigate("recipe_detail/$recipeId")
                    },
                    onNavigateToCreate = {
                        navController.navigate("create_recipe")
                    },
                    onNavigateToLocalDetail = { localId ->
                        navController.navigate("local_recipe_detail/$localId")
                    }
                )
            }
            composable(BottomNavItem.Shopping.route) {
                ShoppingScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToMyRecipes = { navController.navigate("my_recipes") },
                    onNavigateToMyFavorites = { navController.navigate("my_favorites") },
                    onNavigateToMyComments = { navController.navigate("my_comments") },
                    onNavigateToPreferences = { navController.navigate("preferences") },
                    onNavigateToAiProfile = { navController.navigate("ai_profile") }
                )
            }

            // 个人中心子页面
            composable("my_recipes") {
                MyRecipesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLocalDetail = { localId ->
                        navController.navigate("local_recipe_detail/$localId")
                    }
                )
            }
            composable("my_favorites") {
                MyFavoritesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { recipeId ->
                        navController.navigate("recipe_detail/$recipeId")
                    }
                )
            }
            composable("my_comments") {
                MyCommentsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { recipeId ->
                        navController.navigate("recipe_detail/$recipeId")
                    }
                )
            }
            composable("preferences") {
                PreferencesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("ai_profile") {
                AiProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onApplyPreferences = {
                        navController.navigate("preferences") {
                            popUpTo("ai_profile") { inclusive = true }
                        }
                    }
                )
            }

            // 食谱详情页
            composable("recipe_detail/{recipeId}") { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId")?.toLongOrNull() ?: 0L
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate("edit_recipe/$id")
                    },
                    onNavigateToShopping = {
                        // 先返回，再跳转到购物清单，避免导航栈混乱
                        navController.popBackStack()
                        navController.navigate(BottomNavItem.Shopping.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCooking = {
                        navController.navigate("cooking_mode")
                    }
                )
            }

            // 创建食谱页
            composable("create_recipe") {
                CreateRecipeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 编辑食谱页
            composable("edit_recipe/{recipeId}") { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId")?.toLongOrNull() ?: 0L
                EditRecipeScreen(
                    recipeId = recipeId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 本地食谱详情页
            composable("local_recipe_detail/{localId}") { backStackEntry ->
                val localId = backStackEntry.arguments?.getString("localId")?.toLongOrNull() ?: 0L
                LocalRecipeDetailScreen(
                    localRecipeId = localId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate("edit_local_recipe/$id")
                    },
                    onNavigateToCooking = {
                        navController.navigate("cooking_mode")
                    }
                )
            }

            // 编辑本地食谱页
            composable("edit_local_recipe/{localId}") { backStackEntry ->
                val localId = backStackEntry.arguments?.getString("localId")?.toLongOrNull() ?: 0L
                EditLocalRecipeScreen(
                    localRecipeId = localId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 其他子页面
            composable("camera") {
                val isRecognizing by ingredientViewModel.loading.collectAsState()
                val recognitionError by ingredientViewModel.error.collectAsState()
                CameraScreen(
                    onImageCaptured = { base64 ->
                        ingredientViewModel.recognizeImage(base64) { _ ->
                            navController.navigate("recognition_result")
                        }
                    },
                    onDismiss = { navController.popBackStack() },
                    isRecognizing = isRecognizing,
                    recognitionError = recognitionError,
                    onClearError = { ingredientViewModel.clearError() }
                )
            }

            // AI识别结果页
            composable("recognition_result") {
                val recognizedItems by ingredientViewModel.recognizedIngredients.collectAsState()
                RecognitionResultScreen(
                    recognizedItems = recognizedItems,
                    onNavigateBack = {
                        ingredientViewModel.clearRecognized()
                        navController.popBackStack()
                    },
                    onAddAll = { selectedItems ->
                        ingredientViewModel.addRecognizedIngredients(selectedItems)
                        navController.popBackStack()
                    },
                    viewModel = ingredientViewModel
                )
            }

            // AI智能推荐页（传入当前食材库中未过期的食材名称）
            composable("ai_recommend") {
                val ingredientNames = ingredientsList
                    .filter { ingredient ->
                        val remaining = ingredient.getRemainingDays()
                        remaining == null || remaining >= 0  // 排除已过期食材
                    }
                    .map { it.name }
                RecommendScreen(
                    ingredientNames = ingredientNames,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecipeDetail = { recipeName, mainIngredients ->
                        // 使用Base64编码中文参数，避免导航参数中包含特殊字符导致闪退
                        val encodedName = Base64.encodeToString(recipeName.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                        // 将mainIngredients列表编码为JSON字符串再Base64编码
                        val ingredientsJson = org.json.JSONArray(mainIngredients).toString()
                        val encodedIngredients = Base64.encodeToString(ingredientsJson.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                        navController.navigate("ai_recipe_detail/$encodedName/$encodedIngredients")
                    }
                )
            }

            // AI生成的完整食谱详情页（参数用Base64编码传递，进入时解码）
            composable(
                "ai_recipe_detail/{encodedRecipeName}/{encodedIngredients}",
                arguments = listOf(
                    navArgument("encodedRecipeName") { type = NavType.StringType },
                    navArgument("encodedIngredients") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedName = backStackEntry.arguments?.getString("encodedRecipeName") ?: ""
                val encodedIngredients = backStackEntry.arguments?.getString("encodedIngredients") ?: ""
                val recipeName = try {
                    String(Base64.decode(encodedName, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
                } catch (e: Exception) {
                    encodedName // 解码失败则使用原值
                }
                val mainIngredients = try {
                    val ingredientsJson = String(Base64.decode(encodedIngredients, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
                    val jsonArray = org.json.JSONArray(ingredientsJson)
                    List(jsonArray.length()) { jsonArray.getString(it) }
                } catch (e: Exception) {
                    // 解码失败则使用当前所有食材
                    ingredientsList.map { it.name }
                }
                RecipeDetailFromRecommendScreen(
                    recipeName = recipeName,
                    mainIngredients = mainIngredients,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMyRecipes = { navController.navigate("my_recipes") },
                    onNavigateToLocalRecipes = {
                        // 使用与底部导航栏相同的导航模式，避免导航状态异常
                        navController.navigate(BottomNavItem.Recipes.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCooking = {
                        navController.navigate("cooking_mode")
                    }
                )
            }

            // AI对话助手页
            composable("ai_chat") {
                val chatIngredientNames = ingredientsList
                    .filter { ingredient ->
                        val remaining = ingredient.getRemainingDays()
                        remaining == null || remaining >= 0  // 排除已过期食材
                    }
                    .map { it.name }
                AiChatScreen(
                    onNavigateBack = { navController.popBackStack() },
                    ingredientNames = chatIngredientNames
                )
            }

            // 烹饪模式页
            composable("cooking_mode") {
                CookingModeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
