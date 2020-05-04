/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.android.codelabs.paging.data.GithubRepository
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * ViewModel for the [SearchRepositoriesActivity] screen.
 * The ViewModel works with the [GithubRepository] to get the data.
 */
class SearchRepositoriesViewModel(
    private val repository: GithubRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val state: Flow<UiState>
    val accept: (UiAction) -> Unit

    init {
        val queryStateFlow = MutableStateFlow<UiAction>(
            UiAction.Search(query = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY)
        )
        accept = { queryStateFlow.tryEmit(it) }

        state = queryStateFlow
            .filterIsInstance<UiAction.Search>()
            .map { it.query }
            .flatMapLatest { query ->
                searchRepo(query)
                    .withIndex()
                    .map { (index, pagingData) ->
                        UiState(
                            query = query,
                            pagingData = pagingData,
                            queryChanged = index == 0
                        )
                    }
            }
            .onStart { emit(UiState()) }
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
    }

    private fun searchRepo(queryString: String): Flow<PagingData<Repo>> =
        repository.getSearchResultStream(queryString)
            .cachedIn(viewModelScope)
}

sealed class UiAction {
    data class Search(val query: String) : UiAction()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val queryChanged: Boolean = false,
    val pagingData: PagingData<Repo> = PagingData.empty()
)

private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val DEFAULT_QUERY = "Android"