package com.example.parsaniahardik.kotlinselectimagegallery.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.parsaniahardik.kotlinselectimagegallery.BaseActivity
import com.example.parsaniahardik.kotlinselectimagegallery.helper.ImageHelper
import com.example.parsaniahardik.kotlinselectimagegallery.helper.PermissionHelper
import com.example.parsaniahardik.kotlinselectimagegallery.dialog.PickSelectionDialog
import com.example.parsaniahardik.kotlinselectimagegallery.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class HomeFragment : Fragment() {

    private var btn: ImageButton? = null
    private var imageview: ImageView? = null
    private var permissionHelper: PermissionHelper? = null
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    companion object {
        const val REQUEST_PICK_DIALOG = 101
        const val ACTION_CAMERA = 102
        const val ACTION_GALLERY = 103
        const val ACTION_REMOVE_PHOTO = 104
        const val IMAGE_DIRECTORY = "/demonuts"
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
                    Toast.makeText(context,
                            "Permission ($permissionName) Not Declared in manifest",
                            Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionGranted() {
                    showPictureDialog()
                }

                override fun onPermissionDeniedBySystem() {
                    Toast.makeText(context,
                            "Permission denied by system",
                            Toast.LENGTH_SHORT).show()
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
                        try {
                            val bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, contentURI)
                            imageview!!.setImageBitmap(bitmap)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                ImageHelper.CAMERA -> {
                    val thumbnail = data!!.extras!!.get("data") as Bitmap
                    imageview!!.setImageBitmap(thumbnail)
                    saveImage(thumbnail)
                }
            }
        }

    }

    private fun performAction(data: Intent?) {
        if (data != null && data.hasExtra("action")) {
            val action = data.getIntExtra("action", -1)
            when (action) {
                ACTION_REMOVE_PHOTO -> {
                    imageview!!.setImageBitmap(null)
                    imageview!!.background = ContextCompat.getDrawable(context!!, R.mipmap.ic_launcher)
                }
                ACTION_GALLERY -> ImageHelper().choosePhotoFromGallary(this@HomeFragment)
                ACTION_CAMERA -> ImageHelper().takePhotoFromCamera(this@HomeFragment)
            }
        }
    }

    private fun saveImage(myBitmap: Bitmap): String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val wallpaperDirectory = File(
                (Environment.getExternalStorageDirectory()).toString() + IMAGE_DIRECTORY)
        // have the object build the directory structure, if needed.
        Log.e("fee", wallpaperDirectory.toString())
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }

        try {
            Log.e("heel", wallpaperDirectory.toString())
            val f = File(wallpaperDirectory, ((Calendar.getInstance()
                    .timeInMillis).toString() + ".jpg"))
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(context, arrayOf(f.path), arrayOf("image/jpeg"), null)
            fo.close()
            Log.e("TAG", "File Saved::--->" + f.absolutePath)

            return f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        return ""
    }

}