package com.example.tonyar.ARCore

import android.app.Activity
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.example.tonyar.*
import com.example.tonyar.Filament.TonyFilament
import com.google.android.filament.*
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import kotlin.math.roundToInt

class ModelBuffers(val clipPosition: V2A, val uvs: V2A, val triangleIndices: ShortArray)

class TonyArCore(
    private val activity : Activity,
    val filament : TonyFilament,
    private val view : View){

    private val cameraStreamTextureId : Int = createExternalTextureId()
    private lateinit var stream : Stream
    private lateinit var depthMaterialInstance: MaterialInstance
    private lateinit var flatMaterialInstance: MaterialInstance

    @Entity
    var depthRenderable : Int = 0
    @Entity
    var flatRenderable : Int = 0

    val session : Session = Session(activity)
        .also {session ->
            session.config
                .apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    focusMode = Config.FocusMode.AUTO
                    depthMode =
                        if(session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                .let{
                    session.configure(it)
                }
            session.setCameraTextureName(cameraStreamTextureId)
        }

    private val cameraId = session.cameraConfig.cameraId
    private val cameraManager : CameraManager =
        ContextCompat.getSystemService(activity, CameraManager::class.java)!!

    var timeStamp : Long = 0L

    lateinit var frame : Frame

    private lateinit var cameraDevice : CameraDevice
    private lateinit var depthTexture : Texture

    fun destroy(){
        session.close()
        cameraDevice.close()
    }

    var displayRotationDegrees : Int = 0

    fun configurationChange(){
        if(this::frame.isInitialized.not()) return

        val intrinsics = frame.camera.textureIntrinsics
        val dimensions = intrinsics.imageDimensions

        val displayWidth : Int
        val displayHeight : Int
        val displayRotation : Int

        DisplayMetrics()
            .also { displayMetrics ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display!!.also { display ->
                        display.getRealMetrics(displayMetrics)
                        displayRotation = display.rotation
                    }
                }  else{
                    activity.windowManager.defaultDisplay!!.also { display ->
                        display.getRealMetrics(displayMetrics)
                        displayRotation = display.rotation
                    }
                }

                displayWidth = displayMetrics.widthPixels
                displayHeight = displayMetrics.heightPixels
            }

        displayRotationDegrees =
            when(displayRotation){
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> throw Exception("Invalid Display Rotation")
            }

        val cameraWidth : Int
        val cameraHeight : Int

        when(cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!){
            0, 180 -> when(displayRotation){
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }
                else -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }
            }
            else -> when(displayRotation){
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }
                else -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }
            }
        }

        val cameraRatio = cameraWidth.toFloat() / cameraHeight.toFloat()
        val displayRatio = displayWidth.toFloat() / displayHeight.toFloat()

        val viewWidth : Int
        val viewHeight : Int

        if(displayRatio < cameraRatio){
            viewWidth = displayWidth
            viewHeight = (displayWidth.toFloat() / cameraRatio).roundToInt()
        } else {
            viewWidth = (displayHeight.toFloat() * cameraRatio).roundToInt()
            viewHeight = displayHeight
        }

        view.updateLayoutParams<FrameLayout.LayoutParams> {
            width = viewWidth
            height = viewHeight
        }

        session.setDisplayGeometry(displayRotation, viewWidth, viewHeight)
    }

    var hasDepthImage: Boolean = false

    fun update(frame : Frame, filament: TonyFilament){
        val firstFrame = this::frame.isInitialized.not()
        this.frame = frame

        if(firstFrame){
            configurationChange()
            initFlatMaterialInstance()
            initFlat()
        }
    }

    private fun initFlatMaterialInstance() {
        val stream = MaterialFactory.createStream(filament, frame, cameraStreamTextureId)
        flatMaterialInstance = MaterialFactory.createFlatMaterialInstance(activity, filament, stream)
    }

    private fun initFlat() {
        val mb = ModelBuffers(V2A(floatArrayOf(1f, 2f)), V2A(floatArrayOf(1f,2f)), shortArrayOf(1,2))

        RenderableManager
            .Builder(1)
            .castShadows(false)
            .receiveShadows(false)
            .culling(false)
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                BufferFactory.createVertexBuffer(filament, mb),
                BufferFactory.createIndexBuffer(filament, mb)
            )
            .material(0, flatMaterialInstance)
            .build(filament.engine, EntityManager.get().create().also { flatRenderable = it})
    }


    companion object {
        const val near = 0.1f
        const val far = 30f
    }

}