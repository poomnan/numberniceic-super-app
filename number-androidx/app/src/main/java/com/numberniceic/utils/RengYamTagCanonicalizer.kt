package com.numberniceic.utils

import com.numberniceic.data.rengyam.TagDetail
import com.numberniceic.data.rengyam.Wanpra

object RengYamTagCanonicalizer {
    private val canonicalMap = mapOf(
        "วันบอด" to "บอด",
        "วันอุบาทว์/อุบาสน" to "อุบาทว์",
        "กาลกรรณี" to "กาลกิณี",
        "ทับทิมไฟ" to "ทักทินไฟ"
    )

    fun canonical(tag: String?): String {
        val clean = tag?.trim().orEmpty()
        if (clean.isEmpty()) return ""
        canonicalAgniknirod(clean)?.let { return it }
        return canonicalMap[clean] ?: clean
    }

    private fun canonicalAgniknirod(tag: String): String? {
        if (!tag.contains("อัคนิโรธ")) return null
        val explicitTopic = tag.substringAfter("(-", "").substringBefore(")", "").trim()
        val rawTopic = if (explicitTopic.isNotEmpty()) {
            explicitTopic
        } else {
            tag.replace("อัคนิโรธน์", "")
                .replace("อัคนิโรธ", "")
                .replace("(", "")
                .replace(")", "")
                .replace("-", "")
                .trim()
        }
        val topic = rawTopic.replace(" ", "")
        return if (topic.isEmpty()) "อัคนิโรธ" else "อัคนิโรธ (-$topic)"
    }

    fun normalize(wp: Wanpra) {
        wp.calendarDisplayTags = canonicalizeTags(wp.calendarDisplayTags)
        wp.kalTags = canonicalizeTags(wp.kalTags)
        wp.dithiTags = canonicalizeTags(wp.dithiTags)
        wp.dayTypeTags = canonicalizeTags(wp.dayTypeTags)
        wp.warningTags = canonicalizeTags(wp.warningTags)
        wp.displayTags = canonicalizeTags(wp.displayTags)
        wp.displayTagsPrioritized = canonicalizeTags(wp.displayTagsPrioritized)
        wp.myhoraDisplayTags = canonicalizeTags(wp.myhoraDisplayTags)
        wp.myhoraDisplayTagsPrioritized = canonicalizeTags(wp.myhoraDisplayTagsPrioritized)
        wp.mahamodoDisplayTags = canonicalizeTags(wp.mahamodoDisplayTags)
        wp.mahamodoDisplayTagsPrioritized = canonicalizeTags(wp.mahamodoDisplayTagsPrioritized)
        wp.tagDetails = canonicalizeDetails(wp.tagDetails)
        wp.myhoraTagDetails = canonicalizeDetails(wp.myhoraTagDetails)
        wp.mahamodoTagDetails = canonicalizeDetails(wp.mahamodoTagDetails)
    }

    private fun canonicalizeTags(tags: List<String>?): List<String>? {
        return tags
            ?.map(::canonical)
            ?.filter { it.isNotBlank() }
            ?.distinct()
    }

    private fun appendUniqueParts(first: String, second: String): String {
        val parts = (first.split("/", "•", ",") + second.split("/", "•", ","))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return parts.joinToString(" / ")
    }

    private fun canonicalizeDetails(details: List<TagDetail>?): List<TagDetail>? {
        if (details.isNullOrEmpty()) return details
        val merged = linkedMapOf<String, TagDetail>()
        details.forEach { detail ->
            val canonical = canonical(detail.tag ?: detail.displayTag)
            if (canonical.isBlank()) return@forEach
            val existing = merged[canonical]
            if (existing == null) {
                merged[canonical] = detail.copy(
                    tag = canonical,
                    displayTag = canonical
                )
            } else {
                merged[canonical] = existing.copy(
                    source = appendUniqueParts(existing.source.orEmpty(), detail.source.orEmpty()),
                    school = appendUniqueParts(existing.school.orEmpty(), detail.school.orEmpty())
                )
            }
        }
        return merged.values.toList()
    }
}
