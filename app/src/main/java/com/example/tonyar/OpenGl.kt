package com.example.tonyar

import android.opengl.GLES11Ext
import android.opengl.GLES30

fun createExternalTextureId(): Int = IntArray(1)
    .apply { GLES30.glGenTextures(1, this, 0) }
    .first()
    .apply {
        val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES30.glBindTexture(textureTarget, this)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
    }