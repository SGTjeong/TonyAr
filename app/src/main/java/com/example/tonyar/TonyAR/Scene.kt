package com.example.tonyar.TonyAR

import android.content.Context
import com.example.tonyar.Filament.PlaneRenderer
import com.example.tonyar.Filament.TonyFilament
import com.example.tonyar.Filament.getTonyDirectionalLightInstance
import com.example.tonyar.Filament.getTonyIndirectLight
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.core.Frame

class Scene(
    context : Context,
    filament : TonyFilament
) {
    private val engine = filament.engine
    private val filamentScene = filament.scene.also { filamentScene ->
        filamentScene.indirectLight = getTonyIndirectLight(context, engine)
    }
    private val lightInstance = getTonyDirectionalLightInstance(
        filamentScene,
        engine
    )
    private val planeRenderer = PlaneRenderer(context, filament)
    var children = mutableListOf<Pair<Node, FilamentAsset>>()

    fun addChild(node : Node, asset : FilamentAsset){
        children.add(Pair(node, asset))
        filamentScene.addEntities(asset.entities)
    }

    fun detachChild(node : Node){
        val itr = children.iterator()
        while(itr.hasNext()){
            val child = itr.next().first
            if(child == node) itr.remove()
        }
    }

    fun doFrame(frame: Frame) {
        for(pair in children){
            pair.first.doFrame(frame)
        }
        planeRenderer.doFrame(frame)
    }

}