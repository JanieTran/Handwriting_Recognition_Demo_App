package com.example.android.handwritinrecognition

import android.content.Context
import android.view.textservice.*
import java.lang.StringBuilder
import java.util.*


class SpellChecker(context: Context) : SpellCheckerSession.SpellCheckerSessionListener {
    val tsm = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
    val scs = tsm.newSpellCheckerSession(null, Locale.ENGLISH, this, false)

    lateinit var correction : Array<String>

    init {
        if (scs != null) {
            println("---SPELLCHECKER: Initialised SpellCheckerSession successfully")
        } else {
            println("---SPELLCHECKER: Null SpellCheckerSession")
        }
    }

    fun getCorrectionFor(input: String) {
        correction = input.split(' ').toTypedArray()
        scs.getSentenceSuggestions(arrayOf(TextInfo(input)), 1)
    }

    //-----------------------------------------------------------------------------------
    // IMPLEMENT: SpellCheckerListener
    //-----------------------------------------------------------------------------------

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        val stringBuilder = StringBuilder("")

        for (i in 0 until results!!.size) {
            val n = results[i].suggestionsCount

            println("---SPELLCHECKER: word suggestion counts $n")

            for (j in 0 until n) {
                val suggestionInfos = results[i].getSuggestionsInfoAt(j)
                if (suggestionInfos.suggestionsCount > 0) {
                    correction[j] = suggestionInfos.getSuggestionAt(0)
                }
            }
        }
    }

    override fun onGetSuggestions(p0: Array<out SuggestionsInfo>?) {
    }

}