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