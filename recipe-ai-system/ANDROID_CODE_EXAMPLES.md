# Android端核心代码示例

本文件包含Android端的关键实现代码，帮助你快速开发前端应用。

---

## 1. Gradle配置 (build.gradle.kts)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.recipe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.recipe"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

---

## 2. 数据模型 (data/model/)

```kotlin
// Ingredient.kt
data class Ingredient(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val category: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val freshness: String = "FRESH",
    val purchaseDate: String,
    val expiryDate: String? = null,
    val shelfLife: Int? = null,
    val storageMethod: String? = null,
    val storageAdvice: String? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val isConsumed: Boolean = false
) {
    fun getRemainingDays(): Int? {
        if (expiryDate == null) return null
        val expiry = LocalDate.parse(expiryDate)
        return ChronoUnit.DAYS.between(LocalDate.now(), expiry).toInt()
    }
    
    fun getPriorityColor(): Color {
        val remaining = getRemainingDays() ?: return Color.Gray
        return when {
            remaining <= 2 -> Color.Red
            remaining in 3..4 -> Color(0xFFFF9800)
            else -> Color.Green
        }
    }
}

// ApiResponse.kt
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: Long
)
```

---

## 3. Retrofit API接口 (data/remote/)

```kotlin
// ApiService.kt
interface ApiService {
    companion object {
        const val BASE_URL = "http://10.0.2.2:8080/api/"  // 模拟器使用
        // const val BASE_URL = "http://192.168.1.100:8080/api/"  // 真机使用
    }
    
    // 食材相关
    @POST("ingredients/recognize")
    suspend fun recognizeIngredient(
        @Header("user-id") userId: Long,
        @Body request: RecognizeRequest
    ): ApiResponse<List<Ingredient>>
    
    @GET("ingredients")
    suspend fun getIngredients(
        @Header("user-id") userId: Long
    ): ApiResponse<List<Ingredient>>
    
    @GET("ingredients/alerts")
    suspend fun getExpiryAlerts(
        @Header("user-id") userId: Long
    ): ApiResponse<List<ExpiryAlert>>
    
    @POST("ingredients")
    suspend fun addIngredient(
        @Header("user-id") userId: Long,
        @Body ingredient: Ingredient
    ): ApiResponse<Ingredient>
    
    @DELETE("ingredients/{id}")
    suspend fun deleteIngredient(
        @Header("user-id") userId: Long,
        @Path("id") id: Long
    ): ApiResponse<Unit>
}

// RecognizeRequest.kt
data class RecognizeRequest(val imageBase64: String)

// RetrofitClient.kt
object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

---

## 4. ViewModel (viewmodel/)

```kotlin
// IngredientViewModel.kt
class IngredientViewModel : ViewModel() {
    private val api = RetrofitClient.api
    private val userId = 1L  // 实际项目从登录状态获取
    
    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients
    
    private val _alerts = MutableStateFlow<List<ExpiryAlert>>(emptyList())
    val alerts: StateFlow<List<ExpiryAlert>> = _alerts
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // 加载食材列表
    fun loadIngredients() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val response = api.getIngredients(userId)
                if (response.success) {
                    _ingredients.value = response.data ?: emptyList()
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    // 加载过期提醒
    fun loadAlerts() {
        viewModelScope.launch {
            try {
                val response = api.getExpiryAlerts(userId)
                if (response.success) {
                    _alerts.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("IngredientVM", "加载提醒失败", e)
            }
        }
    }
    
    // AI识别食材
    fun recognizeImage(imageBase64: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val request = RecognizeRequest(imageBase64)
                val response = api.recognizeIngredient(userId, request)
                if (response.success) {
                    loadIngredients()  // 重新加载列表
                    _error.value = response.message
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                _error.value = "识别失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    // 删除食材
    fun deleteIngredient(id: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteIngredient(userId, id)
                if (response.success) {
                    loadIngredients()
                }
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }
}
```

---

## 5. UI界面 (ui/)

```kotlin
// IngredientListScreen.kt
@Composable
fun IngredientListScreen(
    viewModel: IngredientViewModel = viewModel()
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadIngredients()
        viewModel.loadAlerts()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的食材") },
                actions = {
                    IconButton(onClick = { /* 拍照识别 */ }) {
                        Icon(Icons.Default.Camera, "拍照识别")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* 手动添加 */ }) {
                Icon(Icons.Default.Add, "添加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 过期提醒区域
            if (alerts.isNotEmpty()) {
                AlertSection(alerts = alerts)
            }
            
            // 食材列表
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(ingredients) { ingredient ->
                        IngredientItem(
                            ingredient = ingredient,
                            onDelete = { viewModel.deleteIngredient(it.id!!) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    onDelete: (Ingredient) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 食材图片
            AsyncImage(
                model = ingredient.imageUrl ?: R.drawable.ic_ingredient_placeholder,
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 食材信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${ingredient.category} · ${ingredient.freshness}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                ingredient.getRemainingDays()?.let { days ->
                    Text(
                        text = when {
                            days < 0 -> "已过期"
                            days == 0 -> "今天到期"
                            days == 1 -> "明天到期"
                            else -> "还剩${days}天"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ingredient.getPriorityColor()
                    )
                }
            }
            
            // 删除按钮
            IconButton(onClick = { onDelete(ingredient) }) {
                Icon(Icons.Default.Delete, "删除")
            }
        }
    }
}

@Composable
fun AlertSection(alerts: List<ExpiryAlert>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚠️ 过期提醒",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(8.dp))
            alerts.forEach { alert ->
                Text(
                    text = "• ${alert.message}",
                    style = MaterialTheme.typography.bodySmall
                )
                alert.quickSolution?.let {
                    Text(
                        text = "  $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
```

---

## 6. 相机拍照功能 (ui/camera/)

```kotlin
// CameraScreen.kt
@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val preview = remember { Preview.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    
    val previewView = remember {
        PreviewView(context).apply {
            preview.setSurfaceProvider(surfaceProvider)
        }
    }
    
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // 拍照按钮
        Button(
            onClick = {
                captureImage(context, imageCapture, onImageCaptured)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Text("拍照识别")
        }
        
        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "关闭", tint = Color.White)
        }
    }
}

fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (String) -> Unit
) {
    val file = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // 转换为Base64
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val base64 = bitmapToBase64(bitmap)
                onImageCaptured(base64)
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "拍照失败", exception)
            }
        }
    )
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
```

---

## 7. MainActivity.kt

```kotlin
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
```

---

## 使用说明

1. **复制以上代码到对应目录**
2. **修改BASE_URL为你的后端地址**
3. **在AndroidManifest.xml中添加权限**:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

4. **运行项目**: 
```bash
./gradlew assembleDebug
```

5. **测试流程**:
   - 启动应用
   - 点击相机图标
   - 拍照食材
   - 等待AI识别
   - 查看识别结果

---

完整项目示例请参考 `android/` 目录。