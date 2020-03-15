package com.example.android.handwritinrecognition

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.activity_read.*
import org.zeromq.ZMQ

class ReadActivity : AppCompatActivity() {

    //-----------------------------------------------------------------------------------
    // PROPERTIES
    //-----------------------------------------------------------------------------------

    private val TAG: String = "READ_ACTIVITY"

    lateinit var crnn: CRNN

    lateinit var zmqContext: ZMQ.Context
    lateinit var zmqSocketImage: ZMQ.Socket
    lateinit var zmqSocketLabel: ZMQ.Socket

    lateinit var popupImprove: PopupWindow

    //-----------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        initActivity()
        initLayout()

        val byteArray = intent.getByteArrayExtra("CapturedImage")
        val imgWidth = intent.extras.getInt("Width")
        val imgHeight = intent.extras.getInt("Height")

        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, imgWidth, imgHeight, true)

        // Rotate bitmap
        val rotateMatrix = Matrix()
        rotateMatrix.postRotate(-90f)
        val rotatedBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0, imgWidth, imgHeight, rotateMatrix, true)

        iv_captured_image.setImageBitmap(rotatedBitmap)
        tv_predicted.text = requestRecognition(byteArray)

        // Option for improvement
        btn_correct.setOnClickListener {
            initPopup()

            // Display popup at center
            popupImprove.showAtLocation(mv_activity_read, Gravity.CENTER, 0, 0)
        }
    }

    //-----------------------------------------------------------------------------------

    private fun initActivity() {
        crnn = CRNN(assets)
        zmqContext = ZMQ.context(1)

        // Socket to send image
        zmqSocketImage = zmqContext.socket(ZMQ.REQ)
        zmqSocketImage.connect("tcp://192.168.137.1:8080")

        // Socket to send label
        zmqSocketLabel = zmqContext.socket(ZMQ.REQ)
        zmqSocketLabel.connect("tcp://192.168.137.1:8181")
    }

    private fun initLayout() {
        btn_retake.setOnClickListener {
            val cameraIntent = Intent(this, CameraActivity::class.java)
            startActivity(cameraIntent)
            finish()
        }
    }

    private fun requestRecognition(imageByteArray: ByteArray) : String {
        Log.i(TAG, "Sending image")
        zmqSocketImage.send(imageByteArray)

        Log.i(TAG, "Receiving recognition")
        val byteReply = zmqSocketImage.recv()
        val plainReply = String(byteReply)
        Log.i(TAG, "Received $plainReply")

        return plainReply
    }

    private fun sendLabel(label: String) {
        Log.i(TAG, "Sending label")
        zmqSocketLabel.send(label.toByteArray(Charsets.UTF_8))

        val byteReply = zmqSocketLabel.recv()
        Log.i(TAG, "Reply: ${byteReply.toString(Charsets.UTF_8)}")
    }

    //-----------------------------------------------------------------------------------
    // Popup window for improvement
    //-----------------------------------------------------------------------------------

    private fun initPopup() {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = layoutInflater.inflate(R.layout.popup_improve, null)
        val btnCancel = popupView.findViewById<Button>(R.id.btn_cancel)
        val btnSend = popupView.findViewById<LinearLayout>(R.id.btn_send)
        val editText = popupView.findViewById<EditText>(R.id.et_correct)

        popupImprove = PopupWindow(popupView, 1700, 500)
        popupImprove.elevation = 5f

        popupImprove.isFocusable = true
        popupImprove.update()

        editText.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(view: View?, focused: Boolean) {
                showHideKeyboard(view!!, focused)
            }
        })

        // Close popup on cancelling
        btnCancel.setOnClickListener {
            popupImprove.dismiss()
        }

        // Send label and close popup
        btnSend.setOnClickListener {
            val correctLabel = editText.text.toString()
            sendLabel(correctLabel)
            popupImprove.dismiss()

            val toast = Toast.makeText(this, "Thank you for your contribution", Toast.LENGTH_SHORT)
            toast.show()

            // Hide improve button if label already sent for current image
            btn_correct.visibility = View.INVISIBLE
        }
    }

    private fun showHideKeyboard(view: View, show: Boolean) {
        val inputService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            inputService.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } else {
            inputService.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
    }

    private fun hideKeyboard(view: View) {
        val inputService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputService.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

}
