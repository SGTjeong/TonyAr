package com.example.tonyar.Filament

import com.example.tonyar.ARCore.ModelBuffers
import com.example.tonyar.count
import com.example.tonyar.toFloatBuffer
import com.example.tonyar.toShortBuffer
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BufferFactory {
    companion object{
        private const val positionBufferIndex: Int = 0
        const val uvBufferIndex: Int = 1

        fun createVertexBuffer(filament : TonyFilament, mb : ModelBuffers) : VertexBuffer{
            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(mb.clipPosition.count())
                .bufferCount(2)
                .attribute(VertexBuffer.VertexAttribute.POSITION, positionBufferIndex, VertexBuffer.AttributeType.FLOAT2, 0, 0)
                .attribute(VertexBuffer.VertexAttribute.UV0, uvBufferIndex, VertexBuffer.AttributeType.FLOAT2, 0, 0)
                .build(filament.engine)

            vertexBuffer.setBufferAt(
                filament.engine,
                positionBufferIndex,
                mb.clipPosition.floatArray.toFloatBuffer()
            )

            vertexBuffer.setBufferAt(
                filament.engine,
                uvBufferIndex,
                mb.uvs.floatArray.toFloatBuffer()
            )

            return vertexBuffer
        }

        fun createIndexBuffer(filament: TonyFilament, mb : ModelBuffers) : IndexBuffer{
            return IndexBuffer
                .Builder()
                .indexCount(mb.triangleIndices.size)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(filament.engine)
                .apply { setBuffer(filament.engine, mb.triangleIndices.toShortBuffer())}
        }

/*
        fun createCameraUVBuffer() : FloatBuffer {
            val buffer =
                ByteBuffer.allocateDirect(CAMERA_UVS.size * FLOAT_SIZE_IN_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(CAMERA_UVS)
            buffer.rewind()
            return buffer
        }

        private val CAMERA_UVS = floatArrayOf(0.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f)
        private const val FLOAT_SIZE_IN_BYTES = java.lang.Float.SIZE / 8

 */
    }
}