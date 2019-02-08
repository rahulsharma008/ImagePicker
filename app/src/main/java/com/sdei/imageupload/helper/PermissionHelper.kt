package com.sdei.imageupload.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionHelper {

    private val TAG = "PermissionHelperJava"
    private var REQUEST_CODE: Int = 0

    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var mPermissionCallback: PermissionCallback? = null
    private var showRational: Boolean = false

    //=========Constructors - START=========
    constructor(activity: Activity, requestCode: Int = 100) {
        this.activity = activity
        this.REQUEST_CODE = requestCode
    }

    constructor(fragment: Fragment, requestCode: Int = 100) {
        this.fragment = fragment
        this.REQUEST_CODE = requestCode
    }

    private fun checkIfPermissionPresentInAndroidManifest(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!hasPermissionInManifest(permission)) {
                mPermissionCallback!!.permissionNotInManifest(permission)
                return false
            }
        }
        return true
    }

    fun request(permissions: Array<String>, permissionCallback: PermissionCallback?) {
        this.mPermissionCallback = permissionCallback
        checkIfPermissionPresentInAndroidManifest(permissions)

        if (!hasPermission(permissions)) {
            showRational = shouldShowRational(permissions)
            if (activity != null)
                ActivityCompat.requestPermissions(activity!!, filterNotGrantedPermission(permissions), REQUEST_CODE)
            else
                fragment!!.requestPermissions(filterNotGrantedPermission(permissions), REQUEST_CODE)
        } else {
            mPermissionCallback?.onPermissionGranted()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (grantResults.isNotEmpty() && requestCode == REQUEST_CODE) {
            var denied = false
            val grantedPermissions = ArrayList<String>()
            for ((i, grantResult) in grantResults.withIndex()) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true
                } else {
                    grantedPermissions.add(permissions[i])
                }
            }
            if (denied) {
                val currentShowRational = shouldShowRational(permissions)
                if (!showRational && !currentShowRational) {
                    Log.d(TAG, "PERMISSION: Permission Denied By System")
                    mPermissionCallback?.onPermissionDeniedBySystem()
                } else {
                    Log.i(TAG, "PERMISSION: Permission Denied")
                    //Checking if any single individual permission is granted then show user that permission
//                    if (!grantedPermissions.isEmpty()) {
//                        mPermissionCallback?.onIndividualPermissionGranted(grantedPermissions.toTypedArray())
//                    }
//                    mPermissionCallback?.onPermissionDenied()
                }
            } else {
                Log.e(TAG, "PERMISSION: Permission Granted")
                mPermissionCallback?.onPermissionGranted()
            }
        }

    }

    interface PermissionCallback {

        fun onPermissionGranted()

//        fun onIndividualPermissionGranted(grantedPermission: Array<String>)

//        fun onPermissionDenied()

        fun onPermissionDeniedBySystem()

        fun permissionNotInManifest(permissionName: String)

    }

    private fun <T : Context> getContext(): T? {
        return if (activity != null) activity as T? else fragment!!.context as T
    }

    /**
     * Return list that is not granted and we need to ask for permission
     *
     * @param permissions
     * @return
     */
    private fun filterNotGrantedPermission(permissions: Array<String>): Array<String> {
        val notGrantedPermission = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(getContext()!!, permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermission.add(permission)
            }
        }
        return notGrantedPermission.toTypedArray()
    }

    /**
     * Check permission is there or not
     * @return
     */
    private fun hasPermission(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(getContext()!!, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Check permission is there or not for group of permissions
     * @return
     */
    fun checkSelfPermission(permissions: Array<String>?): Boolean {
        for (permission in permissions!!) {
            if (ContextCompat.checkSelfPermission(getContext()!!, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Checking if there is need to show rational for group of permissions
     *
     * @param permissions
     * @return
     */
    private fun shouldShowRational(permissions: Array<String>): Boolean {
        var currentShowRational = false
        for (permission in permissions) {

            if (activity != null) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission)) {
                    currentShowRational = true
                    break
                }
            } else {
                if (fragment!!.shouldShowRequestPermissionRationale(permission) == true) {
                    currentShowRational = true
                    break
                }
            }
        }
        return currentShowRational
    }

    //===================
    private fun hasPermissionInManifest(permission: String): Boolean {
        try {
            val context = if (activity != null) activity else fragment!!.activity
            val info = context?.packageManager?.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            if (info?.requestedPermissions != null) {
                for (p in info.requestedPermissions) {
                    if (p == permission) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Open current application detail activity so user can change permission manually.
     */
    fun openAppDetailsActivity() {
        if (getContext<Context>() == null) {
            return
        }
        val i = Intent()
        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:" + getContext<Context>()!!.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        getContext<Context>()!!.startActivity(i)
    }

}