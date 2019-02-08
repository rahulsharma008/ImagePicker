package com.sdei.imageupload.data

class DataPart(){

    var fileName: String? = null
    var content: ByteArray? = null
    var type: String? = null

    fun initParams(fileName: String, content: ByteArray) {
        this.fileName = fileName
        this.content = content
    }

}