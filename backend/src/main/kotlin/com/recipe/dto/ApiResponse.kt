package com.recipe.dto

/**
 * 通用API响应封装类
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T> success(data: T, message: String = "操作成功"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = message
            )
        }

        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = "操作成功"
            )
        }

        fun <T> error(message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                message = message
            )
        }
    }
}
