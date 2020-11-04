package com.example.tonyar.Filament

import android.content.Context
import android.view.MotionEvent
import com.example.tonyar.*
import com.example.tonyar.ARCore.TonyArCore
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class ModelRenderer(context : Context, private val arCore : TonyArCore, private val filament : TonyFilament) {
    lateinit var asset : FilamentAsset

    init{
        context.assets.open("arachi_walk.glb").let { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            asset = filament.assetLoader.createAssetFromBinary(ByteBuffer.wrap(bytes))?:return@let
            filament.resourceLoader.loadResources(asset)
        }

        filament.run {
            scene.addEntities(asset.entities)

            engine.transformManager.setTransform(
                engine.transformManager.getInstance(asset.root),
                m4Identity()
                    .translate(1f, 1f,  1f)
                    .rotate(0f, 0f, 1f, 0f)
                    .scale(1f, 1f, 1f)
                    .floatArray
            )
        }
    }

    fun doFrame(frame : Frame){
        asset?.let{
            val animator = asset.animator

            if (animator?.animationCount > 0) {
                animator.applyAnimation(
                    0,
                    (frame.timestamp /
                            TimeUnit.SECONDS.toNanos(1).toDouble())
                        .toFloat() %
                            animator.getAnimationDuration(0)
                )

                animator.updateBoneMatrices()
            }

        }
    }

    fun onTouch(event: MotionEvent) {
        arCore.frame.hitTest(event)?.let {
            if(it.isNotEmpty()){
                it[0]?.let {
                    V3(it.hitPose.translation)?.let{
                        filament.scene.addEntities(asset.entities)

                        filament.engine.transformManager.setTransform(
                            filament.engine.transformManager.getInstance(asset.root),
                            m4Identity()
                                .translate(it.x, it.y,  it.z)
                                .floatArray
                        )
                    }
                }
            }
        }
    }
}