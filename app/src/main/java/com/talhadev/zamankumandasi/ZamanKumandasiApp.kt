package com.talhadev.zamankumandasi

import android.app.Application
import com.talhadev.zamankumandasi.ads.AdManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZamanKumandasiApp : Application() {
	override fun onCreate() {
		super.onCreate()
		AdManager.init(this)
	}
}
