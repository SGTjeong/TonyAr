package com.example.tonyar

import android.content.Context
import android.opengl.Matrix
import com.example.tonyar.ARCore.TonyArCore
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.channels.Channels

const val floatSize: Int = java.lang.Float.BYTES

inline class V2A(val floatArray: FloatArray)
inline class M4(val floatArray: FloatArray)
inline class V3(val floatArray: FloatArray)
inline class V4A(val floatArray: FloatArray)
inline class TriangleIndexArray(val shortArray: ShortArray)
inline val V2A.indices: IntRange get() = IntRange(0, count() - 1)

inline var V3.x: Float
    get() = floatArray[0]
    set(x) {
        floatArray[0] = x
    }

inline var V3.y: Float
    get() = floatArray[1]
    set(y) {
        floatArray[1] = y
    }

inline var V3.z: Float
    get() = floatArray[2]
    set(z) {
        floatArray[2] = z
    }

fun V4A.getX(i: Int): Float = floatArray[(i * dimen) + 0]
fun V4A.getY(i: Int): Float = floatArray[(i * dimen) + 1]
fun V4A.getZ(i: Int): Float = floatArray[(i * dimen) + 2]
fun V4A.getW(i: Int): Float = floatArray[(i * dimen) + 3]

const val dimenV2A: Int = 2
inline val V2A.dimen: Int get() = dimenV2A
const val dimenV4A: Int = 4
inline val V4A.dimen: Int get() = dimenV4A

fun V4A.count(): Int = floatArray.size / dimen
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

fun V2A.set(i: Int, x: Float, y: Float) {
    floatArray[(i * dimen) + 0] = x
    floatArray[(i * dimen) + 1] = y
}

fun m4Identity(): M4 = FloatArray(16)
    .also { Matrix.setIdentityM(it, 0) }
    .let { M4(it) }


fun Pose.matrix(): M4 = FloatArray(16)
    .also { toMatrix(it, 0) }
    .let { M4(it) }

fun M4.scale(x: Float, y: Float, z: Float): M4 = FloatArray(16)
    .also { Matrix.scaleM(it, 0, floatArray, 0, x, y, z) }
    .let { M4(it) }

fun M4.rotate(angle: Float, x: Float, y: Float, z: Float): M4 = FloatArray(16)
    .also { Matrix.rotateM(it, 0, floatArray, 0, angle, x, y, z) }
    .let { M4(it) }

fun M4.translate(x: Float, y: Float, z: Float): M4 = FloatArray(16)
    .also { Matrix.translateM(it, 0, floatArray, 0, x, y, z) }
    .let { M4(it) }

fun FloatArray.toDoubleArray(): DoubleArray = DoubleArray(size)
    .also { doubleArray ->
        for (i in indices) {
            doubleArray[i] = this[i].toDouble()
        }
    }

fun Frame.projectionMatrix(): M4 = FloatArray(16)
    .apply { camera.getProjectionMatrix(this, 0, TonyArCore.near, TonyArCore.far) }
    .let { M4(it) }

fun FloatBuffer.polygonToVertices(m: M4): V4A {
    val f = FloatArray((capacity() / 2) * 4)
    val v = FloatArray(4)
    v[1] = 0f
    v[3] = 1f
    rewind()

    for (i in f.indices step 4) {
        v[0] = get()
        v[2] = get()
        Matrix.multiplyMV(f, i, m.floatArray, 0, v, 0)
    }

    return V4A(f)
}

inline fun triangleIndexArrayCreate(
    count: Int,
    i1: (Int) -> Short,
    i2: (Int) -> Short,
    i3: (Int) -> Short
): TriangleIndexArray {
    val triangleIndexArray = TriangleIndexArray(ShortArray(count * 3))

    for (i in 0 until count) {
        val k = i * 3
        triangleIndexArray.shortArray[k + 0] = i1(i)
        triangleIndexArray.shortArray[k + 1] = i2(i)
        triangleIndexArray.shortArray[k + 2] = i3(i)
    }

    return triangleIndexArray
}

fun FloatBuffer.polygonToUV(): V2A {
    val f = V2A(FloatArray(capacity()))
    rewind()

    for (i in f.indices) {
        f.set(i, get() * 10f, get() * 5f)
    }

    return f
}

inline fun v2aCreate(count: Int, x: (Int) -> Float, y: (Int) -> Float): V2A =
    V2A(FloatArray(count * dimenV2A))
        .also {
            for (i in it.indices) {
                it.set(i, x(i), y(i))
            }
        }

// uses world space to determine UV coordinates for better stability
fun V4A.horizontalToUV(): V2A = v2aCreate(count(), { i -> getX(i) * 10f }, { i -> getZ(i) * 5f })

fun Context.readUncompressedAsset(@Suppress("SameParameterValue") assetName: String): ByteBuffer {
    try {
        val `is` = assets.open(assetName)
        val size = `is`.available()
        val buffer = ByteArray(size)
        `is`.read(buffer)
        `is`.close()

        return ByteBuffer.wrap(buffer)
    } catch (ex: Exception) {
        ex.printStackTrace()
        throw ex
    }
}
