package com.example.tonyar.Filament

import android.content.Context
import android.opengl.*
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader

class TonyFilament(context : Context, val surfaceView: SurfaceView) {
    var timeStamp : Long = 0L
    private val eglContext : EGLContext = createEglContext()
    val engine : Engine = Engine.create(eglContext)
    val renderer : Renderer = engine.createRenderer()
    val scene : Scene = engine.createScene()

    val camera : Camera = engine
        .createCamera()
        .also { camera ->
            camera.setExposure(16f, 1f / 125f, 100f)
        }

    val view : View = engine
        .createView()
        .also { view ->
            view.camera = camera
            view.scene = scene
        }

    val assetLoader =
        AssetLoader(engine, MaterialProvider(engine), EntityManager.get())

    val resourceLoader =
        ResourceLoader(engine)

    var swapChain: SwapChain? = null
    val displayHelper = DisplayHelper(context)

    val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        renderCallback = object : UiHelper.RendererCallback{
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let{ engine.destroySwapChain(it)}
                swapChain = engine.createSwapChain(surface)
                displayHelper.attach(renderer, surfaceView.display)
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0,0,width, height)
            }
        }

        attachTo(surfaceView)
    }

    fun destroy(){
        uiHelper.detach()
        engine.destroy()
        destroyEglContext(eglContext)
    }

    private fun createEglContext() : EGLContext{
        val eglOpenGlEs3bit = 0x40
        val display : EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(display, null, 0, null, 0)
        val configs : Array<EGLConfig?> = arrayOfNulls(1)

        EGL14.eglChooseConfig(
            display,
            intArrayOf(EGL14.EGL_RENDERABLE_TYPE, eglOpenGlEs3bit, EGL14.EGL_NONE),
            0,
            configs,
            0,
            1,
            intArrayOf(0),
            0
        )

        val context : EGLContext =
            EGL14.eglCreateContext(
                display,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
                0
            )

        val surface : EGLSurface =
            EGL14.eglCreatePbufferSurface(
                display,
                configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
                0
            )

        return context
    }

    private fun destroyEglContext(context: EGLContext){
        EGL14.eglDestroyContext(EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY), context)
    }
}