package com.example.parsaniahardik.kotlinselectimagegallery.helper

import android.content.Intent
import android.provider.MediaStore
import androidx.fragment.app.Fragment

class ImageHelper() {

    companion object {
        const val IMAGE_DIRECTORY = "/demonuts"
        const val GALLERY = 1
        const val CAMERA = 2
    }

    fun choosePhotoFromGallary(fragment: Fragment) {
        val galleryIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        fragment.startActivityForResult(galleryIntent, GALLERY)
    }

    fun takePhotoFromCamera(fragment: Fragment) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        fragment.startActivityForResult(intent, CAMERA)
    }

}