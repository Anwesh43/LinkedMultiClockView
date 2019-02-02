package com.anwesh.uiprojects.linkedmulticlockview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.multiclockview.MultiClockView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiClockView.create(this)
    }
}
