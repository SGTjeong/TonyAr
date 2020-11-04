package com.example.tonyar

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import com.example.tonyar.ARCore.ArContext
import com.example.tonyar.ARCore.TonyArCore
import com.example.tonyar.ARCore.TonyFrameCallback
import com.example.tonyar.Filament.PlaneRenderer
import com.example.tonyar.Filament.TonyFilament
import com.example.tonyar.TonyAR.Node
import com.example.tonyar.TonyAR.Scene
import com.example.tonyar.TonyAR.Vector3
import com.google.android.filament.Filament
import com.google.ar.core.Plane

class MainActivity : AppCompatActivity() {
    private lateinit var sv : SurfaceView
    private lateinit var arContext: ArContext
    private lateinit var scene : Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        check()

        sv = findViewById(R.id.surface_view)

        Filament.init()
        System.loadLibrary("filament-utils-jni")

        val filament = TonyFilament(this, sv)
        val arCore = TonyArCore(this, filament, sv)
        scene = Scene(this, filament)


        val frameCallback = TonyFrameCallback(
            arCore
        ) { frame ->
            scene.doFrame(frame)
           // Log.e("Wonsik", "${frame.timestamp}")
        }

        arContext = ArContext(arCore, frameCallback)
    }

    private fun check() : Boolean{
        val result = checkSelfPermission(Manifest.permission.CAMERA)
        if(result != PackageManager.PERMISSION_GRANTED){
            Log.e("WONSIK","?")
            val ALL_PERMISSIONS = 101
            val permissions = arrayOf<String>(Manifest.permission.CAMERA)
            ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS)
            return false
        } else{
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == 101){
        }
    }

    override fun onPause() {
        super.onPause()
        arContext.frameCallback.stop()
        arContext.arCore.session.pause()
    }

    override fun onResume() {
        super.onResume()
        arContext.arCore.session.resume()
        arContext.frameCallback.start()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?:return false

        if(event.actionMasked == MotionEvent.ACTION_DOWN){
            placeObject(event)
        }

        return true
    }

    private fun placeObject(event : MotionEvent){
        val list = arContext.arCore.frame.hitTest(event)?:return
        if(list.isNotEmpty()){
            list.firstOrNull()?.let {
                if(it.trackable is Plane){
                    Log.e("WONSIK", "is Plane")
                    V3(it.hitPose.translation).apply {
                        val node = Node(arContext).apply {
                            setRenderable(this@MainActivity, "arachi_walk.glb")
                            setWorldPosition(Vector3(x,y,z))
                            setParent(scene)
                        }
                    }
                }
            }
        }
    }
}