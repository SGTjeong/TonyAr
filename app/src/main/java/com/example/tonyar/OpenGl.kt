package com.example.tonyar

import android.opengl.GLES11Ext
import android.opengl.GLES30

fun createExternalTextureId(): Int = IntArray(1)
    .apply { GLES30.glGenTextures(1, this, 0) }
    .first()
    .apply { GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, this) }