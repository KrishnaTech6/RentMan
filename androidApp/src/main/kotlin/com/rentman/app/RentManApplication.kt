package com.rentman.app

import android.app.Application
import com.rentman.sharedui.di.startRentManKoin

class RentManApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startRentManKoin(this)
    }
}
