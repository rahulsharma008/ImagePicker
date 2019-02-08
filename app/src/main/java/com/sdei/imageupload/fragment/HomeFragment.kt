package com.sdei.imageupload.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.sdei.imageupload.R
import com.sdei.imageupload.data.DataPart
import com.sdei.imageupload.dialog.PickSelectionDialog
import com.sdei.imageupload.dialog.ProgressDialog
import com.sdei.imageupload.helper.ImageHelper
import com.sdei.imageupload.helper.PermissionHelper
import com.sdei.imageupload.helper.VolleyMultipartRequest
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    private var btn: ImageButton? = null
    private var imageview: ImageView? = null
    private var permissionHelper: PermissionHelper? = null
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var rQueue: RequestQueue? = null

    private var dialog: ProgressDialog? = null
    private var imageHelper = ImageHelper()

    companion object {
        const val REQUEST_PICK_DIALOG = 101
        const val ACTION_CAMERA = 102
        const val ACTION_GALLERY = 103
        const val ACTION_REMOVE_PHOTO = 104
        const val IMAGE_DIRECTORY = "/sdei_directory"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val homeView = inflater.inflate(R.layout.fragment_home, container, false)
        btn = homeView!!.findViewById(R.id.btn)
        imageview = homeView.findViewById(R.id.iv)
        setViewListeners()
        return homeView
    }

    private fun setViewListeners() {

        btn!!.setOnClickListener {

            permissionHelper!!.request(permissions, object : PermissionHelper.PermissionCallback {

                override fun permissionNotInManifest(permissionName: String) {
                    Toast.makeText(
                        context,
                        "Permission ($permissionName) Not Declared in manifest",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionGranted() {
                    showPictureDialog()
                }

                override fun onPermissionDeniedBySystem() {
                    Toast.makeText(
                        context,
                        "Permission denied by system",
                        Toast.LENGTH_SHORT
                    ).show()
                    permissionHelper?.openAppDetailsActivity()
                }

            })

        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        permissionHelper = PermissionHelper(this, 100)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHelper != null) {
            permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showPictureDialog() {

        val ft = fragmentManager!!.beginTransaction()
        val prev = fragmentManager!!.findFragmentByTag("dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        val pickDialog = PickSelectionDialog()
        pickDialog.setTargetFragment(this, REQUEST_PICK_DIALOG)
        pickDialog.show(ft, "dialog")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_DIALOG -> {
                    performAction(data)
                }
                ImageHelper.GALLERY -> {
                    if (data != null) {
                        val contentURI = data.data
//                        try {
//                            val bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, contentURI)
//                            imageview!!.setImageBitmap(bitmap)
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                            Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
//                        }
                        var filePath: String? = ""
                        if (contentURI != null && "content" == contentURI.scheme) {
                            val cursor = activity!!.contentResolver.query(
                                contentURI,
                                arrayOf(android.provider.MediaStore.Images.ImageColumns.DATA),
                                null,
                                null,
                                null
                            )
                            if (cursor != null) {
                                cursor.moveToFirst()
                                filePath = cursor.getString(0)
                                cursor.close()
                            }
                        } else if (contentURI != null) {
                            filePath = contentURI.path
                        }
                        uploadImage(filePath!!)
                    }
                }
                ImageHelper.CAMERA -> {
                    val image = File(cameraFilePath)
                    if (image.exists()) {
                        val bitmap = BitmapFactory.decodeFile(image.absolutePath)
                        imageview!!.setImageBitmap(bitmap)
                    }
                    uploadImage(cameraFilePath)
                }
            }
        }

    }

    private var cameraFilePath = ""

    private fun performAction(data: Intent?) {
        if (data != null && data.hasExtra("action")) {
            val action = data.getIntExtra("action", -1)
            when (action) {
                ACTION_REMOVE_PHOTO -> {
                    imageview!!.setImageBitmap(null)
                    imageview!!.background = ContextCompat.getDrawable(context!!, R.mipmap.ic_launcher)
                }
                ACTION_GALLERY -> imageHelper.choosePhotoFromGallary(this@HomeFragment)
                ACTION_CAMERA -> {
                    cameraFilePath = imageHelper.takePhotoFromCamera(this@HomeFragment)
                }
            }
        }
    }

    private fun uploadImage(filePath: String?) {
        log("Uploading image")
        val compressedBitmap = imageHelper.getCompressedBitmap(filePath!!)
        showProgressDialog()
        val volleyMultipartRequest = object : VolleyMultipartRequest(
            Request.Method.POST,
            "https://demonuts.com/Demonuts/JsonTest/Tennis/uploadfile.php?",
            Response.Listener { response ->
                rQueue!!.cache.clear()
                hideProgressDialog()
                try {
                    val jsonObject = JSONObject(String(response.data))
                    log(jsonObject.getString("message"))
                    jsonObject.toString().replace("\\\\", "")
                    if (jsonObject.getString("status") == "true") {
                        val dataArray = jsonObject.getJSONArray("data")
                        var url = ""
                        for (i in 0 until dataArray.length()) {
                            val dataObj = dataArray.getJSONObject(i)
                            url = dataObj.optString("pathToFile")
                        }
                        log("Image url: $url")
                        Picasso.get().load(url).into(imageview)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                log(error.message!!)
                hideProgressDialog()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return HashMap()
            }

            override fun getByteData(): Map<String, DataPart> {
                val params = java.util.HashMap<String, DataPart>()
                val imageName = System.currentTimeMillis()
                val data = DataPart()
                data.initParams("$imageName.png", imageHelper.getFileDataFromDrawable(compressedBitmap!!))
                params["filename"] = data
                return params
            }

        }

        volleyMultipartRequest.retryPolicy = DefaultRetryPolicy(
            0,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        rQueue = Volley.newRequestQueue(context)
        rQueue!!.add(volleyMultipartRequest)
    }

    private fun showProgressDialog() {
        if (dialog == null) {
            dialog = ProgressDialog(context!!, "Uploading image...")
            dialog?.show()
        }
    }

    private fun hideProgressDialog() {
        if (dialog != null) {
            dialog?.dismiss()
            dialog = null
        }
    }

    private fun log(message: String) {
        Log.e(TAG, message)
    }

}