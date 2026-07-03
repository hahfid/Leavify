package com.hafd.leafivy3.ui

import com.hafd.leafivy3.R

internal data class CareGuide(
    val summary: Int,
    val immediateSteps: List<Int>,
    val preventionSteps: List<Int>,
    val followUp: List<Int>
)

internal fun careGuideForDisease(disease: String): CareGuide {
    return when (disease.trim().lowercase()) {
        "frog eye leaf spot" -> CareGuide(
            summary = R.string.care_frogeye_summary,
            immediateSteps = listOf(
                R.string.care_frogeye_step1,
                R.string.care_frogeye_step2,
                R.string.care_frogeye_step3
            ),
            preventionSteps = listOf(
                R.string.care_frogeye_prev1,
                R.string.care_frogeye_prev2,
                R.string.care_frogeye_prev3
            ),
            followUp = listOf(
                R.string.care_frogeye_follow1,
                R.string.care_frogeye_follow2
            )
        )
        "powdery mildew" -> CareGuide(
            summary = R.string.care_powdery_summary,
            immediateSteps = listOf(
                R.string.care_powdery_step1,
                R.string.care_powdery_step2,
                R.string.care_powdery_step3
            ),
            preventionSteps = listOf(
                R.string.care_powdery_prev1,
                R.string.care_powdery_prev2,
                R.string.care_powdery_prev3
            ),
            followUp = listOf(
                R.string.care_powdery_follow1,
                R.string.care_powdery_follow2
            )
        )
        "rust" -> CareGuide(
            summary = R.string.care_rust_summary,
            immediateSteps = listOf(
                R.string.care_rust_step1,
                R.string.care_rust_step2,
                R.string.care_rust_step3
            ),
            preventionSteps = listOf(
                R.string.care_rust_prev1,
                R.string.care_rust_prev2,
                R.string.care_rust_prev3
            ),
            followUp = listOf(
                R.string.care_rust_follow1,
                R.string.care_rust_follow2
            )
        )
        "scab" -> CareGuide(
            summary = R.string.care_scab_summary,
            immediateSteps = listOf(
                R.string.care_scab_step1,
                R.string.care_scab_step2,
                R.string.care_scab_step3
            ),
            preventionSteps = listOf(
                R.string.care_scab_prev1,
                R.string.care_scab_prev2,
                R.string.care_scab_prev3
            ),
            followUp = listOf(
                R.string.care_scab_follow1,
                R.string.care_scab_follow2
            )
        )
        "healthy" -> CareGuide(
            summary = R.string.care_healthy_summary,
            immediateSteps = listOf(
                R.string.care_healthy_step1,
                R.string.care_healthy_step2
            ),
            preventionSteps = listOf(
                R.string.care_healthy_prev1,
                R.string.care_healthy_prev2
            ),
            followUp = listOf(
                R.string.care_healthy_follow1
            )
        )
        else -> CareGuide(
            summary = R.string.care_unknown_summary,
            immediateSteps = listOf(
                R.string.care_unknown_step1
            ),
            preventionSteps = listOf(
                R.string.care_unknown_prev1
            ),
            followUp = listOf(
                R.string.care_unknown_follow1
            )
        )
    }
}

internal fun normalizeLabel(raw: String): String {
    val trimmed = raw.trim()
    val withoutPrefix = if ("___" in trimmed) {
        trimmed.substringAfterLast("___")
    } else {
        trimmed
    }
    return withoutPrefix.replace('_', ' ').replace("  ", " ").trim()
        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
