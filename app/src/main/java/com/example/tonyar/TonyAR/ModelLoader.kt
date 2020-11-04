package com.example.tonyar.TonyAR

import android.content.Context
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import java.nio.ByteBuffer

class ModelLoader(
    private val assetLoader: AssetLoader,
    private val resourceLoader: ResourceLoader
) {
    fun createFilamentAsset(context : Context, path : String) : FilamentAsset{
        context.assets.open(path).let { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            val asset = assetLoader.createAssetFromBinary(ByteBuffer.wrap(bytes))?:throw Exception()
            resourceLoader.loadResources(asset)
            return asset
        }
    }

}