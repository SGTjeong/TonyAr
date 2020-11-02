package com.example.tonyar

import com.example.tonyar.ARCore.ModelBuffers
import com.example.tonyar.ARCore.TonyArCore
import com.example.tonyar.Filament.TonyFilament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer

class BufferFactory {
    companion object{
        private const val positionBufferIndex: Int = 0
        const val uvBufferIndex: Int = 1

        fun createVertexBuffer(filament : TonyFilament, mb : ModelBuffers) : VertexBuffer{
            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(mb.clipPosition.count())
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
    }
}