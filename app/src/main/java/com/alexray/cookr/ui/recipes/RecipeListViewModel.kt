package com.alexray.cookr.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexray.cookr.data.model.Recipe
import com.alexray.cookr.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecipeListState {
    data object Loading : RecipeListState
    data object Empty : RecipeListState
    data class Content(val recipes: List<Recipe>) : RecipeListState
    data class Failed(val message: String) : RecipeListState
}

class RecipeListViewModel : ViewModel() {

    private val repo = RecipeRepository()

    private val _state = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val state: StateFlow<RecipeListState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = RecipeListState.Loading
        viewModelScope.launch {
            try {
                val recipes = repo.list()
                _state.value =
                    if (recipes.isEmpty()) RecipeListState.Empty
                    else RecipeListState.Content(recipes)
            } catch (t: Throwable) {
                _state.value = RecipeListState.Failed(t.message ?: "Could not load recipes")
            }
        }
    }
}
