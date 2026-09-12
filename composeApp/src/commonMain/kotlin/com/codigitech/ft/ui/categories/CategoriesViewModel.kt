package com.codigitech.ft.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.usecase.AddCategoryUseCase
import com.codigitech.ft.domain.usecase.DeleteCategoryUseCase
import com.codigitech.ft.domain.usecase.Result
import com.codigitech.ft.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    categories: CategoryRepository,
    private val addCategory: AddCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {
    val categories: StateFlow<List<Category>> =
        categories.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun save(id: Long?, name: String, icon: String, colorHex: String, type: CategoryType) {
        viewModelScope.launch {
            val result = if (id == null) addCategory(name, icon, colorHex, type) else updateCategory(id, name, icon, colorHex)
            if (result is Result.Failure) _messages.tryEmit(result.message)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            when (val r = deleteCategory(id)) {
                is Result.Failure -> _messages.tryEmit(r.message)
                is Result.Success -> _messages.tryEmit("Category deleted; its transactions moved to Uncategorized")
            }
        }
    }
}
