package com.hafd.leafivy3.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LeafivyUiStateTest {

    @Test
    fun defaultState_isIdle() {
        val state = LeafivyUiState()
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.predictions.isNotEmpty())
    }
}
