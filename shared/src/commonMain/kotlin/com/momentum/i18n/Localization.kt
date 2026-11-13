package com.momentum.i18n

import com.momentum.util.formatFixed
import kotlinx.serialization.Serializable

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
    fun translate(key: String, language: AppLanguage = this.language): String = key
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
    fun name(id: String, english: String): String = english
    fun steps(id: String, english: List<String>): List<String> = english
}
