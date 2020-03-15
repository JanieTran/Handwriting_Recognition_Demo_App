package com.example.android.handwritinrecognition

import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_menu.*

class MenuActivity : AppCompatActivity() {

    //-----------------------------------------------------------------------------------
    // PROPERTIES
    //-----------------------------------------------------------------------------------

    // Log tag
    private val TAG: String = "MENU_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val handler = Handler()
        handler.postDelayed({
            // Display loading spinner
            pb_spinner.visibility = View.VISIBLE
        }, 1000)

        handler.postDelayed({
            // Check for camera on device and ask for permission
            checkCamera()
        }, 4000)
    }

    //-----------------------------------------------------------------------------------
    // CAMERA PERMISSION
    //-----------------------------------------------------------------------------------

    private fun checkCamera() {
        // Check if device has camera
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.i(TAG, "Camera found")

            // Check for camera permission
            if (isCameraPermissionGranted()) {
                Log.i(TAG, "Camera permission granted")
                openCamera()
            }

            // Else, ask for permission
            else {
                Log.e(TAG, "Camera permission denied")
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1)
            }
        }

        // If camera not found, log error and show toast
        else {
            Log.e(TAG, "Camera not found")
            val toast = Toast.makeText(this, "Camera not found", Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        try {
            val permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            return permission == PackageManager.PERMISSION_GRANTED
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception ${e.reason}")
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open Camera Activity
                Log.i(TAG, "Camera permission granted")
                openCamera()
            } else {
                // Permission denied, make toast message
                Log.e(TAG, "Camera permission denied")
                val toast = Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    //-----------------------------------------------------------------------------------
    // INTENT NAVIGATION
    //-----------------------------------------------------------------------------------

    private fun openCamera() {
        val cameraIntent = Intent(this, CameraActivity::class.java)
        startActivity(cameraIntent)
    }
}
