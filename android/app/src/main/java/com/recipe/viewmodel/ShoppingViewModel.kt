package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.model.ShoppingItem
import com.recipe.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShoppingViewModel : ViewModel() {
    private val api = RetrofitClient.api

    // 待购买列表
    private val _pendingItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val pendingItems: StateFlow<List<ShoppingItem>> = _pendingItems

    // 已完成列表
    private val _completedItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val completedItems: StateFlow<List<ShoppingItem>> = _completedItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    // 选中的项目（用于批量操作）
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    init {
        loadPendingItems()
    }

    /**
     * 加载待购买列表
     */
    fun loadPendingItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getShoppingList(completed = false)
                if (response.success) {
                    _pendingItems.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                _toastMessage.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载已完成列表
     */
    fun loadCompletedItems() {
        viewModelScope.launch {
            try {
                val response = api.getShoppingList(completed = true)
                if (response.success) {
                    _completedItems.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                _toastMessage.value = "加载失败"
            }
        }
    }

    /**
     * 添加采购项
     */
    fun addItem(name: String, quantity: Double?, unit: String?, category: String?) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any?>(
                    "name" to name
                )
                if (quantity != null) body["quantity"] = quantity
                if (unit != null) body["unit"] = unit
                if (category != null) body["category"] = category

                val response = api.addShoppingItem(body)
                if (response.success) {
                    _toastMessage.value = "添加成功"
                    loadPendingItems()
                } else {
                    _toastMessage.value = response.message ?: "添加失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "添加失败: ${e.message}"
            }
        }
    }

    /**
     * 切换选中状态
     */
    fun toggleSelection(itemId: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _selectedIds.value = current
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        val pending = _pendingItems.value
        if (_selectedIds.value.size == pending.size) {
            _selectedIds.value = emptySet()
        } else {
            _selectedIds.value = pending.mapNotNull { it.id }.toSet()
        }
    }

    /**
     * 清空选中
     */
    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    /**
     * 批量勾选完成
     */
    fun completeSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) {
            _toastMessage.value = "请先选择要完成的项目"
            return
        }
        viewModelScope.launch {
            try {
                val response = api.completeShoppingItems(mapOf("itemIds" to ids))
                if (response.success) {
                    _toastMessage.value = "已完成${response.data ?: ids.size}个采购项"
                    _selectedIds.value = emptySet()
                    loadPendingItems()
                    loadCompletedItems()
                }
            } catch (e: Exception) {
                _toastMessage.value = "操作失败"
            }
        }
    }

    /**
     * 单个勾选完成
     */
    fun completeItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val response = api.completeShoppingItems(mapOf("itemIds" to listOf(itemId)))
                if (response.success) {
                    _toastMessage.value = response.message ?: "已完成并添加到食材库"
                    loadPendingItems()
                    loadCompletedItems()
                } else {
                    _toastMessage.value = response.message ?: "操作失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "操作失败: ${e.message}"
                Log.e("ShoppingVM", "完成采购项失败", e)
            }
        }
    }

    /**
     * 批量同步到食材库
     */
    fun syncSelectedToIngredients() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) {
            _toastMessage.value = "请先选择要同步的项目"
            return
        }
        viewModelScope.launch {
            try {
                val response = api.syncToIngredients(mapOf("itemIds" to ids))
                if (response.success) {
                    _toastMessage.value = response.message ?: "已同步到食材库"
                    _selectedIds.value = emptySet()
                    loadCompletedItems()
                }
            } catch (e: Exception) {
                _toastMessage.value = "同步失败"
            }
        }
    }

    /**
     * 删除采购项
     */
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteShoppingItem(itemId)
                if (response.success) {
                    _toastMessage.value = "已删除"
                    loadPendingItems()
                    loadCompletedItems()
                }
            } catch (e: Exception) {
                _toastMessage.value = "删除失败"
            }
        }
    }

    /**
     * 从食谱导入缺少的食材
     */
    fun importFromRecipe(recipeId: Long) {
        viewModelScope.launch {
            try {
                val response = api.importFromRecipe(recipeId)
                if (response.success) {
                    _toastMessage.value = response.message ?: "导入成功"
                    loadPendingItems()
                }
            } catch (e: Exception) {
                _toastMessage.value = "导入失败"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
