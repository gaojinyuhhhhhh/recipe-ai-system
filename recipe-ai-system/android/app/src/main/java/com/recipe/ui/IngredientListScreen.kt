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