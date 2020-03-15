package com.example.android.handwritinrecognition

import java.lang.Exception

//-----------------------------------------------------------
// Decode output from CTC
//-----------------------------------------------------------

fun decodeCTCResults(results : LongArray) : String {
    val decoded = Array(results.size) { ' ' }

    try {
        for (i in 0 until results.size)
            decoded[i] = CHAR_SET[results.get(i).toInt()]
    }
    catch (ex : Exception) {
        println("---decodedCTCResults: EXCEPTION")
        ex.printStackTrace()
    }

    return decoded.joinToString("")
}
