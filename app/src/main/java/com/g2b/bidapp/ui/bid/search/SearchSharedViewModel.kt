package com.g2b.bidapp.ui.bid.search

import androidx.lifecycle.ViewModel
import com.g2b.bidapp.domain.model.SearchParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SearchSharedViewModel @Inject constructor() : ViewModel() {

    private val _pendingParams = MutableStateFlow<SearchParams?>(null)
    val pendingParams: StateFlow<SearchParams?> = _pendingParams.asStateFlow()

    fun submit(params: SearchParams) {
        _pendingParams.value = params
    }

    fun consume() {
        _pendingParams.value = null
    }
}