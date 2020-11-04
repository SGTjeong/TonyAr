package com.example.tonyar.TonyAR

import android.content.Context
import android.util.Log
import com.example.tonyar.ARCore.ArContext
import com.example.tonyar.m4Identity
import com.example.tonyar.translate
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.core.Frame
import java.util.concurrent.TimeUnit


class Node(
    arContext: ArContext
)
{
    private var isRenderableAttached = false
    private var isAttachedToScene = false
    private lateinit var asset : FilamentAsset
    private val modelLoader = ModelLoader(arContext.arCore.filament.assetLoader, arContext.arCore.filament.resourceLoader)
    private val transformManager = arContext.arCore.filament.engine.transformManager

    private var worldPosition : Vector3 = Vector3(0f, 0f, 0f)
    private var worldScale : Vector3 = Vector3(0f, 0f, 0f)

    fun setRenderable(context : Context, path : String){
        try{
            asset = modelLoader.createFilamentAsset(context, path)
            isRenderableAttached = true
        } catch (e : Exception){
            Log.e("WONSIK", "Node.setRenderable", e)
        }
    }

    fun setParent(scene : Scene){
        if(!isRenderableAttached) return
        scene.addChild(this, asset)
    }

    fun setWorldPosition(position : Vector3){
        if(!isRenderableAttached) return

        transformManager.apply{
            setTransform(
                getInstance(asset.root),
                m4Identity()
                    .translate(position.x, position.y, position.z)
                    .floatArray
            )
        }
    }

    fun doFrame(frame : Frame){
        if(!isRenderableAttached) return

        asset.animator?.apply {
            if (animationCount > 0) {
                applyAnimation(
                    0, (frame.timestamp / TimeUnit.SECONDS.toNanos(1).toDouble()).toFloat() % getAnimationDuration(0)
                )
                updateBoneMatrices()
            }
        }
    }
}