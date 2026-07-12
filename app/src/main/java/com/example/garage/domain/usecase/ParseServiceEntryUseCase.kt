package com.example.garage.domain.usecase

import com.example.garage.domain.model.ServiceCategory
import java.util.Locale
import javax.inject.Inject

/** Best-effort guess at a record's fields from a free-text sentence. */
data class ParsedServiceEntry(
    val category: ServiceCategory?,
    val odometer: Int?,
    val title: String
)

/**
 * Powers the "describe it" field on the log-a-service screen. This is a
 * simple keyword/regex heuristic so the feature works fully offline with
 * no API key. Swapping in a real LLM call later (e.g. the Anthropic API,
 * or on-device ML Kit entity extraction) only means replacing the body of
 * [invoke] - the ViewModel and UI never need to change, since they only
 * depend on [ParsedServiceEntry].
 */
class ParseServiceEntryUseCase @Inject constructor() {

    private val categoryKeywords = mapOf(
        ServiceCategory.OIL_CHANGE to listOf("oil"),
        ServiceCategory.TIRES to listOf("tire", "tyre", "rotate", "rotation"),
        ServiceCategory.BRAKES to listOf("brake", "pad", "rotor"),
        ServiceCategory.BATTERY to listOf("battery"),
        ServiceCategory.FLUIDS to listOf("coolant", "transmission fluid", "brake fluid", "washer fluid"),
        ServiceCategory.INSPECTION to listOf("inspection", "inspected", "check up", "checkup")
    )

    private val mileageRegex = Regex("""(\d{1,3}(?:,\d{3})+|\d{4,6})\s*(mi|miles)?""")

    operator fun invoke(input: String): ParsedServiceEntry {
        val lower = input.lowercase(Locale.US)

        val category = categoryKeywords.entries.firstOrNull { (_, keywords) ->
            keywords.any { lower.contains(it) }
        }?.key

        val odometer = mileageRegex.find(input)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull()

        // Always derive the title from what the user actually typed, not
        // just the matched category - "Changed oil and rotated tires at
        // 61,480" should keep that detail rather than collapsing to
        // "Oil change".
        val title = input.trim().replaceFirstChar { it.uppercaseChar() }

        return ParsedServiceEntry(category = category, odometer = odometer, title = title)
    }
}
