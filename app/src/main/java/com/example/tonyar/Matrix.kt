package com.example.tonyar

import android.content.Context
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.channels.Channels

const val floatSize: Int = java.lang.Float.BYTES

inline class V2A(val floatArray: FloatArray)
inline class M4(val floatArray: FloatArray)

const val dimenV2A: Int = 2
inline val V2A.dimen: Int get() = dimenV2A
fun V2A.count(): Int = floatArray.size / dimen

fun FloatArray.toFloatBuffer(): FloatBuffer = ByteBuffer
    .allocateDirect(size * floatSize)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .also { floatBuffer ->
        floatBuffer.put(this)
        floatBuffer.rewind()
    }

fun ShortArray.toShortBuffer(): ShortBuffer = ShortBuffer
    .allocate(size)
    .also { shortBuffer ->
        shortBuffer.put(this)
        shortBuffer.rewind()
    }

fun m4Identity(): M4 = FloatArray(16)
    .also { Matrix.setIdentityM(it, 0) }
    .let { M4(it) }


fun Context.readUncompressedAsset(@Suppress("SameParameterValue") assetName: String): ByteBuffer {
    assets.openFd(assetName)
        .use { fd ->
            val input = fd.createInputStream()
            val dst = ByteBuffer.allocate(fd.length.toInt())

            val src = Channels.newChannel(input)
            src.read(dst)
            src.close()

            return dst.apply { rewind() }
        }
}