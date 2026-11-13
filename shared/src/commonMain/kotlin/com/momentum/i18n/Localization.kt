package com.momentum.i18n

import com.momentum.model.GeneratedData
import com.momentum.util.MomentumJson
import com.momentum.util.formatFixed
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
enum class AppLanguage {
    en, es, pt;
    val id get() = name
    val nativeName get() = when (this) { en -> "English"; es -> "Español"; pt -> "Português" }
    val localeTag get() = when (this) { en -> "en-US"; es -> "es-419"; pt -> "pt-BR" }
    companion object {
        fun detect(preferred: List<String>): AppLanguage = preferred.firstNotNullOfOrNull { tag ->
            entries.firstOrNull { it.name == tag.lowercase().take(2) }
        } ?: en
    }
}

object Loc {
    var language: AppLanguage = AppLanguage.en
    private val tables: Map<AppLanguage, Map<String, String>> by lazy {
        try {
            MomentumJson.decodeFromString<Map<String, Map<String, String>>>(GeneratedData.stringsJson)
                .mapNotNull { (code, table) -> AppLanguage.entries.firstOrNull { it.name == code }?.let { it to table } }
                .toMap()
        } catch (e: Exception) {
            throw IllegalStateException("data/i18n/strings.json failed to decode", e)
        }
    }
    fun translatedKeys(language: AppLanguage): Set<String> = tables[language]?.keys ?: emptySet()
    fun translate(key: String, language: AppLanguage = this.language): String =
        if (language == AppLanguage.en) key else tables[language]?.get(key) ?: key
    fun text(key: String, args: List<String> = emptyList()): String {
        var result = translate(key, language)
        args.forEachIndexed { index, value -> result = result.replace("{$index}", value) }
        return result
    }
    fun list(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> text("{0} and {1}", listOf(items.dropLast(1).joinToString(", "), items.last()))
    }
    fun number(value: Double, maxFractionDigits: Int = 1): String {
        var result = formatFixed(value, maxFractionDigits)
        if ('.' in result) result = result.trimEnd('0').trimEnd('.')
        if (result == "-0") result = "0"
        return if (language == AppLanguage.en) result else result.replace('.', ',')
    }
}
fun L(key: String, vararg args: Any): String = Loc.text(key, args.map { it.toString() })
fun Lp(count: Int, one: String, other: String): String = Loc.text(if (count == 1) one else other, listOf(count.toString()))
object ExerciseText {
    @Serializable data class Entry(val name: String, val steps: List<String>)
    private val tables: Map<AppLanguage, Map<String, Entry>> by lazy {
        try {
            MomentumJson.decodeFromString<Map<String, Map<String, Entry>>>(GeneratedData.exerciseTranslationsJson)
                .mapNotNull { (code, table) -> AppLanguage.entries.firstOrNull { it.name == code }?.let { it to table } }
                .toMap()
        } catch (e: Exception) {
            throw IllegalStateException("data/i18n/exercises.json failed to decode", e)
        }
    }
    fun entry(id: String, language: AppLanguage): Entry? =
        if (language == AppLanguage.en) null else tables[language]?.get(id)
    fun name(id: String, english: String): String = entry(id, Loc.language)?.name ?: english
    fun steps(id: String, english: List<String>): List<String> {
        val entry = entry(id, Loc.language)
        return if (entry == null || entry.steps.size != english.size) english else entry.steps
    }
    fun translatedIDs(language: AppLanguage): Set<String> = tables[language]?.keys ?: emptySet()
}
