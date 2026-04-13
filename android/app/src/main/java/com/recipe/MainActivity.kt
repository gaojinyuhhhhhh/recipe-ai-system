package com.recipe

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化TokenManager
        TokenManager.init(applicationContext)
        setContent {
            RecipeAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecipeApp()
                }
            }
        }
    }
}

@Composable
fun RecipeApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authViewModel: AuthViewModel = viewModel()
    val ingredientViewModel: IngredientViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val recognizedItems by ingredientViewModel.recognizedIngredients.collectAsState()
    val ingredientsList by ingredientViewModel.ingredients.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem.Ingredients,
        BottomNavItem.Recipes,
        BottomNavItem.Shopping,
        BottomNavItem.Profile
    )

    // 只在主Tab页面显示底部导航
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // 确定起始页面
    val startDestination = if (isLoggedIn) BottomNavItem.Ingredients.route else "login"

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
            // 登录页面
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
                CameraScreen(
                    onImageCaptured = { base64 ->
                        ingredientViewModel.recognizeImage(base64) { _ ->
                            navController.navigate("recognition_result")
                        }
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }

            // AI识别结果页
            composable("recognition_result") {
                RecognitionResultScreen(
                    recognizedItems = recognizedItems,
                    onNavigateBack = {
                        ingredientViewModel.clearRecognized()
                        navController.popBackStack()
                    },
                    onAddAll = {
                        ingredientViewModel.addRecognizedIngredients(recognizedItems)
                        navController.popBackStack()
                    },
                    viewModel = ingredientViewModel
                )
            }

            // AI智能推荐页
            composable("ai_recommend") {
                val ingredientNames = ingredientsList.map { it.name }
                RecommendScreen(
                    ingredientNames = ingredientNames,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecipeDetail = { recipeName, mainIngredients ->
                        // 使用Base64编码中文参数避免闪退
                        val encodedName = Base64.encodeToString(recipeName.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                        // 将mainIngredients列表编码为JSON字符串再Base64编码
                        val ingredientsJson = org.json.JSONArray(mainIngredients).toString()
                        val encodedIngredients = Base64.encodeToString(ingredientsJson.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                        navController.navigate("ai_recipe_detail/$encodedName/$encodedIngredients")
                    }
                )
            }

            // AI生成的完整食谱详情页
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
                        navController.navigate(BottomNavItem.Recipes.route) {
                            popUpTo(BottomNavItem.Recipes.route) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // AI对话助手页
            composable("ai_chat") {
                val chatIngredientNames = ingredientsList.map { it.name }
                AiChatScreen(
                    onNavigateBack = { navController.popBackStack() },
                    ingredientNames = chatIngredientNames
                )
            }
        }
    }
}
