package com.cuongnt.tos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.line.tms.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mCM: String? = null
    private var mUM: ValueCallback<Uri>? = null
    private var mUMA: ValueCallback<Array<Uri>>? = null
    private val FCR = 1

    private lateinit var webview: WebView
    private lateinit var offline: ConstraintLayout
    private val TAG = "Permission"
    private val CAM_PERMISSION_REQUEST_CODE = 101
    private val STORE_PERMISSION_REQUEST_CODE = 102

    private val LOCATION_PERMISSION_REQUEST_CODE = 103

    lateinit var networkCallBack: ConnectivityManager.NetworkCallback
    lateinit var connectivityManager: ConnectivityManager
    lateinit var networkRequest: NetworkRequest



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        setupPermissionsCam()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= 21) {
            var results: Array<Uri>? = null
            //Check if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return
                    }
                    if (data == null) {
                        //Capture Photo if no image available
                        if (mCM != null) {
                            results = arrayOf(Uri.parse(mCM))
                        }
                    } else {
                        val dataString = data.dataString
                        if (dataString != null) {
                            results = arrayOf(Uri.parse(dataString))
                        }
                    }
                }
            }
            mUMA!!.onReceiveValue(results)
            mUMA = null
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return
                val result =
                    if (data == null || resultCode != Activity.RESULT_OK) null else data.data
                mUM!!.onReceiveValue(result)
                mUM = null
            }
        }
    }

    private fun setupPermissionsCam() {
        val permissionCam = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        if (permissionCam != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied")
            makeRequestCam()
        } else {
            setUpWebview()
        }
    }

    private fun setupPermissionsLocation() {
        val permissionLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied")
            makeRequestLocation()
        } else {
            setUpWebview()
        }
    }

    private fun setupPermissionsStore() {
        val permissionStore = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissionStore != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to store denied")
            makeRequestStore()
        } else {
            setUpWebview()
        }
    }

    private fun makeRequestCam() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAM_PERMISSION_REQUEST_CODE
        )
    }

    private fun makeRequestStore() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORE_PERMISSION_REQUEST_CODE
        )
    }

    private fun makeRequestLocation() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            CAM_PERMISSION_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bạn Cần Cấp quyền chon App", Toast.LENGTH_LONG)
                    setupPermissionsCam()
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                    setupPermissionsStore()
                }
            }

            STORE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bạn Cần Cấp quyền chon App", Toast.LENGTH_LONG)
                    setupPermissionsStore()
                }
                else {
                    Log.i(TAG, "Permission has been granted by user")
                    setupPermissionsLocation()
                }
            }

            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bạn Cần Cấp quyền chon App", Toast.LENGTH_LONG)
                    setupPermissionsLocation()
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                    setUpWebview()
                }
            }
        }
    }

    private fun setUpWebview() {
        webview = findViewById<WebView>(R.id.webview)
        offline = findViewById<ConstraintLayout>(R.id.internetOffline)
        webview.webViewClient = GMDWebView()
        webview.settings.loadsImagesAutomatically = true
        webview.settings.javaScriptEnabled = true
        webview.settings.allowFileAccess = true
        webview.settings.domStorageEnabled = true
        webview.settings.setAppCacheEnabled(true)
        webview.settings.databaseEnabled = true
        webview.settings.allowContentAccess = true
        webview.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webview.settings.javaScriptCanOpenWindowsAutomatically = true

        webview.settings.setGeolocationEnabled(true)
        webview.clearCache(false)
        webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webview.webChromeClient = object : WebChromeClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                if (mUMA != null) {
                    mUMA!!.onReceiveValue(null)
                }
                mUMA = filePathCallback
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(this@MainActivity.getPackageManager()) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
//                        takePictureIntent.putExtra("PhotoPath", mCM)
                    } catch (ex: IOException) {
                        Log.e("Webview", "Image file creation failed", ex)
                    }

                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath()
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent>
                if (takePictureIntent != null) {
                    intentArray = arrayOf(takePictureIntent)
                } else intentArray = arrayOf(Intent())

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                startActivityForResult(chooserIntent, FCR)
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }
        network()
    }

    override fun onBackPressed() {
        if (webview.canGoBack())
            webview.goBack()
        else
            super.onBackPressed()
    }

    // Create an image file
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir: File = getExternalFilesDir(null)!!.absoluteFile
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun network() {
        networkCallBack =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d("nam", "nam")
                    runOnUiThread(Runnable {
                        if (offline.visibility == View.VISIBLE) {
                            offline.visibility = View.GONE
                        }
                        if (webview.visibility == View.GONE) {
                            webview.visibility = View.VISIBLE
                        }
                        webview.loadUrl("https://tos.gemadeptlogistics.com.vn:4380")
                    })
                }

                override fun onLost(network: Network) {
                    runOnUiThread(Runnable {
                        if (offline.visibility == View.GONE) {
                            offline.visibility = View.VISIBLE
                        }
                        if (webview.visibility == View.VISIBLE) {
                            webview.visibility = View.GONE
                        }
                    })
                }

            }
        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallBack)
        if (!isConnect()) {
            if (offline.visibility == View.GONE) {
                offline.visibility = View.VISIBLE
            }
            if (webview.visibility == View.VISIBLE) {
                webview.visibility = View.GONE
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        connectivityManager.unregisterNetworkCallback(networkCallBack)
        super.onDestroy()
    }

    private fun isConnect(): Boolean {
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            //for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            //for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}