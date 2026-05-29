package com.example.musicplayer

import android.app.Application
import com.google.android.gms.ads.MobileAds

class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}
