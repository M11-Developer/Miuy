package com.miyu.companion.service

import android.content.Intent
import android.service.quicksettings.TileService
import com.miyu.companion.util.OverlayPermission

class MiyuTileService : TileService() {
    override fun onClick() {
        super.onClick()
        if (!OverlayPermission.hasPermission(this)) {
            val intent = OverlayPermission.requestPermissionIntent(this).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
        } else {
            val serviceIntent = Intent(this, FloatingCompanionService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }
}
