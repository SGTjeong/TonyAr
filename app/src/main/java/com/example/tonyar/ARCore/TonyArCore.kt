package com.example.tonyar.ARCore

import android.app.Activity
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.example.tonyar.*
import com.example.tonyar.Filament.BufferFactory
import com.example.tonyar.Filament.MaterialFactory
import com.example.tonyar.Filament.TonyFilament
import com.example.tonyar.TonyAR.CameraStream
import com.google.android.filament.*
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import kotlin.math.roundToInt

class ModelBuffers(val clipPosition: V2A, val uvs: V2A, val triangleIndices: ShortArray)

class TonyArCore(
    private val activity : Activity,
    val filament : TonyFilament,
    private val view : View) {

    private val cameraStreamTextureId: Int = createExternalTextureId()
    private lateinit var cameraStream: CameraStream
    private lateinit var depthMaterialInstance: MaterialInstance
    private lateinit var flatMaterialInstance: MaterialInstance

    @Entity
    var depthRenderable: Int = 0

    @Entity
    var flatRenderable: Int = 0

    val session: Session = Session(activity)
        .also { session ->
            session.config
                .apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    focusMode = Config.FocusMode.AUTO
                    depthMode =
                        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                .let {
                    session.configure(it)
                }
            session.setCameraTextureName(cameraStreamTextureId)
        }

    private val cameraId = session.cameraConfig.cameraId
    private val cameraManager: CameraManager =
        ContextCompat.getSystemService(activity, CameraManager::class.java)!!

    var timeStamp: Long = 0L

    lateinit var frame: Frame

    private lateinit var cameraDevice: CameraDevice
    private lateinit var depthTexture: Texture

    fun destroy() {
        session.close()
        cameraDevice.close()
    }

    var displayRotationDegrees: Int = 0

    fun configurationChange() {
        if (this::frame.isInitialized.not()) return

        Log.e("WONSIK", "configurationChange")

        val intrinsics = frame.camera.textureIntrinsics
        val dimensions = intrinsics.imageDimensions

        val displayWidth: Int
        val displayHeight: Int
        val displayRotation: Int

        DisplayMetrics()
            .also { displayMetrics ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display!!.also { display ->
                        display.getRealMetrics(displayMetrics)
                        displayRotation = display.rotation
                    }
                } else {
                    activity.windowManager.defaultDisplay!!.also { display ->
                        display.getRealMetrics(displayMetrics)
                        displayRotation = display.rotation
                    }
                }

                displayWidth = displayMetrics.widthPixels
                displayHeight = displayMetrics.heightPixels
            }

        displayRotationDegrees =
            when (displayRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> throw Exception("Invalid Display Rotation")
            }

        val cameraWidth: Int
        val cameraHeight: Int

        when (cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!) {
            0, 180 -> when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }
                else -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }
            }
            else -> when (displayRotation) {
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

        val viewWidth: Int
        val viewHeight: Int

        if (displayRatio < cameraRatio) {
            viewWidth = displayWidth
            viewHeight = (displayWidth.toFloat() / cameraRatio).roundToInt()
        } else {
            viewWidth = (displayHeight.toFloat() * cameraRatio).roundToInt()
            viewHeight = displayHeight
        }

        view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = displayWidth
            height = displayHeight
        }

        view.post {
            Log.e("WONSIK", "${filament.uiHelper.desiredWidth},  ${filament.uiHelper.desiredHeight}, ${view.width}, ${view.height}, ${displayWidth}, ${displayHeight}")
            Log.e("WONSIK", "$displayRotation")
            session.setDisplayGeometry(displayRotation, view.width, view.height)
            setDesiredSize(view.width, view.height)
        }
    }

    var hasDepthImage: Boolean = false

    fun update(frame: Frame, filament: TonyFilament) {
        val firstFrame = this::frame.isInitialized.not()
        this.frame = frame

        if (firstFrame) {
            configurationChange()
            initFlatMaterialInstance()
            initCameraStream()
        }

        if(frame.hasDisplayGeometryChanged()){
            //Log.e("WONSIK", "shouldRecalculate")
            cameraStream.recalculateCameraUvs(frame)
        }


        flatMaterialInstance.setParameter(
            "uvTransform",
            MaterialInstance.FloatElement.FLOAT4,
            uvTransform().floatArray,
            0,
            4
        )

        filament.scene.removeEntity(depthRenderable)
        filament.scene.addEntity(flatRenderable)

        filament.camera.setCustomProjection(
            frame.projectionMatrix().floatArray.toDoubleArray(),
            near.toDouble(),
            far.toDouble()
        )


        val cameraTransform = frame.camera.displayOrientedPose.matrix()
        filament.camera.setModelMatrix(cameraTransform.floatArray)

        //val instance = filament.engine.transformManager.create(depthRenderable)
        //filament.engine.transformManager.setTransform(instance, cameraTransform.floatArray)
    }

    private fun initFlatMaterialInstance() {
        val stream = MaterialFactory.createStream(filament, frame, cameraStreamTextureId)
        flatMaterialInstance = MaterialFactory.createFlatMaterialInstance(activity, filament, stream)
    }

    private fun initCameraStream() {
        cameraStream = CameraStream(cameraStreamTextureId, filament, flatMaterialInstance)
    }

    private fun tessellation(tesWidth: Int, tesHeight: Int): ModelBuffers {
        val clipPosition: V2A = (((tesWidth * tesHeight) + tesWidth + tesHeight + 1) * dimenV2A)
            .let { FloatArray(it) }
            .let { V2A(it) }

        val uvs: V2A = (((tesWidth * tesHeight) + tesWidth + tesHeight + 1) * dimenV2A)
            .let { FloatArray(it) }
            .let { V2A(it) }

        for (k in 0..tesHeight) {
            val v = k.toFloat() / tesHeight.toFloat()
            val y = (k.toFloat() / tesHeight.toFloat()) * 2f - 1f

            for (i in 0..tesWidth) {
                val u = i.toFloat() / tesWidth.toFloat()
                val x = (i.toFloat() / tesWidth.toFloat()) * 2f - 1f
                clipPosition.set(k * (tesWidth + 1) + i, x, y)
                uvs.set(k * (tesWidth + 1) + i, u, v)
            }
        }

        val triangleIndices = ShortArray(tesWidth * tesHeight * 6)

        for (k in 0 until tesHeight) {
            for (i in 0 until tesWidth) {
                triangleIndices[((k * tesWidth + i) * 6) + 0] =
                    ((k * (tesWidth + 1)) + i + 0).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 1] =
                    ((k * (tesWidth + 1)) + i + 1).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 2] =
                    ((k + 1) * (tesWidth + 1) + i).toShort()

                triangleIndices[((k * tesWidth + i) * 6) + 3] =
                    ((k + 1) * (tesWidth + 1) + i).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 4] =
                    ((k * (tesWidth + 1)) + i + 1).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 5] =
                    ((k + 1) * (tesWidth + 1) + i + 1).toShort()
            }
        }

        return ModelBuffers(clipPosition, uvs, triangleIndices)
    }


    private fun uvTransform(): M4 = m4Identity()
        .translate(.5f, .5f, 0f)
        .rotate(imageRotation().toFloat(), 0f, 0f, -1f)
        .translate(-.5f, -.5f, 0f)

    private fun imageRotation(): Int = (cameraManager
        .getCameraCharacteristics(cameraId)
        .get(CameraCharacteristics.SENSOR_ORIENTATION)!! +
            when (displayRotationDegrees) {
                0 -> 90
                90 -> 0
                180 -> 270
                270 -> 180
                else -> throw Exception()
            } + 270) % 360

    private fun setDesiredSize(width : Int, height : Int){
        var minor: Int = Math.min(width, height)
        var major: Int = Math.max(width, height)
        if (minor > 1080) {
            major = major * 1080 / minor
            minor = 1080
        }
        if (width < height) {
            val t = minor
            minor = major
            major = t
        }
        filament.uiHelper.setDesiredSize(major, minor)
    }

    companion object {
        const val near = 0.1f
        const val far = 30f
    }


}