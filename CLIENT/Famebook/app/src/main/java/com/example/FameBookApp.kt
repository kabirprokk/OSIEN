package com.example

import android.app.Application
import com.example.di.ServiceLocator

class FameBookApp : Application() {
  override fun onCreate() {
    super.onCreate()
    ServiceLocator.init(this)
  }
}
