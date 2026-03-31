class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    
    NavHost(
        navController = navController,
        startDestination = "ingredient_list"
    ) {
        composable("ingredient_list") {
            IngredientListScreen()
        }
        composable("camera") {
            CameraScreen(
                onImageCaptured = { base64 ->
                    // 调用识别API
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
