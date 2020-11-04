package com.example.tonyar.Filament

import android.content.Context
import com.example.tonyar.readUncompressedAsset
import com.google.android.filament.*
import com.google.android.filament.Scene
import com.google.android.filament.utils.KtxLoader


@EntityInstance
fun getTonyDirectionalLightInstance(filamentScene : Scene, engine : Engine) : Int {
    return EntityManager
        .get()
        .create()
        .let{ directionalLight ->
            filamentScene.addEntity(directionalLight)

            LightManager
                .Builder(LightManager.Type.DIRECTIONAL)
                .intensity(100000f)
                .castShadows(true)
                .build(engine, directionalLight)

            engine.lightManager.getInstance(directionalLight)
        }
}

fun getTonyIndirectLight(context : Context, engine : Engine ) : IndirectLight{
    val buffer = context.readUncompressedAsset("output_ibl.ktx")
    val options = KtxLoader.Options()

    return KtxLoader.createIndirectLight(engine, buffer, options).also {
        it.intensity = 5000f
    }
}