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