package com.example.tonyar.Filament

import com.example.tonyar.ARCore.ModelBuffers
import com.google.android.filament.IndexBuffer
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.VertexAttribute
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class BufferFactory {
    companion object{
        private const val positionBufferIndex: Int = 0
        const val uvBufferIndex: Int = 1
        private var cameraUvCoords: FloatBuffer? = null
        private var transformedCameraUvCoords: FloatBuffer? = null

        fun createVertexBuffer(filament : TonyFilament, mb : ModelBuffers) : VertexBuffer{

            val vertexBufferData = FloatBuffer.allocate(CAMERA_VERTICES.size)
            vertexBufferData.put(CAMERA_VERTICES)

            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(VERTEX_COUNT)
                .bufferCount(2)
                .attribute(
                    VertexAttribute.POSITION,
                    0,
                    VertexBuffer.AttributeType.FLOAT3,
                    0,
                    CAMERA_VERTICES.size / VERTEX_COUNT * FLOAT_SIZE_IN_BYTES
                )
                .attribute(
                    VertexAttribute.UV0,
                    1,
                    VertexBuffer.AttributeType.FLOAT2,
                    0,
                    CAMERA_UVS.size / VERTEX_COUNT * FLOAT_SIZE_IN_BYTES
                )
                .build(filament.engine)

            vertexBufferData.rewind()

            vertexBuffer.setBufferAt(
                filament.engine, POSITION_BUFFER_INDEX, vertexBufferData
            )

            cameraUvCoords = createCameraUVBuffer()
            transformedCameraUvCoords = createCameraUVBuffer()

            adjustCameraUvsForOpenGL()
            vertexBuffer.setBufferAt(
                filament.engine, UV_BUFFER_INDEX, transformedCameraUvCoords!!
            )
            /*
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

             */

            return vertexBuffer

        }

        private fun adjustCameraUvsForOpenGL() {
            transformedCameraUvCoords?:return
            var i = 1
            while (i < VERTEX_COUNT * 2) {
                transformedCameraUvCoords!!.put(i, 1.0f - transformedCameraUvCoords!![i])
                i += 2
            }
        }

        fun createIndexBuffer(filament: TonyFilament, mb : ModelBuffers) : IndexBuffer{

            val indexBufferData = ShortBuffer.allocate(CAMERA_INDICES.size).also {
                it.put(CAMERA_INDICES)
            }

            val indexCount = indexBufferData.capacity()
            val indexBuffer = IndexBuffer.Builder()
                .indexCount(indexCount)
                .bufferType(IndexType.USHORT)
                .build(filament.engine)

            indexBufferData.rewind()

            indexBuffer.setBuffer(filament.engine, indexBufferData);

            return indexBuffer


            /*
            return IndexBuffer
                .Builder()
                .indexCount(mb.triangleIndices.size)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(filament.engine)
                .apply { setBuffer(filament.engine, mb.triangleIndices.toShortBuffer())}

             */

        }


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

        private val CAMERA_VERTICES = floatArrayOf(-1.0f, 1.0f, 1.0f, -1.0f, -3.0f, 1.0f, 3.0f, 1.0f, 1.0f)
        private val CAMERA_INDICES = shortArrayOf(0, 1, 2)
        private const val VERTEX_COUNT = 3
        private const val POSITION_BUFFER_INDEX = 0
        private const val UV_BUFFER_INDEX = 1

    }
}