package com.recipe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipe.data.model.ShoppingItem
import com.recipe.data.remote.CompleteAndAddRequest
import com.recipe.data.remote.CustomIngredientInfo
import com.recipe.data.remote.RetrofitClient
import com.recipe.ui.shopping.InferredIngredientInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 购物清单ViewModel
 *
 * 职责范围：
 * 1. 采购项CRUD — 添加、删除、单个/批量勾选完成
 * 2. AI智能入库 — 采购完成后，AI推断食材保质期/存储方式，用户确认后添加到食材库
 * 3. 批量操作 — 全选/批量完成/批量同步到食材库
 * 4. 食谱导入 — 从食谱一键导入缺少的食材到采购列表
 *
 * 采购流程：
 *   添加采购项 → 勾选完成 → AI推断食材信息 → 用户确认/修改 → 添加到食材库
 */
class ShoppingViewModel : ViewModel() {
    private val api = RetrofitClient.api

    // ==================== 状态定义 ====================

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

    // 当前处理的采购项（用于AI确认对话框）
    private val _currentProcessingItem = MutableStateFlow<ShoppingItem?>(null)
    val currentProcessingItem: StateFlow<ShoppingItem?> = _currentProcessingItem

    // AI推断的食材信息
    private val _inferredInfo = MutableStateFlow<InferredIngredientInfo?>(null)
    val inferredInfo: StateFlow<InferredIngredientInfo?> = _inferredInfo

    init {
        loadPendingItems()
    }

    fun clearToast() { _toastMessage.value = null }

    /**
     * 开始处理单个采购项（AI推断→用户确认→入库）
     * 调用AI接口推断食材的保质期、存储方式等信息，
     * 失败时使用默认值填充，保证流程不中断
     */
    fun startProcessingItem(item: ShoppingItem) {
        _currentProcessingItem.value = item
        _inferredInfo.value = null

        viewModelScope.launch {
            try {
                _isLoading.value = true
                // 调用AI推断接口
                val response = api.inferIngredientInfo(mapOf("name" to item.name))
                if (response.success && response.data != null) {
                    val data = response.data
                    _inferredInfo.value = InferredIngredientInfo(
                        shelfLife = data.shelfLife,
                        storageMethod = data.storageMethod,
                        storageAdvice = data.storageAdvice,
                        freshness = data.freshness
                    )
                } else {
                    // 使用默认值
                    _inferredInfo.value = InferredIngredientInfo()
                }
            } catch (e: Exception) {
                Log.e("ShoppingVM", "AI推断失败", e)
                // 使用默认值
                _inferredInfo.value = InferredIngredientInfo()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 取消处理
     */
    fun cancelProcessing() {
        _currentProcessingItem.value = null
        _inferredInfo.value = null
    }

    /**
     * 确认并添加到食材库
     * 使用用户修改后的实际购买数量和AI推断的保质期/存储信息，
     * 调用后端接口同时完成采购项并创建食材记录
     */
    fun confirmAndAddToIngredients(info: InferredIngredientInfo) {
        val item = _currentProcessingItem.value ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 构建自定义信息，包含用户修改后的实际数量
                val customInfo = CustomIngredientInfo(
                    shelfLife = info.shelfLife,
                    storageMethod = info.storageMethod,
                    storageAdvice = info.storageAdvice,
                    freshness = info.freshness,
                    actualQuantity = info.actualQuantity,
                    unit = info.unit
                )

                // 调用完成并添加接口，传递用户修改的信息
                val request = CompleteAndAddRequest(
                    itemIds = listOf(item.id!!),
                    customInfo = customInfo
                )
                val response = api.completeAndAddToIngredients(request)

                if (response.success) {
                    _toastMessage.value = "${item.name} 已添加到食材库"
                    _currentProcessingItem.value = null
                    _inferredInfo.value = null
                    loadPendingItems()
                    loadCompletedItems()  // 刷新已完成列表
                } else {
                    _toastMessage.value = response.message ?: "添加失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "添加失败: ${e.message}"
                Log.e("ShoppingVM", "确认添加失败", e)
            } finally {
                _isLoading.value = false
            }
        }
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
     * 批量勾选完成（仅标记完成，不入库）
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
                    _toastMessage.value = "已标记完成${response.data ?: ids.size}个，请前往已完成列表入库"
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
     * 单个勾选完成（仅标记完成，不入库）
     */
    fun completeSingleItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val response = api.completeShoppingItems(mapOf("itemIds" to listOf(itemId)))
                if (response.success) {
                    _toastMessage.value = "已标记完成，请前往已完成列表入库"
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
     * 清空所有已完成采购项
     */
    fun clearAllCompleted() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = api.clearCompletedItems()
                if (response.success) {
                    _toastMessage.value = "已清空"
                    loadCompletedItems()
                } else {
                    _toastMessage.value = response.message ?: "清空失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = "清空失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 从食谱一键导入缺少的食材到采购列表（自动比对食材库已有食材） */
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
}
