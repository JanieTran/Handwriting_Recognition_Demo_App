package com.example.android.handwritinrecognition

import android.content.res.AssetManager
import android.graphics.*
import android.util.Log
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.nio.FloatBuffer
import kotlin.math.pow

class CRNN(private val assetManager: AssetManager) {
    val TAG = "CRNN"
    val inferenceInterface = TensorFlowInferenceInterface(assetManager, "frozen_model_custom.pb")

    fun predict(bitmap: Bitmap) : String {
        // Resize and convert to greyscale
        val resizedBitmap = resizeImageForInput(bitmap)
        val imageArray = convertToInput(resizedBitmap)

        // Placeholder for prediction
        val result = LongArray(SEQUENCE_LENGTH)

        // Feed input to input layer
        inferenceInterface.feed(INPUT_NODE, imageArray,
            1, INPUT_WIDTH.toLong(), INPUT_HEIGHT.toLong(), INPUT_CHANNEL.toLong())

        // Feed sequence length to CRNN
        inferenceInterface.feed(SEQ_LEN_NODE, intArrayOf(SEQUENCE_LENGTH), 1)

        // Run output node: CTCBeamSearchDecoder
        inferenceInterface.run(arrayOf(OUTPUT_NODE))

        // Get prediction from model
        inferenceInterface.fetch(OUTPUT_NODE, result)

        return decodeCTCResults(result)
    }

    //-----------------------------------------------------------
    // Resize and convert to greyscale for input
    //-----------------------------------------------------------

    private fun resizeImageForInput(bitmap: Bitmap) : Bitmap {
        // Resize image to fit model input
        return Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, false)
    }

    private fun convertToInput(bitmap: Bitmap) : FloatArray {
        val flattened = FloatArray(INPUT_WIDTH * INPUT_HEIGHT)
        var i = 0

        for (x in 0 until INPUT_WIDTH) {
            for (y in 0 until INPUT_HEIGHT) {
                // Convert pixel to greyscale
                val pixel = bitmap.getPixel(x, y)
                val grey = convertToGreyscale(pixel)

                flattened[i++] = grey
            }
        }

        return flattened
    }

    private fun convertToGreyscale(pixel : Int) : Float {
        // Extract color channels
        var red = Color.red(pixel).toFloat()
        var green = Color.green(pixel).toFloat()
        var blue = Color.blue(pixel).toFloat()

        // Normalise and gamma correction
        red = (red / 255f).pow(2.2f)
        green = (green / 255f).pow(2.2f)
        blue = (blue / 255f).pow(2.2f)

        // Luminance
        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
        return luminance.pow(1/2.2).toFloat()
    }
}
