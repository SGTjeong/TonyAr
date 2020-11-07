package com.example.tonyar.TonyAR

import com.example.tonyar.Filament.TonyFilament
import com.google.android.filament.*
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.Scene
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class CameraStream(cameraTextureId : Int, private val filament : TonyFilament, private val flatMaterialInstance: MaterialInstance) {
    private val TAG = CameraStream::class.java.simpleName
    private val CAMERA_INDICES = shortArrayOf(0, 1, 2)
    private val VERTEX_COUNT = 3
    private val POSITION_BUFFER_INDEX = 0
    private val UV_BUFFER_INDEX = 1
    private val FLOAT_SIZE_IN_BYTES = java.lang.Float.SIZE / 8

    private val CAMERA_VERTICES =
        floatArrayOf(-1.0f, 1.0f, 1.0f, -1.0f, -3.0f, 1.0f, 3.0f, 1.0f, 1.0f)
    private val CAMERA_UVS = floatArrayOf(0.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f)

    private val UNINITIALIZED_FILAMENT_RENDERABLE = -1
    private var cameraTextureId = 0

    private var cameraStreamRenderable = UNINITIALIZED_FILAMENT_RENDERABLE

    private var cameraIndexBuffer: IndexBuffer? = null
    private var cameraVertexBuffer: VertexBuffer? = null
    private var cameraUvCoords: FloatBuffer? = null
    private var transformedCameraUvCoords: FloatBuffer? = null

    init{
        this.cameraTextureId = cameraTextureId


        // create screen quad geometry to camera stream to
        val indexBufferData = ShortBuffer.allocate(CAMERA_INDICES.size)
        indexBufferData.put(CAMERA_INDICES)
        val indexCount = indexBufferData.capacity()
        cameraIndexBuffer = IndexBuffer.Builder()
            .indexCount(indexCount)
            .bufferType(IndexType.USHORT)
            .build(filament.engine)
        indexBufferData.rewind()
        cameraIndexBuffer!!.setBuffer(filament.engine, indexBufferData)


        // Note: ARCore expects the UV buffers to be direct or will assert in transformDisplayUvCoords.
        cameraUvCoords = createCameraUVBuffer()
        transformedCameraUvCoords = createCameraUVBuffer()


        val vertexBufferData = FloatBuffer.allocate(CAMERA_VERTICES.size)
        vertexBufferData.put(CAMERA_VERTICES)

        cameraVertexBuffer = VertexBuffer.Builder()
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
        cameraVertexBuffer!!.setBufferAt(
                filament.engine,
                POSITION_BUFFER_INDEX,
                vertexBufferData
        )

        adjustCameraUvsForOpenGL()
        cameraVertexBuffer!!.setBufferAt(
            filament.engine, UV_BUFFER_INDEX, transformedCameraUvCoords!!
        )

        initializeCameraRenderable()
    }

    private fun adjustCameraUvsForOpenGL() {
        transformedCameraUvCoords ?:return
        var i = 1
        while (i < VERTEX_COUNT * 2) {
            transformedCameraUvCoords!!.put(i, 1.0f - transformedCameraUvCoords!![i])
            i += 2
        }
    }


    fun createCameraUVBuffer() : FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(CAMERA_UVS.size *FLOAT_SIZE_IN_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        buffer.put(CAMERA_UVS)
        buffer.rewind()
        return buffer
    }

    fun recalculateCameraUvs(frame: Frame) {
        val cameraUvCoords = cameraUvCoords
        val transformedCameraUvCoords = transformedCameraUvCoords
        val cameraVertexBuffer = cameraVertexBuffer
        frame.transformDisplayUvCoords(cameraUvCoords, transformedCameraUvCoords)
        adjustCameraUvsForOpenGL()
        cameraVertexBuffer!!.setBufferAt(filament.engine, UV_BUFFER_INDEX, transformedCameraUvCoords!!)
    }

    fun initializeCameraRenderable(){
        cameraStreamRenderable = EntityManager.get().create()
        val builder = RenderableManager.Builder(1)
        builder
            .castShadows(false)
            .receiveShadows(false)
            .culling(false) // Always draw the camera feed last to avoid overdraw
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                cameraVertexBuffer!!,
                cameraIndexBuffer!!
            )
            .material(0, flatMaterialInstance)
            .build(filament.engine, cameraStreamRenderable)

        filament.scene!!.addEntity(cameraStreamRenderable)
    }
}