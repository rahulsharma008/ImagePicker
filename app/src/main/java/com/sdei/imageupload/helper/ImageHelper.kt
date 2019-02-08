package com.sdei.imageupload.helper

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.sdei.imageupload.BaseActivity
import com.sdei.imageupload.fragment.HomeFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageHelper {

    companion object {
        const val GALLERY = 1
        const val CAMERA = 2
    }

    fun choosePhotoFromGallary(fragment: Fragment) {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        fragment.startActivityForResult(galleryIntent, GALLERY)
    }

//    fun takePhotoFromCamera(fragment: Fragment) {
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        fragment.startActivityForResult(intent, CAMERA)
//    }

    fun takePhotoFromCamera(fragment: Fragment): String{

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(fragment.activity?.packageManager!!) != null) {

            var photoFile: File? = null
            try {
                photoFile = createImageFile(fragment)
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {

                val uri = if (Build.VERSION.SDK_INT > 23) {
                    FileProvider.getUriForFile(fragment.context as BaseActivity,
                        "com.sdei.imageupload.fileprovider",
                        photoFile)
                } else {
                    Uri.fromFile(photoFile)
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                fragment.startActivityForResult(takePictureIntent, CAMERA)
                return photoFile.absolutePath
            }
        }

        return ""

    }

    /**
     * returns the File path to save the camera image
     */
    @Throws(IOException::class)
    private fun createImageFile(fragment: Fragment): File {
        // Create an image file name

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = fragment.activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        if (!image.exists()) {
            image.parentFile.mkdirs()
            image.createNewFile()
        }
        return image
    }

    fun getCompressedBitmap(imagePath: String): Bitmap? {
        try {
            val maxHeight = 2560.0f
            val maxWidth = 1440.0f
            var scaledBitmap: Bitmap? = null
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            var bmp = BitmapFactory.decodeFile(imagePath, options)

            var actualHeight = options.outHeight
            var actualWidth = options.outWidth
            var imgRatio = actualWidth.toFloat() / actualHeight.toFloat()
            val maxRatio = maxWidth / maxHeight

            if (actualHeight > maxHeight || actualWidth > maxWidth) {
                when {
                    imgRatio < maxRatio -> {
                        imgRatio = maxHeight / actualHeight
                        actualWidth = (imgRatio * actualWidth).toInt()
                        actualHeight = maxHeight.toInt()
                    }
                    imgRatio > maxRatio -> {
                        imgRatio = maxWidth / actualWidth
                        actualHeight = (imgRatio * actualHeight).toInt()
                        actualWidth = maxWidth.toInt()
                    }
                    else -> {
                        actualHeight = maxHeight.toInt()
                        actualWidth = maxWidth.toInt()

                    }
                }
            }

            options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
            options.inJustDecodeBounds = false
            options.inDither = false
            options.inPurgeable = true
            options.inInputShareable = true
            options.inTempStorage = ByteArray(16 * 1024)

            try {
                bmp = BitmapFactory.decodeFile(imagePath, options)
            } catch (exception: OutOfMemoryError) {
                exception.printStackTrace()
            }

            try {
                scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
            } catch (exception: OutOfMemoryError) {
                exception.printStackTrace()
            }

            val ratioX = actualWidth / options.outWidth.toFloat()
            val ratioY = actualHeight / options.outHeight.toFloat()
            val middleX = actualWidth / 2.0f
            val middleY = actualHeight / 2.0f

            val scaleMatrix = Matrix()
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

            val canvas = Canvas(scaledBitmap!!)
            canvas.matrix = scaleMatrix
            canvas.drawBitmap(bmp, middleX - bmp.width / 2, middleY - bmp.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))

            val exif: ExifInterface?
            try {
                exif = ExifInterface(imagePath)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
                val matrix = Matrix()
                when (orientation) {
                    6 -> matrix.postRotate(90f)
                    3 -> matrix.postRotate(180f)
                    8 -> matrix.postRotate(270f)
                }
                scaledBitmap =
                    Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val out = ByteArrayOutputStream()
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, out)

            val byteArray = out.toByteArray()

            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }

    fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

}