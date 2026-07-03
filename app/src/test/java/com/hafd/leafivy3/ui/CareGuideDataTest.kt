package com.hafd.leafivy3.ui

import com.hafd.leafivy3.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareGuideDataTest {

    @Test
    fun normalizeLabel_stripsPrefixAndUnderscore() {
        val input = "Potato___early_blight"
        val normalized = normalizeLabel(input)
        assertEquals("early blight", normalized.lowercase())
    }

    @Test
    fun careGuideForDisease_returnsHealthyGuide() {
        val guide = careGuideForDisease("healthy")
        assertEquals(R.string.care_healthy_summary, guide.summary)
        assertTrue(guide.immediateSteps.isNotEmpty())
    }
}
