package com.example.tonyar.ARCore

import android.view.Choreographer
import com.example.tonyar.TonyAR.ModelLoader

data class ArContext(
    val arCore: TonyArCore,
    val frameCallback : TonyFrameCallback
){

}