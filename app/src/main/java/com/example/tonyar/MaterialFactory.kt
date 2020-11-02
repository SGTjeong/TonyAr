package com.example.tonyar

import android.content.Context
import com.example.tonyar.Filament.TonyFilament
import com.google.android.filament.*
import com.google.ar.core.Frame

class MaterialFactory {
    companion object{
        fun createStream(filament : TonyFilament, frame : Frame, cameraStreamTextureId : Int) : Stream{
            val camera = frame.camera
            val intrinsics = camera.textureIntrinsics
            val dimensions = intrinsics.imageDimensions
            val width = dimensions[0]
            val height = dimensions[1]

            return Stream
                .Builder()
                .stream(cameraStreamTextureId)
                .width(width)
                .height(height)
                .build(filament.engine)
        }

        fun createFlatMaterialInstance(context : Context, filament : TonyFilament, stream : Stream) : MaterialInstance{
            val materialInstance = context.readUncompressedAsset("tony")
                .let{ byteBuffer ->
                    Material
                        .Builder()
                        .payload(byteBuffer, byteBuffer.remaining())
                }
                .build(filament.engine)
                .createInstance()

            materialInstance.setParameter(
                "cameraTexture",
                Texture
                    .Builder()
                    .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                    .format(Texture.InternalFormat.RGB8)
                    .build(filament.engine)
                    .apply { setExternalStream(filament.engine, stream)},
                TextureSampler(
                    TextureSampler.MinFilter.LINEAR,
                    TextureSampler.MagFilter.LINEAR,
                    TextureSampler.WrapMode.CLAMP_TO_EDGE
                )
            )

            materialInstance.setParameter(
                "uvTransform",
                MaterialInstance.FloatElement.FLOAT4,
                m4Identity().floatArray,
                0,
                4
            )

            return materialInstance
        }
    }
}